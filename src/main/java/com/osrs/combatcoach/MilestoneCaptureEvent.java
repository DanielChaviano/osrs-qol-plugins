package com.osrs.combatcoach;

import net.runelite.client.util.Filepath;

/** Completed or failed capture; success never means merely requested. */
public final class MilestoneCaptureEvent {
    public final String id,skill,message;
    public final int level;
    public final Filepath file,cardFile;
    public final boolean test,success,includesCelebration;
    public final long createdAt;
    public MilestoneCaptureEvent(String skill,int level,Filepath file,boolean test,boolean success,String message){
        this("",skill,level,file,null,test,success,false,System.currentTimeMillis(),message);
    }
    public MilestoneCaptureEvent(String id,String skill,int level,Filepath file,Filepath cardFile,boolean test,boolean success,boolean includesCelebration,long createdAt,String message){
        this.id=id;this.skill=skill;this.level=level;this.file=file;this.cardFile=cardFile;this.test=test;this.success=success;
        this.includesCelebration=includesCelebration;this.createdAt=createdAt;this.message=message==null?"":message;
    }
    public CoachCaptureRecord record(){return new CoachCaptureRecord(id,skill,level,test,createdAt,file==null?"":file.toString(),cardFile==null?"":cardFile.toString(),includesCelebration);}
}
