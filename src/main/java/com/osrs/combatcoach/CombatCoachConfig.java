package com.osrs.combatcoach;

import net.runelite.client.config.*;

@ConfigGroup("combatcoach")
public interface CombatCoachConfig extends Config {
    enum Focus { AUTO, ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC }
    enum Priority { FAST_XP, LOW_ATTENTION }
    @ConfigItem(keyName="focus",name="Training skill",description="Follow XP gains or choose a guide focus",position=0)
    default Focus focus(){return Focus.AUTO;}
    @ConfigItem(keyName="priority",name="Training priority",description="Existing training planner preference",position=1)
    default Priority priority(){return Priority.FAST_XP;}
    @ConfigItem(keyName="members",name="Members account",description="Include members destinations. Turn off for F2P.",position=2)
    default boolean members(){return true;}
    @ConfigItem(keyName="boneVoyage",name="Bone Voyage completed",description="Allow Fossil Island suggestions",position=3)
    default boolean boneVoyage(){return false;}
    @ConfigItem(keyName="nightmareZone",name="Nightmare Zone unlocked",description="Five eligible completed quests required",position=4)
    default boolean nightmareZone(){return false;}
    @ConfigItem(keyName="naguas",name="Sulphur Naguas unlocked",description="Include Naguas in the existing planner",position=5)
    default boolean naguas(){return false;}
    @ConfigItem(keyName="worldMapPin",name="World map destination marker",description="Mark the recommended destination",position=6)
    default boolean worldMapPin(){return true;}
    @ConfigItem(keyName="premiumHud",name="In-game HUD",description="Small movable forecast. Alt-drag to reposition.",position=10)
    default boolean premiumHud(){return false;}
    @ConfigItem(keyName="premiumPopups",name="Milestone screenshot popup",description="Show the saved-image confirmation; capture cards are controlled separately",position=11)
    default boolean premiumPopups(){return true;}
    @ConfigItem(keyName="premiumReducedMotion",name="Reduced motion",description="Disable decorative animations",position=12)
    default boolean premiumReducedMotion(){return false;}
    @ConfigItem(keyName="premiumTheme",name="Visual theme",description="Presentation colours only",position=13)
    default CoachPreferences.Theme premiumTheme(){return CoachPreferences.Theme.EMERALD;}
    @ConfigItem(keyName="premiumEffects",name="Effect intensity",description="0 static, 1 subtle, 2 cinematic. Reduced motion overrides movement.",position=14)
    @Range(min=0,max=2)
    default int premiumEffects(){return 2;}
    @ConfigItem(keyName="premiumHudCompact",name="Compact HUD",description="Smaller HUD with full-date forecast",position=15)
    default boolean premiumHudCompact(){return false;}
    @ConfigItem(keyName="premiumCompactLayout",name="Compact sidebar",description="Reduce decorative spacing without shrinking body text",position=16)
    default boolean premiumCompactLayout(){return false;}
}

