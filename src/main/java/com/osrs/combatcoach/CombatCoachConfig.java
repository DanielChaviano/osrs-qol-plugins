package com.osrs.combatcoach;

import net.runelite.client.config.*;

@ConfigGroup("combatcoach")
public interface CombatCoachConfig extends Config {
    enum Focus { AUTO, ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC }
    enum Priority { FAST_XP, LOW_ATTENTION }
    @ConfigItem(keyName="focus", name="Training skill", description="Auto follows skill XP gains or set a skill manually", position=0)
    default Focus focus() { return Focus.AUTO; }
    @ConfigItem(keyName="priority", name="Training priority", description="Fast XP or relaxed training", position=1)
    default Priority priority() { return Priority.FAST_XP; }
    @ConfigItem(keyName="members", name="Members account", description="Include members-only places. Disable for F2P.", position=2)
    default boolean members() { return true; }
    @ConfigItem(keyName="boneVoyage", name="Bone Voyage completed", description="Allows Ammonite Crabs on Fossil Island", position=3)
    default boolean boneVoyage() { return false; }
    @ConfigItem(keyName="nightmareZone", name="Nightmare Zone unlocked", description="You need at least five eligible completed quests", position=4)
    default boolean nightmareZone() { return false; }
    @ConfigItem(keyName="naguas", name="Sulphur Naguas unlocked", description="Perilous Moons access and other requirements are your responsibility", position=5)
    default boolean naguas() { return false; }
    @ConfigItem(keyName="worldMapPin", name="World map destination marker", description="Show the suggested destination on the full world map", position=6)
    default boolean worldMapPin() { return true; }
}
