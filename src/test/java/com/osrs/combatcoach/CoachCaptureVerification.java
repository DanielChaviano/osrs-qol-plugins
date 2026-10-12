package com.osrs.combatcoach;

/** State-machine regression tests: timing, order, duplicates, failures and raw-capture opt-out. */
public final class CoachCaptureVerification {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    public static void main(String[] args){
        MilestoneCaptureSequence c=new MilestoneCaptureSequence(true);
        check(!c.request(1),"No capture while queued");c.present(100);
        check(!c.request(101),"Cannot capture before rendered card");
        check(!c.painted(400,true),"No partially revealed image");
        check(!c.painted(1100,false),"Transparent card does not count");
        check(!c.painted(1101,true),"First stable frame");
        check(c.painted(1140,true),"Second stable frame");
        check(c.freezePresentation(),"Freeze the fully rendered hero for capture");
        check(c.request(1141),"One next-frame request");check(!c.request(1142),"Never double request");
        check(!c.timedOut(2141),"Normal save does not time out");c.gameSaved(2300);
        check(c.requestCard(2310),"Companion only after game file success");check(!c.requestCard(2320),"Never duplicate companion");
        c.complete(2500);check(c.stage()==MilestoneCaptureSequence.Stage.COMPLETE,"Completion after files");check(!c.timedOut(999999),"Finished jobs stay finished");
        MilestoneCaptureSequence stalled=new MilestoneCaptureSequence(true);stalled.present(0);
        check(stalled.timedOut(12001),"No renderer means no silent incomplete screenshot");stalled.fail(12002);check(!stalled.request(12003),"Failed requests cannot capture later");
        MilestoneCaptureSequence raw=new MilestoneCaptureSequence(false);raw.present(0);check(raw.request(1),"Explicit raw opt-out can capture immediately");raw.gameSaved(2);raw.complete(3);check(raw.stage()==MilestoneCaptureSequence.Stage.COMPLETE,"Raw flow completes");
        MilestoneCaptureSequence saveFailure=new MilestoneCaptureSequence(false);saveFailure.present(0);saveFailure.request(1);check(saveFailure.timedOut(20002),"Save timeout is bounded");saveFailure.fail(20003);check(saveFailure.stage()==MilestoneCaptureSequence.Stage.FAILED,"Failure never becomes saved");
        boolean protectedOrder=false;try{new MilestoneCaptureSequence(true).complete(0);}catch(IllegalStateException expected){protectedOrder=true;}check(protectedOrder,"Cannot show success with no saved file");
        CoachSession session=new CoachSession();session.select("STRENGTH");session.gain(12,1000);session.gain(4,2000);check(session.gained()==16,"Session records actual earned XP");check(session.idle(12000)==10000,"Idle slowdown context");
        session.select("DEFENCE");check(session.gained()==0,"No cross-skill mixing");
        System.out.println("CAPTURE AND SESSION CHECKS PASSED: "+checks);
    }
}
