package com.osrs.combatcoach;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import static com.osrs.combatcoach.CoachArt.*;

/** Display-only HUD, using the same visual language and full-date forecast as the dashboard. */
final class CombatCoachOverlay extends Overlay {
    private final Supplier<CoachUiState> state;
    private double shown,phase;
    private long lastFrame=System.nanoTime();
    private String skill="";
    CombatCoachOverlay(Plugin plugin,Supplier<CoachUiState> state){super(plugin);this.state=state;setPosition(OverlayPosition.TOP_LEFT);setLayer(OverlayLayer.ABOVE_WIDGETS);}
    @Override public Dimension render(Graphics2D original){
        CoachUiState s=state.get();if(s==null||!s.hud||!s.loggedIn)return null;
        CoachPreferences prefs=s.preferences;long now=System.nanoTime();double dt=Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;
        if(!skill.equals(s.skill)){shown=s.progress();skill=s.skill;}
        if(prefs.animate()){shown+=(s.progress()-shown)*(1-Math.exp(-dt*7));phase+=dt;}else shown=s.progress();
        Graphics2D g=(Graphics2D)original.create();quality(g);boolean compact=prefs.compactHud;
        int w=compact?234:273;int textX=compact?70:84;int tw=w-textX-13;
        int linesHeight=textHeight(g,s.skill+"  "+s.level+" > "+s.goal,font(12,true),tw)+4
            +textHeight(g,s.complete()?"Goal reached":s.duration(),font(11,true),tw)+4
            +(s.secondsRemaining()>=0?textHeight(g,s.finishDate(Instant.now(),ZoneId.systemDefault()),font(10,false),tw)+textHeight(g,s.finishTime(Instant.now(),ZoneId.systemDefault()),font(10,false),tw)+5:20)
            +(compact?0:textHeight(g,s.spot,font(11,false),tw));
        int h=Math.max(compact?124:157,(compact?12:39)+linesHeight+15);
        Rectangle box=new Rectangle(1,1,w-2,h-2);
        forecastV4(g,box,phase,prefs,s.complete()?.8:0,0);
        int radius=compact?24:29,cx=compact?34:41,cy=compact?61:74;
        g.setStroke(new BasicStroke(6,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(38,59,76));
        g.draw(new Arc2D.Double(cx-radius,cy-radius,radius*2,radius*2,0,360,Arc2D.OPEN));g.setColor(prefs.theme.primary);
        g.draw(new Arc2D.Double(cx-radius,cy-radius,radius*2,radius*2,90,-360*shown,Arc2D.OPEN));
        center(g,Integer.toString(s.goal),cx,cy-13,font(compact?18:22,true),FG);center(g,"GOAL",cx,cy+12,font(8,true),prefs.theme.secondary);
        int y=12;
        if(!compact){icon(g,s.skill.toLowerCase(java.util.Locale.ROOT),10,7,21);line(g,"COMBAT COACH",36,12,font(10,true),prefs.theme.secondary);y=39;}
        y+=text(g,s.skill+"  "+s.level+" > "+s.goal,textX,y,tw,font(12,true),FG)+4;
        y+=text(g,s.complete()?"Goal reached":s.duration(),textX,y,tw,font(11,true),prefs.theme.primary)+4;
        if(s.secondsRemaining()>=0){
            y+=text(g,s.finishDate(Instant.now(),ZoneId.systemDefault()),textX,y,tw,font(10,false),MUTED);
            y+=text(g,s.finishTime(Instant.now(),ZoneId.systemDefault()),textX,y,tw,font(10,false),MUTED)+5;
        }else y+=text(g,"Learning your pace",textX,y,tw,font(10,false),MUTED)+5;
        if(!compact)text(g,s.spot,textX,y,tw,font(11,false),prefs.theme.secondary);
        g.dispose();return new Dimension(w,h);
    }
}
