package com.osrs.combatcoach;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Samples actual combat XP by NPC name with an elapsed, idle-inclusive clock.
 * A sample measures a local session, not a mathematically verified global optimum.
 */
final class CombatSpotTracker {
    private final Map<String, Sample> samples=new HashMap<>();
    private String active="";
    private long lastFighting=-1L;

    void reset() { samples.clear();active="";lastFighting=-1L; }
    void fighting(String name,long now) {
        if (name==null || name.isEmpty()) return;
        String key=name.toLowerCase(Locale.ROOT);
        if (!key.equals(active)) {
            active=key;
            Sample s=samples.computeIfAbsent(key,k->new Sample(name));
            if (s.started==0) s.started=now;
        }
        lastFighting=now;
    }
    void gain(int xp,long now) {
        if (xp<=0 || active.isEmpty() || lastFighting<0 || now-lastFighting>8000) return;
        Sample s=samples.get(active);
        if (s==null) return;
        if (s.started==0) s.started=now;
        s.xp+=xp;
        s.last=now;
    }
    /** Sample time includes non-attacking time until the last recorded XP gain. */
    String summary() {
        StringBuilder b=new StringBuilder();
        int count=0;
        for (Sample s:samples.values()) {
            long elapsed=s.last-s.started;
            if (elapsed<60_000 || s.xp<100) continue;
            long rate=Math.round(s.xp*3_600_000.0/elapsed);
            if (count++>0) b.append("; ");
            b.append(s.name).append(" ").append(String.format(Locale.US,"%,d",rate)).append(" XP/hr");
        }
        return b.length()==0?"Fight a monster for 1+ minute and earn 100+ XP to record a comparison.":b.toString();
    }
    String best() {
        Sample best=null;
        double rate=0;
        for(Sample s:samples.values()) {
            long elapsed=s.last-s.started;
            if (elapsed<60_000 || s.xp<100) continue;
            double r=s.xp*3_600_000.0/elapsed;
            if(r>rate) {rate=r;best=s;}
        }
        return best==null?"No measured winner yet":best.name+" ("+String.format(Locale.US,"%,d",Math.round(rate))+" XP/hr)";
    }
    private static final class Sample {
        final String name;
        long started, last;
        int xp;
        Sample(String name){this.name=name;}
    }
}
