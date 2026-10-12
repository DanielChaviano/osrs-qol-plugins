package com.osrs.combatcoach;

import net.runelite.api.Skill;

/** Equipment-aware conservative recommendations. Not a full NPC DPS simulator. */
final class TrainingPlanner {
    private TrainingPlanner() {}

    static TrainingSpot choose(Skill skill, int level, CombatLoadout gear, CombatCoachConfig c) {
        boolean ranged=skill==Skill.RANGED;
        boolean magic=skill==Skill.MAGIC;
        boolean melee=!ranged && !magic;
        // Strength by itself is not an adequate difficulty estimate. Low attack
        // and a weak weapon lead to missed attacks even with high strength.
        int effectiveMelee=Math.min(gear.attack, Math.max(gear.strength,gear.defence));
        int effective=melee?effectiveMelee:level;
        boolean weakWeapon=melee && gear.weaponTier<5;
        boolean lowSurvival=gear.defence<20 || gear.hitpoints<25;
        boolean cautious=c.priority()==CombatCoachConfig.Priority.LOW_ATTENTION;
        String context="Gear: "+gear.weapon+"; Attack "+gear.attack+", Strength "+gear.strength+
            ", Defence "+gear.defence+", Hitpoints "+gear.hitpoints+". ";

        if (!c.members()) {
            if (effective<10 || weakWeapon)
                return new TrainingSpot("Lumbridge chickens",context+"Low defence keeps accuracy high while you improve your weapon or Attack level.",
                    "Free to play",3235,3296);
            if (effective<35 || lowSurvival || cautious)
                return new TrainingSpot("Lumbridge cows",context+"Good low-defence training given your current damage and survival.",
                    "Free to play",3253,3270);
            return new TrainingSpot("Al Kharid warriors",context+"An accessible F2P option; compare observed XP/hour with cows before committing.",
                "Free to play; bring food",3293,3170);
        }
        if (effective<10 || weakWeapon)
            return new TrainingSpot("Lumbridge chickens",context+"Very low defence, especially useful with weak melee accuracy.",
                "None",3235,3296);
        if (effective<25 || lowSurvival)
            return new TrainingSpot("Sand Crabs",context+"Low defensive stats and manageable combat; bring food if needed.",
                "Hosidius beach",1775,3463);
        if (c.boneVoyage() && (cautious || effective<60))
            return new TrainingSpot("Ammonite Crabs",context+"Low-defence, high-HP targets for sustained training.",
                "Bone Voyage",3715,3897);
        if (effective<50 || (ranged && level<70) || (magic && level<60))
            return new TrainingSpot("Sand Crabs",context+"Reliable low-defence fallback; XP depends on actual damage and downtime.",
                "Hosidius beach",1775,3463);
        if (c.naguas() && melee && level>=60 && gear.attack>=55 && gear.strength>=60)
            return new TrainingSpot("Sulphur Naguas",context+"Potential high melee XP, but specialized weapons and unlocks matter.",
                "Perilous Moons access and 48 Slayer; check other requirements",1492,9575);
        if (c.nightmareZone() && level>=60)
            return new TrainingSpot("Nightmare Zone",context+"Sustained XP depends on unlocked bosses, setup and gear.",
                "Five eligible quests, coins and supplies",2608,3116);
        if (c.boneVoyage())
            return new TrainingSpot("Ammonite Crabs",context+"Fallback until advanced training methods are unlocked.",
                "Bone Voyage",3715,3897);
        return new TrainingSpot("Sand Crabs",context+"Accessible fallback. Improve equipment and unlock other training areas.",
            "Hosidius beach",1775,3463);
    }
}
