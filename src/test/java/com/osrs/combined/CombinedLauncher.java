package com.osrs.combined;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import com.osrs.combatcoach.CombatCoachPlugin;
import com.milestones.screenshots.MilestoneScreenshotsPlugin;

public class CombinedLauncher {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(CombatCoachPlugin.class);
        ExternalPluginManager.loadBuiltin(MilestoneScreenshotsPlugin.class);
        RuneLite.main(args);
    }
}
