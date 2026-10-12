package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/** Animated, narrow-screen Combat Coach dashboard. No gameplay automation. */
final class CombatCoachPanel extends PluginPanel {
    private static final Color BG=new Color(16,21,29);
    private static final Color PANEL=new Color(27,35,46);
    private static final Color LIGHT=new Color(238,247,250);
    private static final Color MUTED=new Color(160,178,190);
    private static final Color CYAN=new Color(79,210,239);
    private static final Color GREEN=new Color(101,235,171);
    private static final Color GOLD=new Color(255,207,115);

    private final Label headline=new Label("STRENGTH",14,LIGHT,true);
    private final Label level=new Label("LEVEL --",12,MUTED,true);
    private final Label recommended=new Label("Waiting for recommendation",14,GREEN,true);
    private final Label forecast=new Label("Start fighting to estimate",14,LIGHT,true);
    private final Label duration=new Label("",12,MUTED,false);
    private final Label remaining=new Label("Waiting for XP",12,MUTED,false);
    private final Label status=new Label("AWAITING DATA",10,GOLD,true);
    private final Label why=new Label("",12,LIGHT,false);
    private final Label access=new Label("",12,MUTED,false);
    private final Label best=new Label("",12,GREEN,true);
    private final Label comparison=new Label("",12,MUTED,false);
    private final Label predicted=new Label("",12,MUTED,false);
    private final JSpinner goal=new JSpinner(new SpinnerNumberModel(20,2,99,1));
    private final Ring ring=new Ring();
    private final JPanel skills=new JPanel(new GridLayout(0,3,5,5));
    private final JPanel drawer=new JPanel();
    private final JPanel trainingPage=new JPanel();
    private final JPanel analysisPage=new JPanel();
    private final JPanel settingsPage=new JPanel();
    private final JLabel currentXpText=new Label("0",12,LIGHT,true);
    private final JLabel targetXpText=new Label("0",12,LIGHT,true);
    private final JLabel remainingXpText=new Label("0",12,LIGHT,true);
    private final JPanel forecastCard=new JPanel();
    private final JButton drawerButton=new JButton("TRAINING ANALYSIS  +");
    private final javax.swing.Timer animations;
    private int currentLevel=1,currentXp=0,previousLevel=-1;
    private long currentRate=-1;
    private boolean updatingGoal=false;
    private long celebrateUntil=0;

    CombatCoachPanel(ActionListener refresh,ActionListener navigate,ActionListener goalChanged) {
        setLayout(new BorderLayout());
        setBackground(BG);
        ScrollBody content=new ScrollBody();
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(10,8,15,8));

        JPanel banner=panel();
        banner.setBackground(new Color(18,51,69));
        banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(47,112,140),1,true),
            new EmptyBorder(12,11,12,11)));
        banner.add(small("COMBAT COACH  /  LIVE",CYAN));
        banner.add(gap(7));
        banner.add(headline);
        banner.add(gap(3));
        banner.add(level);
        banner.add(gap(8));
        banner.add(small("RECOMMENDED TARGET",MUTED));
        banner.add(gap(3));
        banner.add(recommended);
        addSection(content,banner);

        JPanel goalPanel=panel();
        goalPanel.add(small("LEVEL PROGRESSION",GOLD));
        goalPanel.add(gap(7));
        JPanel selector=new JPanel(new BorderLayout(4,0));
        selector.setOpaque(false);
        selector.setAlignmentX(Component.LEFT_ALIGNMENT);
        selector.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        selector.add(new Label("TARGET LEVEL",11,MUTED,true),BorderLayout.WEST);
        goal.setFont(new Font(Font.DIALOG,Font.BOLD,13));
        goal.setPreferredSize(new Dimension(65,28));
        selector.add(goal,BorderLayout.EAST);
        goalPanel.add(selector);
        goalPanel.add(gap(7));
        ring.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalPanel.add(ring);
        goalPanel.add(gap(6));
        goalPanel.add(remaining);
        goalPanel.add(gap(12));
        goalPanel.add(small("EXPECTED COMPLETION",CYAN));
        goalPanel.add(gap(4));
        goalPanel.add(forecast);
        goalPanel.add(gap(4));
        goalPanel.add(duration);
        goalPanel.add(gap(7));
        goalPanel.add(status);
        goalPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56,109,123),1,true),
            new EmptyBorder(12,11,12,11)));
        addSection(content,goalPanel);

        JPanel skillPanel=panel();
        skillPanel.add(small("COMBAT PROFILE",CYAN));
        skillPanel.add(gap(9));
        skills.setOpaque(false);
        skills.setAlignmentX(Component.LEFT_ALIGNMENT);
        skills.setMaximumSize(new Dimension(Integer.MAX_VALUE,148));
        skillPanel.add(skills);
        addSection(content,skillPanel);

        JButton route=button("GUIDE ME TO TARGET  > ",navigate,true);
        JButton reroll=button("Refresh recommendation",refresh,false);
        content.add(route);content.add(gap(6));content.add(reroll);content.add(gap(10));

        drawer.setLayout(new BoxLayout(drawer,BoxLayout.Y_AXIS));
        drawer.setOpaque(false);
        JPanel detailsPanel=panel();
        detailsPanel.add(small("MEASURED TRAINING",GOLD));
        detailsPanel.add(gap(6));detailsPanel.add(best);
        detailsPanel.add(gap(5));detailsPanel.add(comparison);
        detailsPanel.add(gap(12));detailsPanel.add(small("THEORETICAL COMPARISON",CYAN));
        detailsPanel.add(gap(5));detailsPanel.add(predicted);
        detailsPanel.add(gap(12));detailsPanel.add(small("RECOMMENDATION NOTES",CYAN));
        detailsPanel.add(gap(5));detailsPanel.add(why);
        detailsPanel.add(gap(4));detailsPanel.add(access);
        drawer.add(detailsPanel);
        drawer.setVisible(false);
        drawerButton.setFont(new Font(Font.DIALOG,Font.BOLD,11));
        drawerButton.setForeground(CYAN);
        drawerButton.setBackground(PANEL);
        drawerButton.setFocusPainted(false);
        drawerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        drawerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        drawerButton.addActionListener(e-> {
            drawer.setVisible(!drawer.isVisible());
            drawerButton.setText(drawer.isVisible()?"TRAINING ANALYSIS  -":"TRAINING ANALYSIS  +");
            content.revalidate();
        });
        content.add(drawerButton);content.add(gap(7));content.add(drawer);
        JPanel xpRows=panel();
        xpRows.add(small("GOAL BREAKDOWN",CYAN));xpRows.add(gap(7));
        xpRows.add(small("CURRENT XP",MUTED));xpRows.add(currentXpText);xpRows.add(gap(5));
        xpRows.add(small("TARGET XP",MUTED));xpRows.add(targetXpText);xpRows.add(gap(5));
        xpRows.add(small("REMAINING",MUTED));xpRows.add(remainingXpText);
        content.add(gap(8));content.add(xpRows);

        JScrollPane scroll=new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll,BorderLayout.CENTER);

        goal.addChangeListener(e -> {
            if (updatingGoal) return;
            updateGoal();
            if(goalChanged!=null)goalChanged.actionPerformed(null);
        });

        animations=new javax.swing.Timer(90,e -> {
            boolean visible=isShowing();
            if (!visible) return;
            ring.animate(System.currentTimeMillis()<celebrateUntil);
        });
        clear();
    }

    @Override public void addNotify() {
        super.addNotify();
        animations.start();
    }
    @Override public void removeNotify() {
        animations.stop();
        super.removeNotify();
    }

    private static final class ScrollBody extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 18;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(80,r.height-40);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }

    private static final class Label extends JTextArea {
        Label(String s,int size,Color color,boolean bold) {
            super(s);
            setEditable(false);setFocusable(false);setOpaque(false);
            setLineWrap(true);setWrapStyleWord(true);setBorder(null);
            setColumns(1);setAlignmentX(Component.LEFT_ALIGNMENT);
            setForeground(color);
            setFont(new Font(Font.DIALOG,bold?Font.BOLD:Font.PLAIN,size));
        }
        void value(String s) {setText(s==null?"":s);revalidate();}
    }

    private static final class Ring extends JComponent {
        private double shown=0,target=0;
        private float shimmer=0;
        private boolean reducedMotion=false;
        Ring(){setPreferredSize(new Dimension(170,126));setMaximumSize(new Dimension(Integer.MAX_VALUE,126));}
        void setTarget(double value) { target=Math.max(0,Math.min(1,value));repaint(); }
        void animate(boolean celebration) {
            if (reducedMotion)shown=target;
            else shown+=(target-shown)*0.15;
            shimmer+=celebration?0.2f:0.028f;
            repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int cx=getWidth()/2,cy=60,r=43;
            g.setStroke(new BasicStroke(10,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(new Color(54,69,79));
            g.draw(new Arc2D.Double(cx-r,cy-r,r*2,r*2,0,360,Arc2D.OPEN));
            float alpha=(float)(0.75+0.25*Math.sin(shimmer));
            g.setColor(new Color(101,235,171,Math.max(0,Math.min(255,(int)(alpha*255)))));
            g.draw(new Arc2D.Double(cx-r,cy-r,r*2,r*2,90,-360*shown,Arc2D.OPEN));
            g.setFont(new Font(Font.DIALOG,Font.BOLD,21));
            String pct=String.format(Locale.US,"%.1f%%",shown*100);
            FontMetrics fm=g.getFontMetrics();g.setColor(LIGHT);
            g.drawString(pct,cx-fm.stringWidth(pct)/2,cy+7);
            g.setFont(new Font(Font.DIALOG,Font.PLAIN,10));
            g.setColor(MUTED);
            String msg="GOAL PROGRESS";
            FontMetrics sfm=g.getFontMetrics();
            g.drawString(msg,cx-sfm.stringWidth(msg)/2,cy+63);
            g.dispose();
        }
    }

    private static JPanel panel() {
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(PANEL);p.setBorder(new EmptyBorder(12,11,12,11));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        return p;
    }
    private static Label small(String s,Color color){return new Label(s,10,color,true);}
    private static Component gap(int px){return Box.createVerticalStrut(px);}
    private static void addSection(JPanel parent,JPanel section){parent.add(section);parent.add(gap(9));}
    private static JButton button(String text,ActionListener fn,boolean primary) {
        JButton b=new JButton(text);b.addActionListener(fn);
        b.setForeground(primary?new Color(16,24,27):LIGHT);
        b.setBackground(primary?new Color(105,221,168):PANEL);
        if (primary) b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(170,247,204),1,true),
            new EmptyBorder(5,6,5,6)));
        b.setFocusPainted(false);b.setFont(new Font(Font.DIALOG,Font.BOLD,12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,33));
        return b;
    }
    private void tile(String name,int number,boolean active) {
        JPanel cell=new JPanel(new BorderLayout());
        cell.setBackground(active?new Color(27,99,77):new Color(49,61,73));
        cell.setBorder(new EmptyBorder(5,5,5,5));
        JLabel title=new JLabel(name),value=new JLabel(Integer.toString(number));
        title.setForeground(active?GREEN:MUTED);
        title.setFont(new Font(Font.DIALOG,Font.BOLD,10));
        value.setForeground(LIGHT);value.setFont(new Font(Font.DIALOG,Font.BOLD,17));
        cell.add(title,BorderLayout.NORTH);cell.add(value,BorderLayout.CENTER);
        skills.add(cell);
    }
    void showRecommendation(String trainingSkill,int levelNow,TrainingSpot spot,
                            CombatLoadout gear,String ignoredRateText,String modelText) {
        headline.value(trainingSkill.toUpperCase(Locale.US));
        level.value("LEVEL "+levelNow);
        recommended.value(spot.name);
        if(previousLevel>=0 && levelNow>previousLevel)celebrateUntil=System.currentTimeMillis()+2000;
        previousLevel=levelNow;
        currentLevel=levelNow;
        skills.removeAll();
        tile("ATK",gear.attack,trainingSkill.equalsIgnoreCase("Attack"));
        tile("STR",gear.strength,trainingSkill.equalsIgnoreCase("Strength"));
        tile("DEF",gear.defence,trainingSkill.equalsIgnoreCase("Defence"));
        tile("HP",gear.hitpoints,false);
        tile("RNG",gear.ranged,trainingSkill.equalsIgnoreCase("Ranged"));
        tile("MAG",gear.magic,trainingSkill.equalsIgnoreCase("Magic"));
        tile("PRAY",gear.prayer,false);
        int g=((Number)goal.getValue()).intValue();
        if(g<=levelNow && levelNow<99){
            updatingGoal=true;goal.setValue(levelNow+1);updatingGoal=false;
        }
        String note=spot.reason;
        if(note.startsWith("Gear: ")){
            int cut=note.indexOf(". ");
            if(cut>=0)note=note.substring(cut+2);
        }
        why.value(note);access.value(spot.requirements);
        String modelSummary=modelText==null?"":modelText;
        int end=modelSummary.indexOf("Estimates assume");
        if(end>=0)modelSummary=modelSummary.substring(0,end).trim();
        predicted.value(modelSummary.replace("Highest modelled: ","Model best: "));
        skills.revalidate();updateGoal();revalidate();
    }
    void updateProgress(int xp,long rate,String bestMonster,String comparisonText) {
        currentXp=xp;currentRate=rate;
        best.value(bestMonster);
        comparison.value(comparisonText==null?"":comparisonText.replace("; ","\n"));
        updateGoal();
    }
    private void updateGoal() {
        int targetLevel=((Number)goal.getValue()).intValue();
        if(targetLevel<=currentLevel){
            remaining.value("Goal reached");forecast.value("Choose a higher goal");
            duration.value("");status.value("COMPLETE");ring.setTarget(1);
            return;
        }
        int begin=xpForLevel(currentLevel),end=xpForLevel(targetLevel);
        long left=Math.max(0L,(long)end-currentXp);
        currentXpText.setText(String.format(Locale.US,"%,d",currentXp));
        targetXpText.setText(String.format(Locale.US,"%,d",end));
        remainingXpText.setText(String.format(Locale.US,"%,d",left));
        remaining.value(String.format(Locale.US,"%,d XP remaining",left));
        ring.setTarget(Math.max(0,Math.min(1,(currentXp-begin)/(double)Math.max(1,end-begin))));
        if(currentXp<=0 || currentRate<=0){
            forecast.value("Waiting for training data");
            duration.value("");status.value("CALIBRATING");return;
        }
        long seconds=(long)Math.ceil(left*3600.0/currentRate);
        ZonedDateTime finish=ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(seconds);
        forecast.value(finish.format(DateTimeFormatter.ofPattern("MMM d, uuuu  h:mm a",Locale.US)));
        long days=seconds/86400,hours=(seconds%86400)/3600,minutes=(seconds%3600)/60;
        duration.value("Approx. "+(days>0?days+" days ":"")+(hours>0?hours+" hr ":"")+minutes+" min left");
        status.value("LIVE FORECAST");
    }
    private static int xpForLevel(int level) {
        int sum=0;
        for(int i=1;i<level;i++)sum+=(int)Math.floor(i+300*Math.pow(2,i/7.0));
        return sum/4;
    }
    void clear() {
        headline.value("WAITING");level.value("LEVEL --");
        recommended.value("Log in to begin");forecast.value("Waiting for game");
        duration.value("");remaining.value("Waiting for XP");
        status.value("IDLE");skills.removeAll();ring.setTarget(0);
        best.value("");comparison.value("");predicted.value("");why.value("");access.value("");
    }
}
