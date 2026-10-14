package com.osrs.combatcoach;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;

/** Calls the production overlay painters. No RuneScape client is opened or driven. */
public final class CoachOverlayVerification {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    public static void verify(File output)throws Exception{
        CoachUiState state=new CoachUiState(true,"Strength",20,4470,40,5800,new int[]{20,20,16,20,1,1,3},"Lumbridge cows","","Free to play","","",true,true,true,true,"","");
        CombatCoachOverlay hud=new CombatCoachOverlay(null,()->state);
        BufferedImage h=new BufferedImage(360,260,BufferedImage.TYPE_INT_ARGB);Graphics2D gh=h.createGraphics();Dimension hd=hud.render(gh);gh.dispose();
        check(hd!=null&&hd.width<=360&&hd.height<=260,"HUD layout bounds");
        ImageIO.write(h.getSubimage(0,0,hd.width,hd.height),"PNG",new File(output,"actual-hud-render.png"));
        CoachPreferences style=CoachPreferences.defaults();AtomicLong clock=new AtomicLong(1_000_000_000L);
        MilestoneCaptureSequence gate=new MilestoneCaptureSequence(true);gate.present(1000);int[] ready={0};
        CoachToastOverlay toast=new CoachToastOverlay(null,()->style,()->true,()->new Dimension(765,503),id->{if(gate.painted(clock.get()/1_000_000,true))ready[0]++;},(action,path)->{},clock::get);
        CoachCaptureRecord record=new CoachCaptureRecord("test-id","Defence",20,true,1791812400000L,"","",true);
        toast.prepare(record,true);
        BufferedImage canvas=new BufferedImage(360,530,BufferedImage.TYPE_INT_ARGB);
        clock.set(1_200_000_000L);Graphics2D early=canvas.createGraphics();toast.render(early);early.dispose();check(ready[0]==0,"No early-frame capture");
        clock.set(2_000_000_000L);Graphics2D first=canvas.createGraphics();toast.render(first);first.dispose();check(ready[0]==0,"First fully shown frame is not enough");
        clock.set(2_033_000_000L);Graphics2D second=canvas.createGraphics();Dimension size=toast.render(second);second.dispose();check(ready[0]==1,"Two stable frames release capture");
        check(gate.request(2033),"Frame-gated request allowed once");check(!gate.request(2034),"No duplicate screenshot request");toast.capturing();
        Graphics2D capture=canvas.createGraphics();capture.setBackground(new java.awt.Color(0,true));capture.clearRect(0,0,360,530);size=toast.render(capture);capture.dispose();
        check(size!=null&&size.width<=360&&size.height<=530,"Capture card fits canvas");
        ImageIO.write(canvas.getSubimage(0,0,size.width,size.height),"PNG",new File(output,"card-visible-before-capture.png"));
        File fixture=new File(output,"test-capture.png");ImageIO.write(canvas,"PNG",fixture);check(fixture.isFile()&&fixture.length()>0,"Fixture file exists");
        record=new CoachCaptureRecord("test-id","Defence",20,true,1791812400000L,fixture.getAbsolutePath(),"test-card.png",true);
        toast.saved(record,"");clock.addAndGet(400_000_000L);Graphics2D saved=canvas.createGraphics();size=toast.render(saved);saved.dispose();
        check(size!=null&&size.width<=360&&size.height<=530,"Saved confirmation fits canvas");
        ImageIO.write(canvas.getSubimage(0,0,size.width,size.height),"PNG",new File(output,"actual-toast-render.png"));
        BufferedImage card=CoachMilestoneArt.keepsake(record,style);ImageIO.write(card,"PNG",new File(output,"actual-keepsake-card.png"));
        check(card.getWidth()==700&&card.getHeight()==650,"High-resolution clean-card dimensions");
        toast.dismiss();Graphics2D empty=canvas.createGraphics();check(toast.render(empty)==null,"Dismiss actually hides popup");empty.dispose();
        // A raw screenshot choice cannot accidentally display a pre-capture card.
        toast.prepare(record,false);Graphics2D raw=canvas.createGraphics();check(toast.render(raw)==null,"Card inclusion OFF hides pre-capture card");raw.dispose();
        toast.dismiss();
        System.out.println("OVERLAY RENDER CHECKS PASSED: "+checks+" (actual painters, capture gate, saved-state and dismissal)");
    }
    public static void main(String[] args)throws Exception{File dir=new File(args.length==0?"build/premium-ui-checks":args[0]);dir.mkdirs();verify(dir);}
}
