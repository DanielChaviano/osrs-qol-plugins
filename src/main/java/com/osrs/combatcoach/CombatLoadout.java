package com.osrs.combatcoach;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

/** A snapshot of the visible equipped loadout, not a simulated DPS calculation. */
final class CombatLoadout {
    final int attack, strength, defence, hitpoints, ranged, magic, prayer;
    final String weapon, armor;
    final int armorPieces;
    final int weaponTier;

    private CombatLoadout(int attack, int strength, int defence, int hitpoints, int ranged,
                          int magic, int prayer, String weapon, String armor, int armorPieces, int weaponTier) {
        this.attack=attack; this.strength=strength; this.defence=defence; this.hitpoints=hitpoints;
        this.ranged=ranged; this.magic=magic; this.prayer=prayer;
        this.weapon=weapon; this.armor=armor; this.armorPieces=armorPieces; this.weaponTier=weaponTier;
    }

    static CombatLoadout read(Client client, ItemManager items) {
        ItemContainer equipment=client.getItemContainer(InventoryID.EQUIPMENT);
        String weapon="Unarmed";
        StringBuilder armor=new StringBuilder();
        int pieces=0;
        int tier=0;
        if (equipment!=null) {
            int weaponSlot=EquipmentInventorySlot.WEAPON.getSlotIdx();
            Item[] equipped=equipment.getItems();
            for (int slot=0;slot<equipped.length;slot++) {
                Item item=equipped[slot];
                if (item==null || item.getId()<=0) continue;
                String name=items.getItemComposition(item.getId()).getName();
                if (name==null || name.trim().isEmpty()) continue;
                if (slot==weaponSlot) {
                    weapon=name;
                    tier=estimateTier(name);
                } else {
                    if (pieces<6) {
                        if (armor.length()>0) armor.append(", ");
                        armor.append(name);
                    }
                    pieces++;
                }
            }
        }
        if (pieces>6) armor.append(", ...");
        return new CombatLoadout(client.getRealSkillLevel(Skill.ATTACK),
            client.getRealSkillLevel(Skill.STRENGTH), client.getRealSkillLevel(Skill.DEFENCE),
            client.getRealSkillLevel(Skill.HITPOINTS),client.getRealSkillLevel(Skill.RANGED),
            client.getRealSkillLevel(Skill.MAGIC),client.getRealSkillLevel(Skill.PRAYER),
            weapon, pieces==0?"None":armor.toString(),pieces,tier);
    }

    private static int estimateTier(String name) {
        String n=name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("dragon") || n.contains("abyssal") || n.contains("whip") ||
            n.contains("scimitar of") || n.contains("zombie axe")) return 60;
        if (n.contains("rune")) return 40;
        if (n.contains("adamant")) return 30;
        if (n.contains("mithril")) return 20;
        if (n.contains("black")) return 10;
        if (n.contains("steel")) return 5;
        if (n.contains("iron")) return 1;
        if (n.contains("bronze")) return 1;
        return 0; // Unknown weapon: never assume it has useful melee stats.
    }
}
