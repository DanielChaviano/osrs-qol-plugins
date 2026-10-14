package com.osrs.combatcoach;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Executed by the installer against the staged build, using actual production UI code. */
public final class CoachUiVerification {
    private static int checks;
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private static CoachUiState sample(int xp,int goal,long rate,String spot){return new CoachUiState(true,"Defence",16,xp,goal,rate,new int[]{20,20,16,20,1,1,3},spot,
        "Low-defence training given your current damage and survivability.","Free to play","Cow (5,600 XP/hr)","Highest modelled: Cow\nCow: ~5,700 XP/hr",false,true,true,true,
        "C:\\Users\\example\\.runelite\\screenshots\\example-player\\Milestones\\defence-level-20-example.png","");}
    public static void main(String[] args)throws Exception {
        File output=new File(args.length==0?"build/premium-ui-checks":args[0]);if(!output.isDirectory()&&!output.mkdirs())throw new IllegalStateException("Cannot create test output directory");
        check(CoachUiState.xpForLevel(20)==4470,"Level 20 XP");check(CoachUiState.xpForLevel(99)==13034431,"Level 99 XP");
        CoachUiState normal=sample(2748,20,5800,"Lumbridge cows");
        check(normal.remaining()==1722,"Remaining XP");check(normal.secondsRemaining()==1069,"ETA uses unchanged remaining/rate arithmetic");
        CoachUiState reached=sample(4470,20,5800,"Lumbridge cows");check(reached.complete(),"Reached goal stays complete");check(reached.goal==20,"Goal does not advance without user choice");
        CoachUiState longGoal=sample(4470,99,5500,"Lumbridge cows");
        Instant now=Instant.parse("2026-10-12T17:00:00Z");ZoneId zone=ZoneId.of("America/New_York");
        check(longGoal.secondsRemaining()>60L*86400,"Level 99 is not shown as two days");
        check(longGoal.finishDate(now,zone).matches("[A-Za-z]+ \\d+, \\d{4}"),"Full calendar date including year");
        check(sample(2748,20,-1,"Cow").finish(now,zone)==null,"No fabricated ETA without a rate");
        check(CoachArt.assetsPresent(),"All ten packaged artwork assets load");
        SwingUtilities.invokeAndWait(()->{
            CoachDashboard dashboard=new CoachDashboard((a,v)->{});
            try {
                for(int width:new int[]{185,209,225,242,320,376})for(int tab=0;tab<3;tab++){
                    BufferedImage image=dashboard.renderForTest(width,normal,tab);dashboard.assertWidthSafe();
                    check(image.getWidth()==width,"Rendered requested width");
                    try{ImageIO.write(image,"PNG",new File(output,"ui-"+width+"-tab-"+tab+".png"));}catch(Exception e){throw new RuntimeException(e);}
                }
                for(CoachUiState state:new CoachUiState[]{CoachUiState.waiting(),reached,longGoal,sample(2748,99,1,"A deliberately long recommended monster name for the narrow sidebar")}){
                    dashboard.renderForTest(209,state,0);dashboard.assertWidthSafe();checks++;
                }
                BufferedImage scratch=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);Graphics2D g=scratch.createGraphics();
                for(int width:new int[]{65,100,165})for(String row:CoachArt.lines(g,normal.lastCapture,CoachArt.font(12,false),width))check(g.getFontMetrics(CoachArt.font(12,false)).stringWidth(row)<=width,"Long paths wrap without clipping");
                g.dispose();
            }finally{dashboard.dispose();}
        });
        System.out.println("PREMIUM UI CHECKS PASSED: "+checks);
        System.out.println("Rendered actual Java UI at 185, 209, 225, 242, 320 and 376 pixels.");
    }
}
