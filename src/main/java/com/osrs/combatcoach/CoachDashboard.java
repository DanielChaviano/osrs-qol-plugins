package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javax.swing.*;
import static com.osrs.combatcoach.CoachArt.*;

/** One responsive, custom-painted surface. No BoxLayout, nested scroll panes or forced text widths. */
public final class CoachDashboard extends JComponent implements Scrollable {
    private CoachUiState state = CoachUiState.waiting();
    private final BiConsumer<String,String> action;
    private final Timer timer;
    private final List<Block> blocks = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private int tab, layoutWidth=-1, physicalWidth=-1, layoutHeight=600;
    private double uiScale=1;
    private double displayProgress, phase, tabPosition;
    private boolean dirty=true;
    private long lastFrame=System.nanoTime(), lastGainNanos;
    private String hovered="";
    private final java.util.Map<String,Double> hoverAmounts=new java.util.HashMap<>();
    private final java.util.Map<String,BufferedImage> thumbnails=new java.util.HashMap<>();
    private long lastGainAmount, pageChanged=System.nanoTime(), rippleStarted;
    private Point ripplePoint=new Point();
    private boolean goalJustReached;
    private Instant testNow;
    private Instant now(){return testNow==null?Instant.now():testNow;}
    private int keyboardHit=-1;
    private final Font body=font(13,false), caption=font(11,true);
    private static final String[] SKILLS={"Attack","Strength","Defence","Hitpoints","Ranged","Magic","Prayer"};
    private static final String[] SHORT={"ATK","STR","DEF","HP","RNG","MAG","PRAY"};
    private static final class Block {
        final String kind;final Rectangle r;final String text;
        Block(String kind,Rectangle r,String text){this.kind=kind;this.r=r;this.text=text;}
    }
    private static final class Hit {
        final String id,label;final Rectangle r;
        Hit(String id,String label,Rectangle r){this.id=id;this.label=label;this.r=new Rectangle(r);}
    }
    public CoachDashboard(BiConsumer<String,String> action) {
        this.action=action;
        setOpaque(true);setBackground(BG);setFocusable(true);
        getAccessibleContext().setAccessibleName("Combat Coach dashboard");
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mouseMoved(MouseEvent e){String id=hitAt(e.getPoint());if(!id.equals(hovered)){hovered=id;setCursor(Cursor.getPredefinedCursor(id.isEmpty()?Cursor.DEFAULT_CURSOR:Cursor.HAND_CURSOR));repaint();}}
            @Override public void mouseExited(MouseEvent e){hovered="";repaint();}
            @Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)){String id=hitAt(e.getPoint());if(!id.isEmpty()){requestFocusInWindow();ripplePoint=new Point((int)(e.getX()/uiScale),(int)(e.getY()/uiScale));rippleStarted=System.nanoTime();activate(id);}}}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);
        addComponentListener(new ComponentAdapter(){@Override public void componentResized(ComponentEvent e){dirty=true;revalidate();repaint();}});
        timer=new Timer(33,e->{
            if(!isShowing()||!state.preferences.animate()){timerStopIfHidden();return;}
            long now=System.nanoTime();double dt=Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;
            phase+=dt;displayProgress+=(state.progress()-displayProgress)*(1-Math.exp(-dt*7));
            tabPosition+=(tab-tabPosition)*(1-Math.exp(-dt*15));
            for(Hit h:hits){double old=hoverAmounts.getOrDefault(h.id,0.0);double wanted=h.id.equals(hovered)?1:0;hoverAmounts.put(h.id,old+(wanted-old)*(1-Math.exp(-dt*12)));}
            repaint();
        });
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0)updateTimer();});
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0),"previousTab");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0),"nextTab");
        getActionMap().put("previousTab",new AbstractAction(){public void actionPerformed(ActionEvent e){setTab(Math.max(0,tab-1));}});
        getActionMap().put("nextTab",new AbstractAction(){public void actionPerformed(ActionEvent e){setTab(Math.min(2,tab+1));}});
        setFocusTraversalKeysEnabled(false);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),"nextControl");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"activateControl");
        getActionMap().put("nextControl",new AbstractAction(){public void actionPerformed(ActionEvent e){ensureLayout();if(!hits.isEmpty()){keyboardHit=(keyboardHit+1)%hits.size();hovered=hits.get(keyboardHit).id;Rectangle r=hits.get(keyboardHit).r;scrollRectToVisible(new Rectangle((int)(r.x*uiScale),(int)(r.y*uiScale),(int)(r.width*uiScale),(int)(r.height*uiScale)));repaint();}}});
        getActionMap().put("activateControl",new AbstractAction(){public void actionPerformed(ActionEvent e){if(keyboardHit>=0&&keyboardHit<hits.size())activate(hits.get(keyboardHit).id);}});
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext(){
        if(accessibleContext==null)accessibleContext=new AccessibleJComponent(){};
        return accessibleContext;
    }
    public void setState(CoachUiState next) {
        if(state.loggedIn&&next.xp>state.xp && next.skill.equals(state.skill)){
            lastGainNanos=System.nanoTime();lastGainAmount=next.xp-state.xp;
        }
        goalJustReached=state.loggedIn&&next.complete()&&!state.complete()&&state.skill.equals(next.skill);
        if(!next.skill.equals(state.skill))displayProgress=next.progress();
        state=next;
        if(!next.preferences.animate())displayProgress=next.progress();
        dirty=true;updateTimer();revalidate();repaint();
    }
    public void dispose(){timer.stop();ToolTipManager.sharedInstance().unregisterComponent(this);}
    private void timerStopIfHidden(){if(!isShowing()||!state.preferences.animate())timer.stop();}
    private void updateTimer(){if(isShowing()&&state.preferences.animate()){if(!timer.isRunning()){lastFrame=System.nanoTime();timer.start();}}else timer.stop();}
    @Override public void removeNotify(){timer.stop();super.removeNotify();}
    @Override public void addNotify(){super.addNotify();updateTimer();}
    public void setTab(int index){pageChanged=System.nanoTime();keyboardHit=-1;tab=Math.max(0,Math.min(2,index));if(!state.preferences.animate())tabPosition=tab;dirty=true;revalidate();repaint();SwingUtilities.invokeLater(()->scrollRectToVisible(new Rectangle(0,0,1,1)));}
    public int getTab(){return tab;}
    private String hitAt(Point p){ensureLayout();Point local=new Point((int)(p.x/uiScale),(int)(p.y/uiScale));for(Hit h:hits)if(h.r.contains(local))return h.id;return "";}
    @Override public String getToolTipText(MouseEvent e){ensureLayout();for(Hit h:hits)if(h.r.contains(new Point((int)(e.getX()/uiScale),(int)(e.getY()/uiScale))))return h.label;return null;}
    private void activate(String id) {
        if(id.startsWith("tab")){setTab(Integer.parseInt(id.substring(3)));return;}
        if(id.equals("analysis")){setTab(1);return;}
        if(id.equals("settings")){setTab(2);return;}
        if(id.equals("goal")){
            JSpinner spinner=new JSpinner(new SpinnerNumberModel(state.goal,2,99,1));
            spinner.setFont(font(16,true));
            if(JOptionPane.showConfirmDialog(this,spinner,"Target "+state.skill+" level",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
                try{spinner.commitEdit();action.accept("goal",spinner.getValue().toString());}catch(java.text.ParseException ignored){JOptionPane.showMessageDialog(this,"Enter a whole level from 2 to 99.");}
            }
            return;
        }
        if(id.equals("focus")){
            String[] options={"AUTO","ATTACK","STRENGTH","DEFENCE","RANGED","MAGIC"};
            Object selected=JOptionPane.showInputDialog(this,"Only changes the guide, not your combat style.","Training focus",JOptionPane.PLAIN_MESSAGE,null,options,"AUTO");
            if(selected!=null)action.accept("focus",selected.toString());return;
        }
        if(id.startsWith("skill:")){
            int i=Integer.parseInt(id.substring(6));if(i!=3&&i!=6)action.accept("focus",SKILLS[i].toUpperCase(Locale.ROOT));return;
        }
        if(id.equals("forecast"))return;
        if(id.equals("theme")){
            CoachPreferences.Theme[] options=CoachPreferences.Theme.values();
            String[] labels=java.util.Arrays.stream(options).map(t->t.label).toArray(String[]::new);
            Object choice=JOptionPane.showInputDialog(this,"Choose an appearance. Your training settings stay the same.","Theme",JOptionPane.PLAIN_MESSAGE,null,labels,state.preferences.theme.label);
            if(choice!=null)for(CoachPreferences.Theme t:options)if(t.label.equals(choice))action.accept("theme",t.name());
            return;
        }
        if(id.equals("effects")){
            String[] options={"Static","Subtle","Cinematic"};
            Object choice=JOptionPane.showInputDialog(this,"Decorative effects only. Reduced motion overrides animations.","Visual effects",JOptionPane.PLAIN_MESSAGE,null,options,options[state.preferences.effects]);
            if(choice!=null)for(int i=0;i<options.length;i++)if(options[i].equals(choice))action.accept("effects",Integer.toString(i));
            return;
        }
        if(id.equals("gallery")){setTab(2);SwingUtilities.invokeLater(()->{ensureLayout();for(Block b:blocks)if(b.kind.equals("memory")){scrollRectToVisible(new Rectangle(0,(int)(b.r.y*uiScale),1,(int)(b.r.height*uiScale)));break;}});return;}
        action.accept(id,"");
    }
    public void setThumbnail(String id,BufferedImage image){if(image!=null){thumbnails.put(id,image);if(thumbnails.size()>16){String first=thumbnails.keySet().iterator().next();thumbnails.remove(first);}repaint();}}
    private double hover(String id){return state.preferences.animate()?hoverAmounts.getOrDefault(id,0.0):(hovered.equals(id)?1:0);}
    private Rectangle rect(int x,int y,int width,int height){return new Rectangle(x,y,Math.max(1,width),height);}
    private int add(String kind,int x,int y,int w,int h,String text){blocks.add(new Block(kind,rect(x,y,w,h),text));return y+h+8;}
    private void hit(String id,String label,Rectangle r){hits.add(new Hit(id,label,r));}
    private int wrapped(Graphics2D g,String s,Font f,int w){return textHeight(g,s,f,Math.max(30,w));}
    private void ensureLayout() {
        int physical=Math.max(160,getWidth()>0?getWidth():242);
        double scale=physical>=320?Math.min(1.8,physical/260.0):1.0;
        int width=(int)Math.floor(physical/scale);
        if(!dirty&&physicalWidth==physical)return;
        physicalWidth=physical;uiScale=scale;
        blocks.clear();hits.clear();layoutWidth=width;
        BufferedImage scratch=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);Graphics2D g=scratch.createGraphics();quality(g);
        int x=8,w=width-16,y=8;
        y=add("brand",x,y,w,38,"");hit("expand","Open a larger live dashboard",rect(x+w-26,10,26,30));
        y=add("tabs",x,y,w,34,"");for(int i=0;i<3;i++)hit("tab"+i,new String[]{"Training","Analysis","Settings"}[i],rect(x+i*w/3,y-42,w/3,34));
        if(tab==0){
            int trainH=Math.max(74,36+wrapped(g,state.loggedIn?state.skill+"\nLevel "+state.level:"Log in to begin",font(15,true),w-76));
            y=add("training",x,y,w,trainH,"");hit("focus","Choose which skill Combat Coach follows",blocks.get(blocks.size()-1).r);
            int monsterH=Math.max(70,37+wrapped(g,state.spot,font(14,true),w-77))+(state.routeStatus.isEmpty()?0:32);
            y=add("monster",x,y,w,monsterH,"");
            y=add("progress",x,y,w,w<180?260:171,"");hit("goal","Set a target level; your chosen goal stays until you change it",rect(x+w-68,y-(w<180?268:179)+8,59,25));
            int forecastExtra=Math.max(0,wrapped(g,state.duration(),font(12,true),w-24)-(g.getFontMetrics(font(12,true)).getHeight()+2));
            y=add("forecast",x,y,w,(state.preferences.compactLayout?155:188)+forecastExtra,"");hit("forecast","Full local completion date at your current pace; assumes nonstop training.",blocks.get(blocks.size()-1).r);
            y=add("navigate",x,y,w,46,"");hit("navigate","Send the recommendation to Shortest Path. Move manually.",blocks.get(blocks.size()-1).r);
            y=add("skills",x,y,w,state.preferences.compactLayout?167:181,"");
            Rectangle grid=blocks.get(blocks.size()-1).r;int cellW=(w-26)/3;
            for(int i=0;i<7;i++)hit("skill:"+i,SKILLS[i]+" "+state.skillLevel(i),rect(x+9+(i%3)*(cellW+4),grid.y+36+(i/3)*44,cellW,39));
            y=add("link",x,y,w,42,"Explore training");hit("analysis","Read the evidence and limitations",blocks.get(blocks.size()-1).r);
            y=add("screenshots",x,y,w,50,"");hit("gallery","Open your captured memories and screenshot controls",blocks.get(blocks.size()-1).r);
        } else if(tab==1){
            String session="Selected skill: "+state.skill+"\n"+number(state.sessionXp)+" XP earned this session\n"+Math.max(0,state.sessionMillis/60000)+" minutes since the first gain\n\n"+state.paceLabel().toLowerCase(Locale.US);
            y=add("paragraph",x,y,w,42+wrapped(g,session,body,w-24),"YOUR SESSION\n"+session);
            String measured=measuredSummary();
            String predicted=predictedSummary();
            String why=state.reason.replaceFirst("^Gear: .*?\\. ","");
            y=add("paragraph",x,y,w,42+wrapped(g,measured,body,w-24),"YOUR TRAINING TESTS\n"+measured);
            y=add("paragraph",x,y,w,42+wrapped(g,predicted,body,w-24),"MODEL COMPARISON\n"+predicted);
            y=add("paragraph",x,y,w,42+wrapped(g,why.isEmpty()?"Log in to see the recommendation.":why,body,w-24),"WHY THIS TARGET\n"+(why.isEmpty()?"Log in to see the recommendation.":why));
            String access=state.requirements.isEmpty()?"No destination selected":state.requirements;
            y=add("paragraph",x,y,w,42+wrapped(g,access,body,w-24),"ACCESS\n"+access);
            String note="Measured results are samples, not proof of the best training method. The current melee model is approximate. Routing uses Shortest Path; movement stays manual.";
            y=add("paragraph",x,y,w,42+wrapped(g,note,body,w-24),"WHAT THIS MEANS\n"+note);
            y=add("refresh",x,y,w,43,"");hit("refresh","Re-read gear, levels and recommendation",blocks.get(blocks.size()-1).r);
        } else {
            y=add("choice",x,y,w,58,"theme\nTheme\n"+state.preferences.theme.label);hit("theme","Choose a presentation theme",blocks.get(blocks.size()-1).r);
            y=add("choice",x,y,w,58,"effects\nEffects\n"+new String[]{"Static","Subtle","Cinematic"}[state.preferences.effects]);hit("effects","Set visual effect intensity",blocks.get(blocks.size()-1).r);
            String[] ids={"hud","hudCompact","compactLayout","popups","motion","captures","includeCelebration","saveCard","includeFrame"};
            String[] names={"In-game HUD","Compact HUD","Compact layout","Saved-image popup","Reduced motion","Auto screenshots","Capture level-up card","Save card-only PNG","Include client frame"};
            for(int i=0;i<ids.length;i++){int h=Math.max(52,20+wrapped(g,names[i],font(12,true),w-73));y=add("toggle",x,y,w,h,ids[i]+"\n"+names[i]);hit(ids[i],names[i],blocks.get(blocks.size()-1).r);}
            String note="The level-up card appears first. A rendered-frame check then captures it with your game. A separate clean card PNG is saved when enabled. Saved confirmations use the actual resulting files.";
            y=add("paragraph",x,y,w,42+wrapped(g,note,body,w-24),"YOUR CAPTURE FLOW\n"+note);
            y=add("test",x,y,w,44,"");hit("testCapture","Show and capture the real animated card without changing your level",blocks.get(blocks.size()-1).r);
            y=add("openFolder",x,y,w,42,"");hit("openFolder","Copy the confirmed capture folder path",blocks.get(blocks.size()-1).r);
            if(!state.capturesList.isEmpty()){
                y=add("paragraph",x,y,w,70,"RECENT MEMORIES\nYour last 8 captures, saved locally.");
                for(CoachCaptureRecord record:state.capturesList){
                    int actionY=memoryActionY(g,record,w);int h=actionY+(record.cardFile.isEmpty()?44:77);
                    y=add("memory",x,y,w,h,record.id);Rectangle r=blocks.get(blocks.size()-1).r;
                    hit("view:"+record.id,"Open the full gameplay screenshot",rect(x+8,r.y+actionY,w-16,29));
                    if(!record.cardFile.isEmpty())hit("card:"+record.id,"Open the clean milestone-card PNG",rect(x+8,r.y+actionY+33,w-16,29));
                }
            }else{
                String empty="Your captures will appear here. Use the test button to record your first V4 memory.";
                y=add("paragraph",x,y,w,42+wrapped(g,empty,body,w-24),"MEMORY GALLERY\n"+empty);
            }
            if(!state.lastCapture.isEmpty()){
                y=add("paragraph",x,y,w,42+wrapped(g,state.lastCapture,font(12,false),w-24),"LAST SAVED FILE\n"+state.lastCapture);
                y=add("openLatest",x,y,w,42,"");hit("openLatest","Open your last saved screenshot",blocks.get(blocks.size()-1).r);
            }
            String help="The ETA keeps its full date and timezone. It assumes nonstop training at your observed pace. Hold Alt and drag overlays to move them. Turning on Reduced motion stops decorative motion, not data updates.";
            y=add("paragraph",x,y,w,42+wrapped(g,help,body,w-24),"FORECAST & OVERLAYS\n"+help);
        }
        if(!state.notice.isEmpty())y=add("notice",x,y,w,18+wrapped(g,state.notice,body,w-20),state.notice);
        y=add("footer",x,y,w,28,"");
        layoutHeight=y;dirty=false;g.dispose();
    }
    private String measuredSummary(){
        if(state.measured.isEmpty()||state.measured.startsWith("No measured"))return "No comparison yet. Train against different monsters with the same loadout to collect samples.";
        return "Fastest in your recorded samples:\n"+state.measured.replaceAll("\\s*\\([^)]*XP/hr\\)","")+"\n\nGear or skill changes begin a new comparison.";
    }
    private String predictedSummary(){
        if(state.predicted.isEmpty())return "Waiting for loadout data.";
        java.util.List<String[]> options=new java.util.ArrayList<>();
        java.util.regex.Pattern p=java.util.regex.Pattern.compile("^([^:]+):\\s*~?([0-9,]+)\\s*XP/hr.*$");
        for(String row:state.predicted.split("\n")){java.util.regex.Matcher m=p.matcher(row);if(m.matches())options.add(new String[]{m.group(1),m.group(2).replace(",","")});}
        options.sort((a,b)->Long.compare(Long.parseLong(b[1]),Long.parseLong(a[1])));
        if(options.isEmpty())return state.predicted.split("\n")[0]+"\n\nThis is a model, not a verified maximum.";
        StringBuilder result=new StringBuilder("Modelled order for your current setup:\n");
        for(int i=0;i<options.size()&&i<5;i++)result.append(i+1).append(". ").append(options.get(i)[0]).append("\n");
        return result.toString()+"\nAn estimate, not a proven winner. It does not change your route target automatically.";
    }
    @Override public Dimension getPreferredSize(){ensureLayout();return new Dimension(Math.max(160,getWidth()>0?getWidth():242),(int)Math.ceil(layoutHeight*uiScale));}
    @Override public Dimension getMinimumSize(){return new Dimension(160,100);}
    @Override public Dimension getPreferredScrollableViewportSize(){return new Dimension(242,780);}
    @Override public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 24;}
    @Override public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(60,r.height-40);}
    @Override public boolean getScrollableTracksViewportWidth(){return true;}
    @Override public boolean getScrollableTracksViewportHeight(){return false;}
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);ensureLayout();Graphics2D g=(Graphics2D)graphics.create();quality(g);
        g.setColor(BG);g.fillRect(0,0,getWidth(),getHeight());
        g.scale(uiScale,uiScale);
        float pageAlpha=state.preferences.animate()?(float)Math.min(1,.65+(System.nanoTime()-pageChanged)/1e9*2.5):1;
        for(Block b:blocks){Graphics2D layer=(Graphics2D)g.create();if(!b.kind.equals("brand")&&!b.kind.equals("tabs"))layer.setComposite(AlphaComposite.SrcOver.derive(pageAlpha));paintBlock(layer,b);layer.dispose();}
        if(state.preferences.animate()&&rippleStarted>0){double age=(System.nanoTime()-rippleStarted)/1e9;if(age<.45){g.setColor(alpha(state.preferences.theme.secondary,(int)(50*(1-age/.45))));g.setStroke(new BasicStroke(1));double size=age*60;g.draw(new Ellipse2D.Double(ripplePoint.x-size,ripplePoint.y-size,size*2,size*2));}}
        g.dispose();
    }
    private void title(Graphics2D g,String value,Rectangle r,Color color){line(g,value,r.x+12,r.y+12,caption,color);}
    private void paintBlock(Graphics2D g,Block b) {
        Rectangle r=b.r;int x=r.x,y=r.y,w=r.width;boolean animate=state.preferences.animate();Color accent=state.preferences.theme.secondary;Color primary=state.preferences.theme.primary;
        switch(b.kind){
            case "brand":{
                icon(g,"brand",x-3,y-2,37);
                Font f=new Font(Font.SERIF,Font.BOLD,w<210?16:18);
                if(w<195){line(g,"COMBAT",x+34,y+1,font(14,true),FG);line(g,"COACH",x+34,y+17,font(14,true),FG);}
                else line(g,"COMBAT COACH",x+34,y+6,f,FG);
                line(g,"+",x+w-22,y+6,font(20,false),CYAN);break;}
            case "tabs":{
                premiumCard(g,r,accent,.05);String[] names={"TRAINING","ANALYSIS","SETTINGS"};
                for(int i=0;i<3;i++)center(g,names[i],x+(2*i+1)*w/6,y+10,font(w<200?10:11,true),tab==i?CYAN:MUTED);
                double p=state.reducedMotion?tab:tabPosition;int left=(int)(x+(p+.18)*w/3),right=(int)(x+(p+.82)*w/3);
                Shape glowLine=new Line2D.Double(left,y+r.height-2,right,y+r.height-2);glow(g,glowLine,CYAN,3,1);g.setColor(CYAN);g.setStroke(new BasicStroke(2));g.draw(glowLine);break;}
            case "training":{
                premiumCard(g,r,accent,hover("focus"));card(g,new Rectangle(x+9,y+12,48,48),new Color(41,59,77));
                icon(g,state.skill.toLowerCase(Locale.ROOT),x+6,y+7,54);
                line(g,w<195?"TRAINING":"CURRENT TRAINING",x+67,y+11,font(10,true),CYAN);
                text(g,state.loggedIn?state.skill+"\nLevel "+state.level:"Log in to begin",x+67,y+29,w-78,font(15,true),FG);break;}
            case "monster":{
                premiumCard(g,r,accent,0);card(g,new Rectangle(x+9,y+12,48,46),new Color(37,56,73));
                icon(g,state.spot.toLowerCase(Locale.ROOT).contains("cow")?"cow":"brand",x+6,y+8,53);
                line(g,w<195?"MONSTER":"SUGGESTED MONSTER",x+67,y+10,font(10,true),CYAN);
                text(g,state.spot,x+67,y+28,w-78,font(14,true),FG);if(!state.routeStatus.isEmpty())text(g,state.routeStatus,x+10,y+r.height-30,w-20,font(10,true),primary);break;}
            case "progress":{
                line(g,"LEVEL GOAL",x+10,y+10,caption,GOLD);
                Rectangle control=new Rectangle(x+w-68,y+7,59,25);card(g,control,new Color(52,87,106));center(g,state.goal+"  +",control.x+control.width/2,control.y+5,font(13,true),FG);
                if(w>=180){
                    int radius=Math.min(78,Math.max(39,(w-112)/2));int cx=x+radius+11,cy=y+94;
                    ringV4(g,cx,cy,radius,state.reducedMotion?state.progress():displayProgress,state.goal,phase,state.preferences,gainPulse());
                    int mx=x+2*radius+29;int mw=x+w-mx-7;
                    metric(g,"CURRENT XP",number(state.xp),mx,y+44,mw);
                    metric(g,"TARGET XP",number(CoachUiState.xpForLevel(state.goal)),mx,y+84,mw);
                    metric(g,"XP REMAINING",number(state.remaining()),mx,y+124,mw);
                }else{
                    ringV4(g,x+w/2,y+90,46,state.reducedMotion?state.progress():displayProgress,state.goal,phase,state.preferences,gainPulse());
                    metric(g,"CURRENT XP",number(state.xp),x+12,y+155,w-24);
                    metric(g,"TARGET XP",number(CoachUiState.xpForLevel(state.goal)),x+12,y+194,w-24);
                    line(g,number(state.remaining())+" XP left",x+12,y+234,font(12,false),MUTED);
                }
                if(animate&&gainPulse()>0&&lastGainAmount>0){
                    g.setColor(alpha(primary,(int)(gainPulse()*200)));g.setFont(font(10,true));
                    String ping="+"+number(lastGainAmount)+" XP";g.drawString(ping,x+12,y+r.height-1);
                }
                break;}
            case "forecast":{
                double near=state.complete()?1:state.secondsRemaining()>0&&state.secondsRemaining()<120?.7:0;
                forecastV4(g,r,phase,state.preferences,near,hover("forecast"));
                miniClock(g,x+12,y+12,14,accent,phase,animate);
                line(g,state.complete()?"GOAL REACHED":"LIVE FORECAST",x+33,y+11,font(11,true),accent);
                Instant now=now();ZoneId zone=ZoneId.systemDefault();
                String date=state.complete()?state.skill+" level "+state.goal:state.secondsRemaining()<0?"Learning your pace":state.finishDate(now,zone);
                int yy=y+38;
                if(!state.preferences.compactLayout){
                    String relative=state.complete()?"MILESTONE COMPLETE":state.relativeDay(now,zone);
                    if(relative.isEmpty())relative="WAITING FOR XP";
                    g.setFont(font(9,true));int pill=Math.min(w-24,g.getFontMetrics().stringWidth(relative)+14);
                    g.setColor(alpha(state.preferences.theme.gold,19));g.fillRoundRect(x+9,yy-2,pill,15,8,8);
                    line(g,relative,x+15,yy,font(9,true),state.preferences.theme.gold);yy+=19;
                }
                yy+=text(g,date,x+12,yy,w-24,font(16,true),FG)+3;
                String time=state.complete()?"Choose your next target":state.secondsRemaining()<0?"Start training for an ETA":state.finishTime(now,zone);
                yy+=text(g,time,x+12,yy,w-24,font(14,true),FG)+7;
                g.setColor(new Color(151,189,208,35));g.setStroke(new BasicStroke(1));g.drawLine(x+12,yy,x+w-12,yy);yy+=8;
                yy+=text(g,state.duration(),x+12,yy,w-24,font(12,true),state.complete()?primary:MUTED)+4;
                line(g,state.paceLabel(),x+12,yy,font(9,true),state.preferences.theme.gold);
                line(g,"At this pace, training nonstop",x+12,y+r.height-18,font(w<195?9:10,false),MUTED);break;}
            case "navigate":button(g,r,"Navigate to monster",true,hovered.equals("navigate"),phase,animate);break;
            case "skills":{
                premiumCard(g,r,accent,0);title(g,"COMBAT SKILLS",r,CYAN);
                int cellW=(w-26)/3;int cellH=39;
                for(int i=0;i<7;i++){
                    int tx=x+9+(i%3)*(cellW+4),ty=y+36+(i/3)*(cellH+5);boolean active=state.skill.equalsIgnoreCase(SKILLS[i]);
                    Rectangle tile=new Rectangle(tx,ty,cellW,cellH);premiumCard(g,tile,active?primary:new Color(62,90,110),hover("skill:"+i));
                    if(active){g.setColor(alpha(primary,(int)(18+gainPulse()*25)));g.fillRoundRect(tx+1,ty+1,cellW-2,cellH-2,10,10);}
                    int iconSize=cellW<61?27:33;icon(g,SKILLS[i].toLowerCase(Locale.ROOT),tx+1,ty+5,iconSize);
                    int textX=tx+iconSize-1;
                    line(g,SHORT[i],textX,ty+5,font(cellW<61?9:10,false),active?GREEN:MUTED);
                    line(g,Integer.toString(state.skillLevel(i)),textX,ty+17,font(16,true),FG);
                }
                break;}
            case "link":{premiumCard(g,r,accent,hover(b.kind.equals("link")?"analysis":"gallery"));line(g,b.text,x+12,y+13,font(13,true),CYAN);line(g,">",x+w-23,y+11,font(16,false),CYAN);break;}
            case "screenshots":{
                premiumCard(g,r,accent,hover(b.kind.equals("link")?"analysis":"gallery"));line(g,"MEMORIES",x+12,y+9,caption,accent);
                line(g,state.capturesList.isEmpty()?(state.captures?"Milestone captures on":"Milestone captures off"):state.capturesList.size()+" recent captures",x+12,y+28,font(11,false),MUTED);
                g.setColor(state.captures?GREEN:new Color(83,97,109));g.fillOval(x+w-23,y+12,8,8);break;}
            case "paragraph":{
                premiumCard(g,r,accent,0);int split=b.text.indexOf('\n');String heading=split<0?b.text:b.text.substring(0,split),value=split<0?"":b.text.substring(split+1);
                title(g,heading,r,CYAN);text(g,value,x+12,y+34,w-24,heading.equals("LAST SAVED FILE")?font(12,false):body,MUTED);break;}
            case "notice":{card(g,r,new Color(125,104,61));text(g,b.text,x+10,y+9,w-20,body,GOLD);break;}
            case "choice":{
                String[] p=b.text.split("\n",3);premiumCard(g,r,accent,hover(p[0]));
                line(g,p[1].toUpperCase(Locale.US),x+12,y+9,caption,accent);
                text(g,p[2],x+12,y+28,w-40,font(13,true),FG);line(g,">",x+w-23,y+20,font(17,false),accent);break;}
            case "memory":{
                CoachCaptureRecord item=state.capturesList.stream().filter(c->c.id.equals(b.text)).findFirst().orElse(null);if(item==null)break;
                premiumCard(g,r,accent,0);BufferedImage thumb=thumbnails.get(item.id);
                Rectangle thumbRect=new Rectangle(x+9,y+10,53,61);
                if(thumb!=null){Shape old=g.getClip();g.clip(new RoundRectangle2D.Double(thumbRect.x,thumbRect.y,thumbRect.width,thumbRect.height,8,8));
                    g.drawImage(thumb,thumbRect.x,thumbRect.y,thumbRect.width,thumbRect.height,null);g.setClip(old);
                }else icon(g,item.skill.toLowerCase(Locale.ROOT),x+10,y+14,48);
                int metaY=y+12+text(g,item.skill+" "+item.level,x+72,y+12,w-84,font(13,true),FG)+4;
                String date=memoryDate(item);
                metaY+=text(g,date,x+72,metaY,w-84,font(10,false),MUTED)+3;
                if(item.test)line(g,"TEST CAPTURE",x+72,metaY,font(9,true),state.preferences.theme.gold);
                button(g,new Rectangle(x+8,y+memoryActionY(g,item,w),w-16,29),"View game image",false,hovered.equals("view:"+item.id),phase,animate);
                if(!item.cardFile.isEmpty())button(g,new Rectangle(x+8,y+memoryActionY(g,item,w)+33,w-16,29),"View milestone card",false,hovered.equals("card:"+item.id),phase,animate);
                break;}
            case "toggle":{
                premiumCard(g,r,accent,0);String[] parts=b.text.split("\n",2);boolean on=isOn(parts[0]);
                text(g,parts[1],x+11,y+16,w-73,font(12,true),FG);
                g.setColor(on?new Color(22,109,82):new Color(49,66,79));g.fillRoundRect(x+w-53,y+18,40,18,18,18);g.setColor(on?GREEN:MUTED);g.fillOval(x+w-51+(on?21:0),y+20,14,14);break;}
            case "test":button(g,r,"Test capture",true,hovered.equals("testCapture"),phase,animate);break;
            case "openFolder":button(g,r,"Copy capture folder path",false,hovered.equals("openFolder"),phase,animate);break;
            case "openLatest":button(g,r,"View last screenshot",false,hovered.equals("openLatest"),phase,animate);break;
            case "refresh":button(g,r,"Refresh recommendation",false,hovered.equals("refresh"),phase,animate);break;
            case "footer":center(g,w<230?"Combat Coach  /  v"+CoachUiState.VERSION:"Small gains. Big progress.  /  v"+CoachUiState.VERSION,x+w/2,y+8,font(10,false),MUTED);break;
            default:break;
        }
    }
    private static String memoryDate(CoachCaptureRecord record){return java.time.Instant.ofEpochMilli(record.createdAt).atZone(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a",Locale.US));}
    private static int memoryActionY(Graphics2D g,CoachCaptureRecord record,int width){return Math.max(85,12+textHeight(g,record.skill+" "+record.level,font(13,true),width-84)+4+textHeight(g,memoryDate(record),font(10,false),width-84)+3+(record.test?13:0)+8);}
    private double gainPulse(){if(!state.preferences.animate())return 0;return Math.max(0,1-(System.nanoTime()-lastGainNanos)/1e9/1.25);}
    private boolean isOn(String key){switch(key){
        case "hud":return state.hud;case "hudCompact":return state.preferences.compactHud;
        case "compactLayout":return state.preferences.compactLayout;case "popups":return state.popups;
        case "motion":return state.reducedMotion;case "includeCelebration":return state.preferences.includeCelebration;
        case "saveCard":return state.preferences.saveCard;case "includeFrame":return state.preferences.includeFrame;
        default:return state.captures;
    }}
    private static String number(long n){return String.format(Locale.US,"%,d",n);}
    private void metric(Graphics2D g,String title,String value,int x,int y,int width){
        line(g,title,x,y,font(9,true),MUTED);text(g,value,x,y+14,width,font(12,true),FG);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(35,57,72));g.drawLine(x,y+33,x+width,y+33);
    }
    /** The tests call the same painter used in the live client, not a separate mockup. */
    public BufferedImage renderForTest(int width,CoachUiState s,int page) {
        testNow=Instant.parse("2026-10-12T19:43:00Z");
        setState(s);setTab(page);setSize(width,800);ensureLayout();setSize(width,(int)Math.ceil(layoutHeight*uiScale));
        displayProgress=s.progress();tabPosition=page;
        BufferedImage image=new BufferedImage(width,(int)Math.ceil(layoutHeight*uiScale),BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();paint(g);g.dispose();testNow=null;return image;
    }
    public BufferedImage renderAtTimeForTest(int width,CoachUiState s,int page,double seconds){
        testNow=Instant.parse("2026-10-12T19:43:00Z");
        setState(s);setTab(page);setSize(width,900);ensureLayout();setSize(width,(int)Math.ceil(layoutHeight*uiScale));
        displayProgress=s.progress();tabPosition=page;phase=seconds;pageChanged=0;
        BufferedImage im=new BufferedImage(width,getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=im.createGraphics();paint(g);g.dispose();testNow=null;return im;
    }
    public void assertWidthSafe(){ensureLayout();for(Block b:blocks)if(b.r.x<0||b.r.x+b.r.width>layoutWidth)throw new IllegalStateException("Outside viewport: "+b.kind);for(Hit h:hits)if(h.r.x<0||h.r.x+h.r.width>layoutWidth)throw new IllegalStateException("Click outside viewport: "+h.id);}
}
