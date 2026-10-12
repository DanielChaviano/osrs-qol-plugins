package com.osrs.combatcoach;

import java.util.ArrayDeque;
import java.util.Deque;
import net.runelite.api.Skill;

/** A rolling rate that includes time between attacks, missed hits, and idle periods. */
final class CombatXpTracker {
    private static final long WINDOW_MS=180_000L;
    private final Deque<Gain> gains=new ArrayDeque<>();
    private Skill skill;
    private long start=-1;
    private long lastCombat=-1;

    void reset() { gains.clear();skill=null;start=-1;lastCombat=-1; }

    void observeCombat(Skill focus,long now) {
        select(focus);
        if (start<0) start=now;
        lastCombat=now;
    }

    void addXp(Skill focus,int delta,long now) {
        if (delta<=0) return;
        select(focus);
        if (start<0) start=now;
        gains.addLast(new Gain(now,delta));
        trim(now);
    }

    void select(Skill focus) {
        if (skill!=focus) { reset();skill=focus; }
    }

    long rate(long now) {
        trim(now);
        if (start<0 || gains.isEmpty()) return -1;
        long from=Math.max(start,now-WINDOW_MS);
        long elapsed=Math.max(15_000L,now-from);
        long total=0;
        for (Gain g:gains) if (g.time>=from) total+=g.amount;
        return Math.round(total*3_600_000.0/elapsed);
    }

    private void trim(long now) {
        while (!gains.isEmpty() && gains.peekFirst().time<now-WINDOW_MS) gains.removeFirst();
    }

    private static final class Gain {
        final long time; final int amount;
        Gain(long time,int amount) {this.time=time;this.amount=amount;}
    }
}
