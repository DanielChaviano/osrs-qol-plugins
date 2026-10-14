package com.osrs.combatcoach;

/** Small immutable gallery entry. Paths are obtained from successful ImageCapture events. */
public final class CoachCaptureRecord {
    public final String id, skill, file, cardFile;
    public final int level;
    public final boolean test, includesCelebration;
    public final long createdAt;
    public CoachCaptureRecord(String id,String skill,int level,boolean test,long createdAt,String file,String cardFile,boolean includesCelebration) {
        this.id=clean(id);this.skill=clean(skill);this.level=level;this.test=test;this.createdAt=createdAt;
        this.file=clean(file);this.cardFile=clean(cardFile);this.includesCelebration=includesCelebration;
    }
    private static String clean(String s){return s==null?"":s;}
    public CoachCaptureRecord withCard(String path) { return new CoachCaptureRecord(id,skill,level,test,createdAt,file,path,includesCelebration); }
}
