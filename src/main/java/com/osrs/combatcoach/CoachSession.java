package com.osrs.combatcoach;

/** Per-skill session context. Hourly numbers are deliberately not part of the UI. */
public final class CoachSession {
    private String skill="";
    private long start, lastGain, gained;
    private double baseline;
    public void select(String next){if(!skill.equals(next)){reset();skill=next;}}
    public void reset(){start=0;lastGain=0;gained=0;baseline=0;}
    public void gain(int xp,long now){if(xp<=0)return;if(start==0)start=now;lastGain=now;gained+=xp;}
    public long gained(){return gained;}
    public long elapsed(long now){return start==0?0:Math.max(0,now-start);}
    public long idle(long now){return lastGain==0?-1:Math.max(0,now-lastGain);}
    public double trend(long rate){if(rate<=0)return 0;if(baseline<=0)baseline=rate;double result=(rate-baseline)/Math.max(1,baseline);baseline+=.02*(rate-baseline);return Math.max(-1,Math.min(1,result));}
}
