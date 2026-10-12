package com.osrs.combatcoach;

import net.runelite.api.Skill;

/** Conservative curated destinations, not calculated optimal DPS. */
final class TrainingPlanner {
    private TrainingPlanner() {}
    static TrainingSpot choose(Skill skill, int level, int attack, int strength, int defence, CombatCoachConfig c) {
        boolean ranged = skill == Skill.RANGED;
        boolean magic = skill == Skill.MAGIC;
        if (!c.members()) {
            if (level < 15) return new TrainingSpot("Lumbridge chickens", "Low defence and safe first combat practice", "Free to play", 3235, 3296);
            if (level < 40) return new TrainingSpot("Lumbridge cows", "Easy low-level target; collect cowhides", "Free to play", 3253, 3270);
            return new TrainingSpot("Al Kharid warriors", "Accessible F2P combat training; evaluate food and accuracy", "Free to play", 3293, 3170);
        }
        if (level < 15) return new TrainingSpot("Lumbridge chickens", "Low defence and quick kills for your first levels", "None", 3235, 3296);
        if (level < 30) return new TrainingSpot("Sand Crabs", "High Hitpoints with low defence; good sustained XP", "Travel to Hosidius beach, Kourend", 1775, 3463);
        if (c.boneVoyage() && (level < 60 || c.priority() == CombatCoachConfig.Priority.LOW_ATTENTION))
            return new TrainingSpot("Ammonite Crabs", "100 Hitpoints, low defence and low-attention combat", "Bone Voyage", 3715, 3897);
        if (level < 50 || (ranged && level < 70) || (magic && level < 60))
            return new TrainingSpot("Sand Crabs", "Steady low-defence targets; bring appropriate weapon or spells", "Hosidius beach", 1775, 3463);
        if (c.naguas() && !ranged && !magic && level >= 60 && attack >= 55 && strength >= 60)
            return new TrainingSpot("Sulphur Naguas", "Strong melee XP with specialized weapons and potions; see in-game map", "Perilous Moons access; 48 Slayer and additional requirements", 1492, 9575);
        if (c.nightmareZone() && level >= 60)
            return new TrainingSpot("Nightmare Zone", "Private, sustained combat; actual XP depends on quest bosses and loadout", "Five eligible quests; admission and supplies", 2608, 3116);
        if (c.boneVoyage())
            return new TrainingSpot("Ammonite Crabs", "Reliable low-attention option until stronger training is unlocked", "Bone Voyage", 3715, 3897);
        return new TrainingSpot("Sand Crabs", "Accessible fallback; unlock Fossil Island, NMZ or Naguas for more options", "Hosidius beach", 1775, 3463);
    }
}
