package com.osrs.combatcoach;

import net.runelite.api.coords.WorldPoint;

final class TrainingSpot {
    final String name, reason, requirements;
    final WorldPoint point;
    TrainingSpot(String name, String reason, String requirements, int x, int y) {
        this.name=name; this.reason=reason; this.requirements=requirements;
        this.point=new WorldPoint(x,y,0);
    }
}
