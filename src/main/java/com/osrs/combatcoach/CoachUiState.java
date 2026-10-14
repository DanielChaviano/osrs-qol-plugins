package com.osrs.combatcoach;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/** Immutable data crossing the client, Swing and overlay render threads. */
public final class CoachUiState {
    public static final String VERSION = "4.0.1-rc.1";
    public final boolean loggedIn, hud, popups, reducedMotion, captures;
    public final String skill, spot, reason, requirements, measured, predicted, lastCapture, notice;
    public final int level, xp, goal;
    private final int[] levels;
    public final CoachPreferences preferences;
    public final java.util.List<CoachCaptureRecord> capturesList;
    public final long sessionXp, sessionMillis, idleMillis;
    public final double paceTrend;
    public final String routeStatus;
    public final long rate; // Internal only: deliberately never painted as XP/hour.

    public CoachUiState(boolean loggedIn, String skill, int level, int xp, int goal, long rate,
                        int[] levels, String spot, String reason, String requirements,
                        String measured, String predicted, boolean hud, boolean popups,
                        boolean reducedMotion, boolean captures, String lastCapture, String notice) {
        this(loggedIn,skill,level,xp,goal,rate,levels,spot,reason,requirements,measured,predicted,hud,popups,
            reducedMotion,captures,lastCapture,notice,
            new CoachPreferences(CoachPreferences.Theme.EMERALD,2,reducedMotion,false,false,true,true),
            java.util.Collections.emptyList(),0,0,-1,0,"");
    }
    public CoachUiState(boolean loggedIn, String skill, int level, int xp, int goal, long rate,
                        int[] levels, String spot, String reason, String requirements,
                        String measured, String predicted, boolean hud, boolean popups,
                        boolean reducedMotion, boolean captures, String lastCapture, String notice,
                        CoachPreferences preferences, java.util.List<CoachCaptureRecord> records,
                        long sessionXp,long sessionMillis,long idleMillis,double paceTrend,String routeStatus) {
        this.preferences=preferences;this.capturesList=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(records));
        this.sessionXp=sessionXp;this.sessionMillis=sessionMillis;this.idleMillis=idleMillis;
        this.paceTrend=paceTrend;this.routeStatus=safe(routeStatus);
        this.loggedIn = loggedIn; this.skill = safe(skill); this.level = level;
        this.xp = Math.max(0, xp); this.goal = Math.max(2, Math.min(99, goal)); this.rate = rate;
        this.levels = Arrays.copyOf(levels, 7); this.spot = safe(spot); this.reason = safe(reason);
        this.requirements = safe(requirements); this.measured = safe(measured); this.predicted = safe(predicted);
        this.hud = hud; this.popups = popups; this.reducedMotion = reducedMotion; this.captures = captures;
        this.lastCapture = safe(lastCapture); this.notice = safe(notice);
    }
    private static String safe(String s) { return s == null ? "" : s; }
    public int skillLevel(int index) { return levels[index]; }
    public long remaining() { return Math.max(0L, (long) xpForLevel(goal) - xp); }
    public boolean complete() { return loggedIn && remaining() == 0; }
    /** Progress never resets to zero when an intermediate level is reached. */
    public double progress() { return loggedIn ? Math.min(1.0, xp / (double) xpForLevel(goal)) : 0; }
    public long secondsRemaining() {
        return complete() ? 0 : (!loggedIn || rate <= 0 ? -1 : (long) Math.ceil(remaining() * 3600.0 / rate));
    }
    public ZonedDateTime finish(Instant now, ZoneId zone) {
        return secondsRemaining() < 0 ? null : now.atZone(zone).plusSeconds(secondsRemaining());
    }
    public String finishDate(Instant now, ZoneId zone) {
        ZonedDateTime t = finish(now, zone);
        return t == null ? "Waiting for training data" : t.format(DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US));
    }
    public String finishTime(Instant now, ZoneId zone) {
        ZonedDateTime t = finish(now, zone);
        return t == null ? "Start fighting to estimate" : t.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US));
    }
    public String duration() {
        long s = secondsRemaining();
        if (s < 0) return "Learning your current pace";
        if (s == 0) return "Goal reached";
        if (s < 60) return "Less than a minute remaining";
        long m = (s + 59) / 60, days = m / 1440, hours = (m % 1440) / 60, minutes = m % 60;
        return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + minutes + "m remaining";
    }
    public static int xpForLevel(int level) {
        int points = 0;
        for (int i = 1; i < Math.max(1, Math.min(level, 99)); i++)
            points += (int) Math.floor(i + 300 * Math.pow(2, i / 7.0));
        return points / 4;
    }
    public String relativeDay(Instant now, ZoneId zone) {
        ZonedDateTime finish=finish(now,zone); if(finish==null)return "";
        long days=java.time.temporal.ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(),finish.toLocalDate());
        return days==0?"TODAY":days==1?"TOMORROW":"IN "+days+" DAYS";
    }
    public String paceLabel() {
        if(complete())return "GOAL COMPLETE";
        if(rate<=0)return "LEARNING YOUR PACE";
        if(idleMillis>30000)return "PACE SLOWING";
        if(sessionMillis>0&&sessionMillis<45000)return "EARLY ESTIMATE";
        return paceTrend>.12?"PACE IMPROVING":"PACE STEADY";
    }
    public static CoachUiState waiting() {
        return new CoachUiState(false, "Strength", 1, 0, 20, -1, new int[7], "Log in to begin", "", "",
            "", "", false, true, false, true, "", "");
    }
}
