package com.osrs.combatcoach;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** V4 permutations through the real painter, including narrow layouts and persisted-history fixtures. */
public final class CoachV4Verification {
    private static int checks;
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);checks++;}
    public static CoachUiState demo(CoachPreferences pref){
        List<CoachCaptureRecord> history=Arrays.asList(new CoachCaptureRecord("fixture","Defence",20,true,1791821400000L,"C:\\Users\\example\\.runelite\\screenshots\\player\\Milestones\\test-defence-level-20-12345678.png","C:\\Users\\example\\.runelite\\screenshots\\player\\Milestones\\test-defence-level-20-12345678-card.png",true));
        return new CoachUiState(true,"Defence",17,3276,20,5620,new int[]{20,20,17,21,1,1,3},"Lumbridge cows","Low-defence training with your current setup.","Free to play","Cow (5,600 XP/hr)","Highest modelled: Cow\nChicken: ~4,800 XP/hr\nCow: ~5,700 XP/hr\nGiant frog: ~5,200 XP/hr",true,true,pref.reducedMotion,true,history.get(0).file,"",pref,history,688,400000,1200,.08,"At the training area");
    }
    public static void main(String[] args)throws Exception{
        File out=new File(args.length==0?"build/premium-ui-checks":args[0]);out.mkdirs();
        CoachPreferences defaults=CoachPreferences.defaults();check(defaults.includeCelebration&&defaults.saveCard,"New installs default to both game-with-card and companion card");
        check(defaults.effects==2&&defaults.animate(),"Cinematic effects are available by default");
        check(!new CoachPreferences(CoachPreferences.Theme.GOLD,2,true,false,false,true,true).animate(),"Reduced motion takes precedence");
        check(!new CoachPreferences(CoachPreferences.Theme.EMERALD,0,false,false,false,true,true).animate(),"Static effects really stay static");
        CoachUiState demo=demo(defaults);check(demo.remaining()==1194,"Accurate remaining XP");check(demo.secondsRemaining()==765,"Exact same rate-to-ETA arithmetic");
        check(demo.capturesList.size()==1,"History snapshot retained");boolean immutable=false;try{demo.capturesList.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"History snapshot immutable");
        Instant now=Instant.parse("2026-10-12T19:43:00Z");check(demo.relativeDay(now,ZoneId.of("America/New_York")).equals("TODAY"),"Today is an annotation, not a missing date");
        check(demo.finishDate(now,ZoneId.of("America/New_York")).equals("Oct 12, 2026"),"Full date retained");
        SwingUtilities.invokeAndWait(()->{
            CoachDashboard board=new CoachDashboard((a,v)->{});
            try{
                for(CoachPreferences.Theme theme:CoachPreferences.Theme.values())for(boolean compact:new boolean[]{false,true})for(int width:new int[]{185,209,225,242,320,376})for(int tab=0;tab<3;tab++){
                    CoachPreferences pref=new CoachPreferences(theme,2,true,compact,compact,true,true);CoachUiState state=demo(pref);
                    BufferedImage im=board.renderAtTimeForTest(width,state,tab,2.2);board.assertWidthSafe();check(im.getWidth()==width,"V4 requested width");
                    if(width==225&&!compact){try{ImageIO.write(im,"PNG",new File(out,"v4-"+theme.name().toLowerCase()+"-tab-"+tab+".png"));}catch(Exception e){throw new RuntimeException(e);}}
                }
                CoachPreferences moving=new CoachPreferences(CoachPreferences.Theme.EMERALD,2,false,false,false,true,true);
                BufferedImage a=board.renderAtTimeForTest(225,demo(moving),0,1),b=board.renderAtTimeForTest(225,demo(moving),0,3.8);
                check(difference(a,b)>200,"Cinematic frames actually change, not a static claim");
                CoachPreferences still=new CoachPreferences(CoachPreferences.Theme.EMERALD,2,true,false,false,true,true);
                BufferedImage c=board.renderAtTimeForTest(225,demo(still),0,1),d=board.renderAtTimeForTest(225,demo(still),0,3.8);
                check(difference(c,d)<100,"Reduced-motion frames stay static except current clock text");
                for(int width:new int[]{185,209,225}){
                    CoachUiState longGoal=new CoachUiState(true,"Strength",20,4470,99,1,new int[]{20,20,20,20,1,1,3},"An exceptionally long target name used for the responsive layout test","","Free to play","","",true,true,true,true,"","");
                    board.renderForTest(width,longGoal,0);board.assertWidthSafe();check(longGoal.secondsRemaining()>365L*86400,"Long forecast beyond one year stays valid");
                }
                BufferedImage keep=CoachMilestoneArt.keepsake(demo.capturesList.get(0),defaults);
                board.setThumbnail("fixture",keep);BufferedImage gallery=board.renderForTest(225,demo,2);
                try{ImageIO.write(gallery,"PNG",new File(out,"v4-gallery.png"));}catch(Exception e){throw new RuntimeException(e);}
                check(gallery.getHeight()>500,"Gallery content scrolls vertically, not clipped");
            }finally{board.dispose();}
        });
        System.out.println("V4 THEME, MOTION, GALLERY AND LAYOUT CHECKS PASSED: "+checks);
    }
    private static int difference(BufferedImage a,BufferedImage b){int changed=0;for(int y=0;y<Math.min(a.getHeight(),b.getHeight());y++)for(int x=0;x<a.getWidth();x++)if(a.getRGB(x,y)!=b.getRGB(x,y))changed++;return changed;}
}
