package com.osrs.combatcoach;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static com.osrs.combatcoach.CoachArt.*;

/** The SAME painter creates the on-screen celebration and optional standalone keepsake PNG. */
public final class CoachMilestoneArt {
    public static final int HERO_HEIGHT=260;
    private CoachMilestoneArt(){}
    public static void hero(Graphics2D original,int width,CoachCaptureRecord record,CoachPreferences prefs,double elapsed){
        Graphics2D g=(Graphics2D)original.create();quality(g);Color gold=prefs.theme.gold;
        Rectangle r=new Rectangle(4,4,width-8,HERO_HEIGHT-8);premiumCard(g,r,gold,.15);
        Shape clip=new RoundRectangle2D.Double(r.x,r.y,r.width,r.height,16,16);g.clip(clip);
        BufferedImage scenery=image("forecast");if(scenery!=null){g.setComposite(AlphaComposite.SrcOver.derive(.25f));g.drawImage(scenery,4,4,width-8,HERO_HEIGHT-8,null);g.setComposite(AlphaComposite.SrcOver);}
        g.setPaint(new GradientPaint(0,0,new Color(10,22,32,110),0,HERO_HEIGHT,new Color(5,14,23,220)));g.fillRect(4,4,width-8,HERO_HEIGHT-8);
        double t=prefs.animate()?elapsed:1.2;int cx=width/2,cy=91;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx,cy),104,new float[]{0,.45f,1},new Color[]{alpha(gold,45),alpha(gold,12),alpha(gold,0)}));g.fillOval(cx-106,cy-106,212,212);
        g.setStroke(new BasicStroke(1));
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12+t*.065;double inner=51+(i%3)*2,outer=76+(i%4)*5;
            g.setColor(alpha(gold,17+i%4*6));g.draw(new Line2D.Double(cx+Math.cos(a)*inner,cy+Math.sin(a)*inner,cx+Math.cos(a)*outer,cy+Math.sin(a)*outer));
        }
        g.setColor(alpha(gold,90));g.drawOval(cx-48,cy-48,96,96);g.setColor(alpha(gold,30));g.drawOval(cx-56,cy-56,112,112);
        int particles=prefs.effects==2?30:12;
        for(int i=0;i<particles;i++){
            double p=(i*.071+(prefs.animate()?t*.055:0))%1;
            int x=17+(i*79)%Math.max(1,width-34),y=15+(int)(p*210);
            int a=(int)(25+55*Math.sin(p*Math.PI));g.setColor(alpha(gold,a));
            int size=i%8==0?3:1;g.fillOval(x,y,size+1,size+1);
            if(i%8==0){g.drawLine(x-3,y+1,x+4,y+1);g.drawLine(x+1,y-3,x+1,y+4);}
        }
        icon(g,record.skill.toLowerCase(Locale.ROOT),cx-41,cy-41,82);
        center(g,record.test?"TEST CAPTURE":"MILESTONE UNLOCKED",cx,18,font(10,true),gold);
        center(g,record.test?"YOUR MEMORY CARD":"LEVEL UP!",cx,154,new Font(Font.SERIF,Font.BOLD,26),gold);
        center(g,record.skill,cx,187,font(15,true),FG);
        center(g,"Level "+record.level,cx,208,font(27,true),FG);
        g.dispose();
    }
    public static BufferedImage keepsake(CoachCaptureRecord record,CoachPreferences prefs){
        // A transparent, high-resolution shareable card; no false "saved" text or invented gameplay.
        final int logicalW=350,logicalH=325,scale=2;
        BufferedImage image=new BufferedImage(logicalW*scale,logicalH*scale,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.scale(scale,scale);quality(g);
        premiumCard(g,new Rectangle(4,4,logicalW-8,logicalH-8),prefs.theme.gold,.3);
        hero(g,logicalW,record,new CoachPreferences(prefs.theme,prefs.effects,true,false,false,true,true),1.2);
        String time=Instant.ofEpochMilli(record.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, uuuu  h:mm a z",Locale.US));
        center(g,time,logicalW/2,266,font(12,true),FG);
        center(g,"COMBAT COACH  /  A MOMENT EARNED",logicalW/2,293,font(9,true),prefs.theme.secondary);
        g.dispose();return image;
    }
}
