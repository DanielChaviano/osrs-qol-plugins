package com.osrs.combatcoach;

import java.awt.Color;

/** Immutable presentation preferences. They never change a combat calculation. */
public final class CoachPreferences {
    public enum Theme {
        EMERALD("Emerald", 0x51f4b6, 0x50d9ff, 0xffd579),
        GOLD("Sunlit gold", 0xffd579, 0xffe5ad, 0xffc75e),
        GLACIER("Glacier", 0x72caff, 0xc2a6ff, 0xd7ebff),
        CLASSIC("Old School", 0xdcc789, 0xc7dfa0, 0xf1cf76);
        public final String label;
        public final Color primary, secondary, gold;
        Theme(String label, int primary, int secondary, int gold) {
            this.label=label; this.primary=new Color(primary); this.secondary=new Color(secondary); this.gold=new Color(gold);
        }
        public static Theme parse(String value) { try { return valueOf(value); } catch (Exception ignored) { return EMERALD; } }
    }
    public final Theme theme;
    public final int effects;
    public final boolean reducedMotion, compactHud, compactLayout, includeCelebration, saveCard, includeFrame;
    public CoachPreferences(Theme theme,int effects,boolean reducedMotion,boolean compactHud,boolean compactLayout,boolean includeCelebration,boolean saveCard) {
        this(theme,effects,reducedMotion,compactHud,compactLayout,includeCelebration,saveCard,false);
    }
    public CoachPreferences(Theme theme,int effects,boolean reducedMotion,boolean compactHud,boolean compactLayout,boolean includeCelebration,boolean saveCard,boolean includeFrame) {
        this.includeFrame=includeFrame;
        this.theme=theme==null?Theme.EMERALD:theme;
        this.effects=Math.max(0,Math.min(2,effects)); this.reducedMotion=reducedMotion;
        this.compactHud=compactHud; this.compactLayout=compactLayout; this.includeCelebration=includeCelebration; this.saveCard=saveCard;
    }
    public boolean animate() { return !reducedMotion&&effects>0; }
    public static CoachPreferences defaults() { return new CoachPreferences(Theme.EMERALD,2,false,false,false,true,true); }
}
