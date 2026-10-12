package com.osrs.combatcoach;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import static com.osrs.combatcoach.CoachArt.*;

/** Owned by the screenshot plugin so captures include the card even with Combat Coach disabled. */
public final class CoachToastOverlay extends Overlay {
    private final Supplier<CoachPreferences> style;
    private final Supplier<Boolean> enabled;
    private final Supplier<Dimension> canvas;
    private final Consumer<String> rendered;
    private final BiConsumer<String,String> open;
    private final java.util.function.LongSupplier nanoClock;
    private volatile CoachCaptureRecord record;
    private volatile String phase="",message="";
    private volatile long began;
    private volatile boolean mustCapture,drawing;
    private volatile double scale=1;
    private volatile BufferedImage thumbnail;
    private volatile CoachPreferences captureStyle;
    private volatile Rectangle view=new Rectangle(),folder=new Rectangle(),card=new Rectangle(),dismiss=new Rectangle();
    private boolean pressed;
    public final MouseAdapter mouse=new MouseAdapter(){
        private Point local(MouseEvent e){Rectangle b=getBounds();return new Point((int)((e.getX()-b.x)/scale),(int)((e.getY()-b.y)/scale));}
        private boolean inside(MouseEvent e){return drawing&&getBounds().contains(e.getPoint());}
        @Override public MouseEvent mousePressed(MouseEvent e){if(!e.isAltDown()&&e.getButton()==MouseEvent.BUTTON1&&inside(e)){pressed=true;e.consume();}return e;}
        @Override public MouseEvent mouseReleased(MouseEvent e){if(pressed){pressed=false;e.consume();if(inside(e)){Point p=local(e);CoachCaptureRecord r=record;if(r!=null){
            if(view.contains(p)&&!r.file.isEmpty())open.accept("view",r.file);
            else if(folder.contains(p)&&!r.file.isEmpty())open.accept("folder",r.file);
            else if(card.contains(p)&&!r.cardFile.isEmpty())open.accept("view",r.cardFile);
            else if(dismiss.contains(p)&&!mustCapture)dismiss();
        }}}return e;}
        @Override public MouseEvent mouseClicked(MouseEvent e){if(!e.isAltDown()&&e.getButton()==MouseEvent.BUTTON1&&inside(e))e.consume();return e;}
    };
    public CoachToastOverlay(Plugin plugin,Supplier<CoachPreferences> style,Supplier<Boolean> enabled,Supplier<Dimension> canvas,Consumer<String> rendered,BiConsumer<String,String> open){
        this(plugin,style,enabled,canvas,rendered,open,System::nanoTime);
    }
    CoachToastOverlay(Plugin plugin,Supplier<CoachPreferences> style,Supplier<Boolean> enabled,Supplier<Dimension> canvas,Consumer<String> rendered,BiConsumer<String,String> open,java.util.function.LongSupplier clock){
        super(plugin);this.style=style;this.enabled=enabled;this.canvas=canvas;this.rendered=rendered;this.open=open;this.nanoClock=clock;
        setPosition(OverlayPosition.BOTTOM_LEFT);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(PRIORITY_HIGH);
    }
    public void prepare(CoachCaptureRecord r,boolean include){captureStyle=style.get();record=r;message="";thumbnail=null;phase="PREPARING";began=nanoClock.getAsLong();mustCapture=include;}
    public void capturing(){phase="CAPTURING";}
    public void saved(CoachCaptureRecord r,String note){record=r;phase="SAVED";message=note;began=nanoClock.getAsLong();mustCapture=false;}
    public void failed(CoachCaptureRecord r,String error){record=r;phase="FAILED";message=error;began=nanoClock.getAsLong();mustCapture=false;}
    public void setThumbnail(String id,BufferedImage image){CoachCaptureRecord r=record;if(r!=null&&r.id.equals(id))thumbnail=image;}
    public void dismiss(){record=null;drawing=false;mustCapture=false;thumbnail=null;}
    @Override public Dimension render(Graphics2D original){
        CoachCaptureRecord r=record;if(r==null||(!mustCapture&&(phase.equals("PREPARING")||phase.equals("CAPTURING")))||(!mustCapture&&!enabled.get())){drawing=false;return null;}
        CoachPreferences prefs=mustCapture&&captureStyle!=null?captureStyle:style.get();double age=(nanoClock.getAsLong()-began)/1e9;
        if(!mustCapture&&age>22){dismiss();return null;}
        boolean success=phase.equals("SAVED"),failure=phase.equals("FAILED");
        int w=330,h=success?426:failure?365:311;
        if(success&&!r.cardFile.isEmpty())h+=32;
        Graphics2D measure=(Graphics2D)original.create();quality(measure);
        if(failure)h=320+textHeight(measure,message,font(11,false),w-36)+45;
        measure.dispose();Dimension available=canvas.get();
        scale=Math.min(1,Math.min(Math.max(120,available.width-20)/(double)w,Math.max(170,available.height-25)/(double)h));
        Graphics2D g=(Graphics2D)original.create();quality(g);g.scale(scale,scale);
        float alpha=prefs.animate()?(float)Math.min(1,age/.35):1;
        if(!mustCapture&&age>21.4&&prefs.animate())alpha=(float)((22-age)/.6);
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0,Math.min(1,alpha))));
        premiumCard(g,new Rectangle(3,3,w-6,h-6),failure?RED:prefs.theme.gold,.2);
        // Capture freezes the fully revealed hero so the following framebuffer contains the same card.
        double heroAge=mustCapture&&age>=.9?1.2:age;
        CoachMilestoneArt.hero(g,w,r,prefs,heroAge);
        view=new Rectangle();folder=new Rectangle();card=new Rectangle();dismiss=new Rectangle();
        if(success){
            line(g,"Screenshot saved",18,268,font(14,true),prefs.theme.secondary);
            line(g,r.includesCelebration?"Your level-up card is in the image.":"Gameplay capture confirmed.",18,288,font(11,false),MUTED);
            BufferedImage preview=thumbnail;
            if(preview!=null)g.drawImage(preview,18,309,85,49,null);
            else {icon(g,"brand",30,312,39);}
            String label=r.test?"Test memory":"Level "+r.level+" memory";
            text(g,label,116,309,w-134,font(12,true),FG);
            text(g,r.cardFile.isEmpty()?"Saved to your Milestones folder":"Gameplay + clean card PNG",116,330,w-134,font(10,false),MUTED);
            view=new Rectangle(14,370,147,34);folder=new Rectangle(169,370,147,34);
            button(g,view,"View screenshot",false,false,0,false);button(g,folder,"Copy folder path",false,false,0,false);
            if(!r.cardFile.isEmpty()){card=new Rectangle(14,407,w-28,29);button(g,card,"View clean milestone card",false,false,0,false);}
            dismiss=new Rectangle(w-32,7,25,25);line(g,"x",w-24,11,font(15,true),GOLD);
        }else if(failure){
            line(g,"Capture needs attention",18,268,font(14,true),RED);text(g,message,18,291,w-36,font(11,false),MUTED);
            dismiss=new Rectangle(14,h-43,w-28,32);button(g,dismiss,"Dismiss",false,false,0,false);
        }else{
            line(g,phase.equals("CAPTURING")?"Capturing this celebration...":"Preparing your milestone...",18,268,font(12,true),prefs.theme.secondary);
            line(g,"Saved confirmation follows the file write.",18,287,font(10,false),MUTED);
        }
        drawing=true;g.dispose();
        if(mustCapture&&age>=.9&&phase.equals("PREPARING"))rendered.accept(r.id);
        return new Dimension((int)Math.ceil(w*scale),(int)Math.ceil(h*scale));
    }
}
