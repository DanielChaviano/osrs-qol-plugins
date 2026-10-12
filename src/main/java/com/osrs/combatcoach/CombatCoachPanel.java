package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/** Compact dashboard: use the sidebar's own vertical scrolling, never horizontal overflow. */
final class CombatCoachPanel extends PluginPanel {
    private static final Color BG=new Color(22,27,34), CARD=new Color(36,43,53);
    private static final Color FG=new Color(240,245,250), MUTED=new Color(169,182,196);
    private static final Color ACCENT=new Color(82,202,229), GOLD=new Color(245,192,89);
    private static final Color GREEN=new Color(102,224,166);

    private final JLabel training=text("Waiting for game",15,FG,true);
    private final JLabel monster=text("-",16,GREEN,true);
    private final JLabel remaining=text("Waiting for XP",12,MUTED,false);
    private final JLabel eta=text("Start training for an ETA",14,FG,true);
    private final JLabel duration=text("",12,MUTED,false);
    private final JProgressBar progress=new JProgressBar(0,1000);
    private final JSpinner goal=new JSpinner(new SpinnerNumberModel(20,2,99,1));
    private final JLabel status=text("MEASURING",10,GOLD,true);
    private final JPanel skillGrid=new JPanel(new GridLayout(0,3,5,5));
    private final JLabel best=text("No measured results yet",12,GREEN,true);
    private final JLabel summary=text("",11,MUTED,false);
    private final JLabel model=text("",11,MUTED,false);
    private final JLabel why=text("",12,FG,false);
    private final JLabel requires=text("",11,MUTED,false);
    private final JButton more=new JButton("Show training details  v");
    private final JPanel details=new JPanel();
    private final Timer animate;
    private int currentLevel=1, currentXp=0;
    private long currentRate=-1;
    private double visualProgress=0;
    private double wantedProgress=0;

    CombatCoachPanel(ActionListener refresh, ActionListener navigate, ActionListener goalChanged) {
        setLayout(new BorderLayout());
        setBackground(BG);
        // Deliberately one vertical scroll container; its content cannot grow horizontally.
        JPanel body=new ScrollBody();
        body.setBackground(BG);
        body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10,8,14,8));
        body.add(text("COMBAT COACH",18,FG,true));
        body.add(space(10));

        JPanel hero=card();
        hero.add(header("CURRENT TRAINING",ACCENT));
        hero.add(space(4)); hero.add(training);
        hero.add(space(9)); hero.add(header("BEST SUGGESTED SPOT",GREEN));
        hero.add(space(4)); hero.add(monster);
        body.add(hero);body.add(space(8));

        JPanel goalCard=card();
        goalCard.add(header("LEVEL TARGET",GOLD));
        goalCard.add(space(6));
        JPanel selector=new JPanel(new BorderLayout(5,0));
        selector.setOpaque(false);
        selector.add(text("Goal",12,FG,false),BorderLayout.WEST);
        goal.setPreferredSize(new Dimension(66,27));
        goal.setFont(new Font(Font.DIALOG,Font.BOLD,13));
        selector.add(goal,BorderLayout.EAST);
        selector.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        selector.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalCard.add(selector);goalCard.add(space(8));
        progress.setForeground(GREEN);progress.setBackground(new Color(55,65,78));
        progress.setBorderPainted(false);progress.setStringPainted(false);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,9));
        progress.setPreferredSize(new Dimension(160,9));
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalCard.add(progress);goalCard.add(space(6));
        goalCard.add(remaining);goalCard.add(space(8));
        goalCard.add(header("ESTIMATED COMPLETION",ACCENT));goalCard.add(space(5));
        goalCard.add(eta);goalCard.add(space(4));goalCard.add(duration);
        goalCard.add(space(5));goalCard.add(status);
        body.add(goalCard);body.add(space(8));

        JPanel stats=card();
        stats.add(header("COMBAT SKILLS",ACCENT));stats.add(space(8));
        skillGrid.setOpaque(false);
        skillGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        skillGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE,165));
        stats.add(skillGrid);
        body.add(stats);body.add(space(8));

        JButton nav=button("NAVIGATE TO MONSTER",navigate,true);
        JButton reload=button("Refresh recommendation",refresh,false);
        body.add(nav);body.add(space(6));body.add(reload);body.add(space(9));

        details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS));
        details.setOpaque(false);
        JPanel comparison=card();
        comparison.add(header("YOUR TESTED MONSTERS",GOLD));comparison.add(space(5));
        comparison.add(best);comparison.add(space(4));comparison.add(summary);
        details.add(comparison);details.add(space(8));
        JPanel estimate=card();
        estimate.add(header("MODELLED TRAINING",ACCENT));estimate.add(space(5));
        estimate.add(model);estimate.add(space(8));estimate.add(header("WHY THIS SPOT",GOLD));
        estimate.add(space(5));estimate.add(why);estimate.add(space(6));estimate.add(requires);
        details.add(estimate);
        details.setVisible(false);
        more.setAlignmentX(Component.LEFT_ALIGNMENT);
        more.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        more.addActionListener(e -> {
            details.setVisible(!details.isVisible());
            more.setText(details.isVisible()?"Hide training details  ^":"Show training details  v");
            body.revalidate();
        });
        body.add(more);body.add(space(7));body.add(details);

        JScrollPane scroller=new JScrollPane(body);
        scroller.setBorder(null);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.setViewportView(body);
        // Keep content bounded; do not rely on fragile per-label width hacks.
        add(scroller,BorderLayout.CENTER);

        goal.addChangeListener(e -> {updateGoal();if(goalChanged!=null)goalChanged.actionPerformed(null);});
        animate=new Timer(80,e -> {
            if(Math.abs(visualProgress-wantedProgress)<.001) return;
            visualProgress+=(wantedProgress-visualProgress)*.22;
            progress.setValue((int)Math.round(visualProgress*1000));
        });
        animate.start();
    }

    private static final class ScrollBody extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 16;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(32,r.height-32);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private static JPanel card() {
        JPanel x=new JPanel();x.setLayout(new BoxLayout(x,BoxLayout.Y_AXIS));
        x.setBackground(CARD);x.setBorder(new EmptyBorder(11,10,11,10));
        x.setAlignmentX(Component.LEFT_ALIGNMENT);
        x.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        return x;
    }
    private static JLabel text(String s,int sz,Color color,boolean bold) {
        JLabel l=new JLabel(s);l.setForeground(color);
        l.setFont(new Font(Font.DIALOG,bold?Font.BOLD:Font.PLAIN,sz));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private static JLabel header(String s,Color c){return text(s,10,c,true);}
    private static Component space(int px){return Box.createVerticalStrut(px);}
    private static JButton button(String s,ActionListener a,boolean primary) {
        JButton b=new JButton(s);b.addActionListener(a);
        b.setFocusPainted(false);b.setForeground(FG);
        b.setFont(new Font(Font.DIALOG,Font.BOLD,12));
        b.setBackground(primary?new Color(29,110,129):CARD);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,33));
        return b;
    }
    private static String html(String value,int width) {
        String s=value==null?"":value;
        s=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        return "<html><div style='width:"+width+"px;'>"+
            s.replace("\n","<br>")+"</div></html>";
    }
    private static void wrap(JLabel label,String value){label.setText(html(value,154));}
    private void tile(String key,int val,boolean active) {
        JPanel t=new JPanel(new BorderLayout());
        t.setBackground(active?new Color(29,107,86):new Color(51,60,73));
        t.setBorder(new EmptyBorder(6,5,6,5));
        t.add(text(key,10,active?GREEN:MUTED,true),BorderLayout.NORTH);
        t.add(text(Integer.toString(val),17,FG,true),BorderLayout.CENTER);
        skillGrid.add(t);
    }

    void showRecommendation(String trainingSkill,int level,TrainingSpot spot,
                            CombatLoadout gear,String rateText,String predicted) {
        training.setText(trainingSkill+"  |  Level "+level);
        wrap(monster,spot.name);
        currentLevel=level;
        skillGrid.removeAll();
        tile("ATK",gear.attack,trainingSkill.equalsIgnoreCase("Attack"));
        tile("STR",gear.strength,trainingSkill.equalsIgnoreCase("Strength"));
        tile("DEF",gear.defence,trainingSkill.equalsIgnoreCase("Defence"));
        tile("HP",gear.hitpoints,false);
        tile("RNG",gear.ranged,trainingSkill.equalsIgnoreCase("Ranged"));
        tile("MAG",gear.magic,trainingSkill.equalsIgnoreCase("Magic"));
        tile("PRAY",gear.prayer,false);
        int g=((Number)goal.getValue()).intValue();
        if(g<=level && level<99)goal.setValue(level+1);
        String desc=spot.reason;
        if(desc.startsWith("Gear: ")){
            int cut=desc.indexOf(". ");
            if(cut>=0)desc=desc.substring(cut+2);
        }
        wrap(why,desc);
        wrap(requires,spot.requirements);
        String pred=predicted==null?"":predicted;
        int cut=pred.indexOf("Estimates assume");
        if(cut>=0)pred=pred.substring(0,cut).trim();
        wrap(model,pred.replace("Highest modelled: ","Best modelled: "));
        updateGoal();
        skillGrid.revalidate();
        revalidate();
    }
    void updateProgress(int xp,long rate,String bestMonster,String comparisons) {
        currentXp=xp;currentRate=rate;
        wrap(best,bestMonster);
        wrap(summary,comparisons==null?"":comparisons.replace("; ","\n"));
        updateGoal();
    }
    private void updateGoal() {
        int g=((Number)goal.getValue()).intValue();
        if(g<=currentLevel){remaining.setText("Goal reached");wrap(eta,"Choose a higher level");wrap(duration,"");wantedProgress=1;return;}
        int start=xpForLevel(currentLevel),end=xpForLevel(g);
        long needed=Math.max(0L,(long)end-currentXp);
        remaining.setText(String.format(Locale.US,"%,d XP left",needed));
        wantedProgress=Math.max(0,Math.min(1,(currentXp-start)/(double)Math.max(1,end-start)));
        if(currentRate<=0 || currentXp<=0){
            wrap(eta,"Waiting for combat data");wrap(duration,"");
            status.setText("CALIBRATING");return;
        }
        // Preserve the working observed-XP time calculation.
        long seconds=(long)Math.ceil(needed*3600.0/currentRate);
        ZonedDateTime finish=ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(seconds);
        wrap(eta,finish.format(DateTimeFormatter.ofPattern("MMM d, uuuu  h:mm a",Locale.US)));
        long days=seconds/86400,hours=(seconds%86400)/3600,mins=(seconds%3600)/60;
        wrap(duration,"In "+(days>0?days+"d ":"")+(hours>0?hours+"h ":"")+mins+"m");
        status.setText("LIVE ETA");
    }
    private static int xpForLevel(int l) {
        int sum=0;for(int i=1;i<l;i++)sum+=(int)Math.floor(i+300*Math.pow(2,i/7.0));
        return sum/4;
    }
    void clear(){
        training.setText("Waiting for game");
        monster.setText("-");eta.setText("Waiting for combat");
        duration.setText("");remaining.setText("Waiting for XP");
        best.setText("No measured results yet");summary.setText("");
        skillGrid.removeAll();wantedProgress=0;
    }
}
