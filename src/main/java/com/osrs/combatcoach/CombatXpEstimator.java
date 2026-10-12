package com.osrs.combatcoach;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * Baseline melee comparison. Uses actual equipped accuracy/strength/speed values,
 * NPC defensive stats and estimated target-switch overhead. Assumes aggressive
 * melee style, no prayers/potions/specs, no special damage mechanics or crowding.
 * Outputs a MODEL, not independently verified combat XP/hour.
 */
final class CombatXpEstimator {
    private CombatXpEstimator(){}

    static String compare(Skill skill, CombatLoadout loadout, boolean members) {
        if (skill!=Skill.ATTACK && skill!=Skill.STRENGTH && skill!=Skill.DEFENCE)
            return "Melee estimates only for now. Ranged and Magic models pending.";
        Candidate[] f2p={
            new Candidate("Chicken",3,1,0,0),
            new Candidate("Cow",8,1,0,0),
            new Candidate("Giant frog",23,6,0,0),
            new Candidate("Flesh crawler",25,10,15,0),
            new Candidate("Hill giant",35,26,0,0)
        };
        int styleBonus=skill==Skill.STRENGTH?3:0;
        int effStr=loadout.strength+8+styleBonus;
        int maxHit=Math.max(1,(int)Math.floor(0.5+effStr*(loadout.strengthBonus+64)/640.0));
        int accuracy=loadout.attack+8+(skill==Skill.ATTACK?3:0);
        int atkBonus=Math.max(loadout.stabBonus,Math.max(loadout.slashBonus,loadout.crushBonus));
        int attackRoll=accuracy*(64+atkBonus);
        int speed=Math.max(2,loadout.attackSpeed);
        StringBuilder report=new StringBuilder();
        Candidate top=null; double topRate=-1;
        for(Candidate npc:f2p) {
            int defenceRoll=(npc.defence+9)*(64+npc.defenceBonus);
            double hit=attackRoll>defenceRoll?
                1.0-(defenceRoll+2.0)/(2.0*(attackRoll+1.0)):
                attackRoll/(2.0*(defenceRoll+1.0));
            hit=Math.max(0.0,Math.min(1.0,hit));
            // Uniform hits from zero to max hit. Approximate damage truncation on final hit.
            double damage=0;
            for(int roll=0;roll<=maxHit;roll++) damage+=Math.min(roll,npc.hp);
            damage=hit*damage/(maxHit+1);
            if(damage<=0.001) continue;
            // Two extra combat ticks/kill approximate reacquisition overhead.
            double attacks=(npc.hp/damage)+2.0/(double)speed;
            double seconds=attacks*speed*0.6;
            double rate=4.0*npc.hp*3600.0/seconds;
            // Low HP / defence survival: risks are not simulated. Restrict dangerous
            // targets when low-health and low-defence rather than implying safe access.
            if (npc.defence>=10 && (loadout.hitpoints<20 || loadout.defence<15)) continue;
            if(report.length()>0) report.append("\n");
            report.append(npc.name).append(": ~").append(String.format(Locale.US,"%,d",(long)rate)).append(" XP/hr");
            if(rate>topRate){topRate=rate;top=npc;}
        }
        if (top==null) return "Not enough equipment data to estimate.";
        return "Highest modelled: "+top.name+"\n"+report+
            "\nEstimates assume continual attacking, approximate target switching and accessible spawns. "+
            "Real XP/hr may be much lower; compare observed sessions.";
    }
    private static final class Candidate {
        final String name;final int hp,defence,defenceBonus,unused;
        Candidate(String name,int hp,int defence,int defenceBonus,int unused) {
            this.name=name;this.hp=hp;this.defence=defence;this.defenceBonus=defenceBonus;this.unused=unused;
        }
    }
}
