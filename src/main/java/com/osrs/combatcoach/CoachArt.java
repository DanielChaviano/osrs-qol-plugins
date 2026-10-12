package com.osrs.combatcoach;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Shared real Java2D artwork, used by the sidebar and both in-game overlays. */
public final class CoachArt {
    public static final Color BG = new Color(8, 16, 24), FG = new Color(239, 246, 253);
    public static final Color MUTED = new Color(169, 188, 206), CYAN = new Color(79, 215, 244);
    public static final Color GREEN = new Color(69, 242, 174), GOLD = new Color(255, 213, 113);
    public static final Color RED = new Color(255, 133, 133);
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    public static Font font(float size, boolean bold) { return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 12).deriveFont(size); }
    static {
        for (String key : new String[]{"brand", "cow", "attack", "strength", "defence", "hitpoints", "ranged", "magic", "prayer", "forecast"}) {
            try (java.io.InputStream in = CoachArt.class.getResourceAsStream("/com/osrs/combatcoach/premium/" + key + ".png")) {
                if (in != null) IMAGES.put(key, ImageIO.read(in));
            } catch (IOException ignored) { /* Null assets use a visible, safe fallback. */ }
        }
    }
    public static boolean assetsPresent() { return IMAGES.size() == 10; }
    public static BufferedImage image(String key) { return IMAGES.get(key); }
    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }
    public static void icon(Graphics2D g, String key, int x, int y, int size) {
        BufferedImage im = image(key.toLowerCase(Locale.ROOT));
        if (im != null) g.drawImage(im, x, y, size, size, null);
        else { g.setColor(CYAN); g.setStroke(new BasicStroke(2)); g.drawOval(x+5,y+5,size-10,size-10); }
    }
    public static void card(Graphics2D g, Rectangle r, Color edge) {
        Shape shape = new RoundRectangle2D.Double(r.x+.5,r.y+.5,r.width-1,r.height-1,13,13);
        g.setPaint(new GradientPaint(r.x,r.y,new Color(24,39,55),r.x+r.width,r.y+r.height,new Color(9,18,29)));
        g.fill(shape); g.setColor(edge); g.setStroke(new BasicStroke(1)); g.draw(shape);
        g.setColor(new Color(255,255,255,8)); g.drawLine(r.x+10,r.y+1,r.x+r.width-10,r.y+1);
    }
    public static void glow(Graphics2D g, Shape shape, Color c, int layers, float strength) {
        for (int i=layers;i>0;i--) {
            g.setStroke(new BasicStroke(i*2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.max(0,Math.min(65,(int)(strength*30/(i+1))))));
            g.draw(shape);
        }
    }
    public static void line(Graphics2D g, String text, float x, float y, Font f, Color c) {
        g.setFont(f);g.setColor(c);g.drawString(text,x,y+g.getFontMetrics().getAscent());
    }
    /** Word-wrap, including arbitrary-length paths. Never silently ellipsize strings. */
    public static List<String> lines(Graphics2D g, String raw, Font f, int width) {
        g.setFont(f);FontMetrics m=g.getFontMetrics();List<String> out=new ArrayList<>();
        for(String paragraph:(raw==null?"":raw).split("\\n",-1)) {
            if(paragraph.isEmpty()){out.add("");continue;}
            StringBuilder row=new StringBuilder();
            for(String word:paragraph.split("\\s+")) {
                if(m.stringWidth(word)>Math.max(1,width)) {
                    if(row.length()>0){out.add(row.toString());row.setLength(0);}
                    StringBuilder part=new StringBuilder();
                    for(int i=0;i<word.length();i++) {
                        if(part.length()>0&&m.stringWidth(part.toString()+word.charAt(i))>width){out.add(part.toString());part.setLength(0);}
                        part.append(word.charAt(i));
                    }
                    row.append(part);
                } else if(row.length()>0&&m.stringWidth(row+" "+word)>width) {
                    out.add(row.toString());row.setLength(0);row.append(word);
                } else {if(row.length()>0)row.append(' ');row.append(word);}
            }
            if(row.length()>0)out.add(row.toString());
        }
        return out;
    }
    public static int textHeight(Graphics2D g,String s,Font f,int width) {
        return lines(g,s,f,width).size()*(g.getFontMetrics(f).getHeight()+2);
    }
    public static int text(Graphics2D g,String s,int x,int y,int width,Font f,Color c) {
        int cy=y;for(String row:lines(g,s,f,width)){line(g,row,x,cy,f,c);cy+=g.getFontMetrics(f).getHeight()+2;}
        return cy-y;
    }
    public static void forecast(Graphics2D g,Rectangle r,double phase,boolean animate) {
        Shape shape=new RoundRectangle2D.Double(r.x,r.y,r.width,r.height,13,13);
        Shape old=g.getClip();g.clip(shape);
        BufferedImage im=image("forecast");if(im!=null)g.drawImage(im,r.x,r.y,r.width,r.height,null);
        g.setPaint(new GradientPaint(r.x,r.y,new Color(4,16,25,225),r.x+r.width,r.y,new Color(4,16,25,95)));
        g.fill(r);
        g.setPaint(new GradientPaint(r.x,r.y,new Color(4,12,19,40),r.x,r.y+r.height,new Color(4,12,19,220)));g.fill(r);
        if(animate){for(int i=0;i<7;i++){double t=(phase*.04+i*.137)%1;int x=r.x+(int)((.63+i*.04)*r.width);int y=r.y+r.height-(int)(t*r.height);g.setColor(new Color(255,221,151,65));g.fillOval(x,y,2,2);}}
        g.setClip(old);g.setStroke(new BasicStroke(1));g.setColor(new Color(182,142,68));g.draw(shape);
    }
    public static void ring(Graphics2D g,int cx,int cy,int radius,double fraction,int target,double phase,boolean animate) {
        Arc2D track=new Arc2D.Double(cx-radius,cy-radius,2*radius,2*radius,0,360,Arc2D.OPEN);
        g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(35,52,69));g.draw(track);
        double p=Math.max(0,Math.min(1,fraction));
        Arc2D arc=new Arc2D.Double(cx-radius,cy-radius,2*radius,2*radius,90,-360*p,Arc2D.OPEN);
        glow(g,arc,GREEN,5,animate?(float)(1.1+.2*Math.sin(phase)):1);
        g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setPaint(new GradientPaint(cx,cy-radius,new Color(138,255,199),cx,cy+radius,GREEN));g.draw(arc);
        if(p>0){double a=Math.toRadians(90-360*p);int dx=(int)(cx+radius*Math.cos(a)),dy=(int)(cy-radius*Math.sin(a));g.setColor(new Color(135,255,212,50));g.fillOval(dx-8,dy-8,16,16);g.setColor(new Color(189,255,228));g.fillOval(dx-3,dy-3,6,6);}
        center(g,"Level",cx,cy-31,font(11,false),MUTED);
        center(g,Integer.toString(target),cx,cy-16,font(radius>47?30:26,true),FG);
        center(g,String.format(Locale.US,"%.1f%%",p*100),cx,cy+18,font(12,true),GREEN);
    }
    public static void center(Graphics2D g,String s,int cx,int y,Font f,Color c){g.setFont(f);line(g,s,cx-g.getFontMetrics().stringWidth(s)/2f,y,f,c);}
    public static void button(Graphics2D g,Rectangle r,String label,boolean gold,boolean hover,double phase,boolean animate) {
        Shape shape=new RoundRectangle2D.Double(r.x+3,r.y+3,r.width-6,r.height-6,11,11);
        Color accent=gold?GOLD:CYAN;
        if(gold||hover)glow(g,shape,accent,4,hover?2:1);
        g.setPaint(new GradientPaint(r.x,r.y,gold?new Color(56,44,19):new Color(20,39,55),r.x,r.y+r.height,new Color(7,16,25)));
        g.fill(shape);g.setStroke(new BasicStroke(1.3f));g.setColor(accent);g.draw(shape);
        if(gold&&animate){Shape old=g.getClip();g.clip(shape);float x=(float)(r.x+((phase*.1)%1)*(r.width+100)-100);g.setPaint(new LinearGradientPaint(x,r.y,x+85,r.y,new float[]{0,.5f,1},new Color[]{new Color(255,240,185,0),new Color(255,240,185,35),new Color(255,240,185,0)}));g.fillRect((int)x,r.y,85,r.height);g.setClip(old);}
        int textY=r.y+(r.height-g.getFontMetrics(font(13,true)).getHeight())/2;
        if(gold&&label.equals("Navigate to monster")){
            g.setFont(font(13,true));int tw=g.getFontMetrics().stringWidth(label);int left=r.x+(r.width-tw-21)/2;
            g.setColor(GOLD);g.fillPolygon(new int[]{left,left+14,left+6},new int[]{r.y+r.height/2-3,r.y+r.height/2-8,r.y+r.height/2+8},3);
            line(g,label,left+21,textY,font(13,true),accent);
        }else center(g,label,r.x+r.width/2,textY,font(13,true),accent);
    }

    /** V4 layered card lighting. Draw into a disposable graphics copy so alpha never leaks. */
    public static void premiumCard(Graphics2D original,Rectangle r,Color edge,double hover) {
        Graphics2D g=(Graphics2D)original.create();quality(g);
        Shape shape=new RoundRectangle2D.Double(r.x+.5,r.y+.5,r.width-1,r.height-1,14,14);
        g.setPaint(new GradientPaint(r.x,r.y,new Color(24,42,58),r.x+r.width,r.y+r.height,new Color(7,17,27)));g.fill(shape);
        Shape old=g.getClip();g.clip(shape);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(r.x+r.width*.86f,r.y-12),Math.max(40,r.width*.9f),
            new float[]{0,1},new Color[]{alpha(edge,(int)(12+18*hover)),alpha(edge,0)}));g.fill(r);g.setClip(old);
        g.setStroke(new BasicStroke(1));g.setColor(alpha(edge,(int)(90+100*hover)));g.draw(shape);
        g.setColor(new Color(231,251,255,20));g.drawLine(r.x+10,r.y+1,r.x+r.width-10,r.y+1);
        if(hover>.01)glow(g,shape,edge,3,(float)(hover*.7));g.dispose();
    }
    public static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.max(0,Math.min(255,a)));}
    public static void forecastV4(Graphics2D original,Rectangle r,double seconds,CoachPreferences pref,double near,double hover) {
        Graphics2D g=(Graphics2D)original.create();quality(g);
        Shape shape=new RoundRectangle2D.Double(r.x+.5,r.y+.5,r.width-1,r.height-1,14,14);g.clip(shape);
        double phase=pref.animate()?seconds:0;
        BufferedImage art=image("forecast");
        if(art!=null){int drift=pref.animate()?(int)(3*Math.sin(phase*.12)):0;g.drawImage(art,r.x-5+drift,r.y-3,r.width+10,r.height+6,null);}
        g.setPaint(new GradientPaint(r.x,r.y,new Color(5,20,31,238),r.x+r.width,r.y,new Color(8,21,33,70)));g.fill(r);
        g.setPaint(new GradientPaint(r.x,r.y+r.height/2,new Color(7,15,24,15),r.x,r.y+r.height,new Color(4,13,21,240)));g.fill(r);
        Color light=pref.theme.secondary;
        // Broad, slow-moving aurora ribbons. Never flash or obscure the text layer.
        for(int band=0;band<(pref.effects==2?3:2);band++){
            Path2D ribbon=new Path2D.Double();
            for(int i=0;i<=32;i++){
                double px=r.x+i*r.width/32.0;
                double py=r.y+12+band*15+Math.sin(i*.14+phase*.24+band)*10;
                if(i==0)ribbon.moveTo(px,py);else ribbon.lineTo(px,py);
            }
            g.setStroke(new BasicStroke(14+band*8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(alpha(light,pref.effects==2?17:7));g.draw(ribbon);
        }
        g.setStroke(new BasicStroke(1));
        int stars=pref.effects==2?24:10;
        for(int i=0;i<stars;i++){
            double pulse=.5+.5*Math.sin(phase*.55+i*2.13);
            int sx=r.x+8+(i*67)%(Math.max(1,r.width-16)),sy=r.y+8+(i*31)%(Math.max(1,r.height-45));
            g.setColor(alpha(i%4==0?pref.theme.gold:light,(int)(18+45*pulse)));
            g.fillOval(sx,sy,i%6==0?2:1,i%6==0?2:1);
            if(i%8==0&&pref.effects>0){g.drawLine(sx-2,sy,sx+2,sy);g.drawLine(sx,sy-2,sx,sy+2);}
        }
        if(pref.animate()&&pref.effects==2){
            double meteor=(phase*.09)%1;
            if(meteor<.18){
                float mx=(float)(r.x+r.width*(.68+meteor)),my=(float)(r.y+19+meteor*125);
                g.setPaint(new GradientPaint(mx-16,my-7,alpha(light,0),mx,my,alpha(light,(int)(120*Math.sin(meteor/.18*Math.PI)))));
                g.setStroke(new BasicStroke(1.2f));g.draw(new Line2D.Float(mx-16,my-7,mx,my));
            }
        }
        // Horizon fog moves within the artwork only.
        if(pref.effects>0){
            float glowX=(float)(r.x+r.width*(.75+.05*Math.sin(phase*.18)));
            g.setPaint(new RadialGradientPaint(new Point2D.Float(glowX,r.y+r.height*.73f),Math.max(20,r.width*.48f),
                new float[]{0,1},new Color[]{alpha(pref.theme.gold,(int)(22+near*15)),alpha(pref.theme.gold,0)}));g.fill(r);
        }
        g.dispose();g=(Graphics2D)original.create();quality(g);
        g.setStroke(new BasicStroke(1));g.setColor(alpha(pref.theme.gold,(int)(130+hover*65)));g.draw(shape);
        glow(g,shape,pref.theme.gold,3,(float)(.4+near*.45+hover*.45));
        if(pref.animate()){
            double t=(phase*.045)%1;float px=(float)(r.x+t*r.width);
            g.setColor(alpha(pref.theme.gold,90));g.setStroke(new BasicStroke(2));g.draw(new Line2D.Double(Math.max(r.x+8,px-8),r.y+r.height-1,Math.min(r.x+r.width-8,px+8),r.y+r.height-1));
        }
        g.dispose();
    }
    public static void ringV4(Graphics2D original,int cx,int cy,int radius,double fraction,int target,double phase,CoachPreferences pref,double pulse) {
        Graphics2D g=(Graphics2D)original.create();quality(g);Color colour=pref.theme.primary;
        double p=Math.max(0,Math.min(1,fraction));
        g.setStroke(new BasicStroke(1));
        for(int i=0;i<48;i++){
            double a=Math.toRadians(i*7.5-90);int inner=radius+10,outer=inner+(i%4==0?4:2);
            g.setColor(alpha(colour,i/48.0<=p?65:17));
            g.draw(new Line2D.Double(cx+inner*Math.cos(a),cy+inner*Math.sin(a),cx+outer*Math.cos(a),cy+outer*Math.sin(a)));
        }
        Arc2D track=new Arc2D.Double(cx-radius,cy-radius,2*radius,2*radius,0,360,Arc2D.OPEN);
        g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(32,52,66));g.draw(track);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx,cy),Math.max(12,radius),new float[]{0,1},new Color[]{alpha(colour,12),alpha(colour,0)}));g.fillOval(cx-radius,cy-radius,radius*2,radius*2);
        Arc2D arc=new Arc2D.Double(cx-radius,cy-radius,radius*2,radius*2,90,-360*p,Arc2D.OPEN);
        glow(g,arc,colour,pref.effects==2?6:3,(float)(.75+pulse*.8));
        g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setPaint(new GradientPaint(cx,cy-radius,colour.brighter(),cx,cy+radius,colour));g.draw(arc);
        if(p>0){double a=Math.toRadians(90-360*p);int dx=(int)(cx+radius*Math.cos(a)),dy=(int)(cy-radius*Math.sin(a));
            g.setColor(alpha(colour,(int)(50+pulse*35)));g.fillOval(dx-7,dy-7,14,14);g.setColor(new Color(216,255,239));g.fillOval(dx-3,dy-3,6,6);}
        center(g,"Level",cx,cy-29,font(10,false),MUTED);center(g,Integer.toString(target),cx,cy-16,font(radius>44?31:26,true),FG);
        center(g,String.format(Locale.US,"%.1f%%",p*100),cx,cy+18,font(11,true),colour);
        g.dispose();
    }
    public static void miniClock(Graphics2D original,int x,int y,int size,Color c,double phase,boolean animate){
        Graphics2D g=(Graphics2D)original.create();quality(g);g.setColor(c);g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x,y,size,size);double angle=-Math.PI/2+(animate?phase*.12:0);int cx=x+size/2,cy=y+size/2;
        g.drawLine(cx,cy,cx+(int)(Math.cos(angle)*size*.31),cy+(int)(Math.sin(angle)*size*.31));g.drawLine(cx,cy,cx+size/5,cy+size/8);g.dispose();
    }
}
