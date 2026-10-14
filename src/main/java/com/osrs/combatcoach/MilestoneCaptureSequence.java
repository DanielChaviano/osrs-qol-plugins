package com.osrs.combatcoach;

/** Deterministic capture gate: no screenshot request until the celebration has been painted. */
public final class MilestoneCaptureSequence {
    public enum Stage { QUEUED, PRESENTING, READY, REQUESTED, GAME_SAVED, CARD_REQUESTED, COMPLETE, FAILED }
    private Stage stage=Stage.QUEUED;
    private long began, changed;
    private int stableFrames;
    private final boolean includeCard;
    public MilestoneCaptureSequence(boolean includeCard){this.includeCard=includeCard;}
    public Stage stage(){return stage;}
    public void present(long now){if(stage!=Stage.QUEUED)throw new IllegalStateException("Capture already started");began=changed=now;stage=Stage.PRESENTING;}
    public boolean painted(long now,boolean opaque){
        if(stage!=Stage.PRESENTING||!opaque||now-began<900)return false;
        if(++stableFrames>=2){stage=Stage.READY;changed=now;return true;}return false;
    }
    public boolean request(long now){
        if(stage==Stage.READY||(!includeCard&&stage==Stage.PRESENTING)){stage=Stage.REQUESTED;changed=now;return true;}return false;
    }
    public void gameSaved(long now){if(stage!=Stage.REQUESTED)throw new IllegalStateException("Unexpected game-image completion");stage=Stage.GAME_SAVED;changed=now;}
    public boolean requestCard(long now){if(stage!=Stage.GAME_SAVED)return false;stage=Stage.CARD_REQUESTED;changed=now;return true;}
    public void complete(long now){if(stage!=Stage.GAME_SAVED&&stage!=Stage.CARD_REQUESTED)throw new IllegalStateException("No saved game image");stage=Stage.COMPLETE;changed=now;}
    public void fail(long now){if(stage!=Stage.COMPLETE){stage=Stage.FAILED;changed=now;}}
    public boolean timedOut(long now){
        if(stage==Stage.COMPLETE||stage==Stage.FAILED||stage==Stage.QUEUED)return false;
        return now-changed>(stage==Stage.PRESENTING||stage==Stage.READY?12000:20000);
    }
    public boolean freezePresentation(){return stage==Stage.READY||stage==Stage.REQUESTED;}
}
