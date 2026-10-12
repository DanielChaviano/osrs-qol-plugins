package com.osrs.combined;
import com.osrs.combatcoach.CombatCoachPlugin;
import com.milestones.screenshots.MilestoneScreenshotsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
public final class CombinedLauncher {
    public static void main(String[] args)throws Exception {
        ExternalPluginManager.loadBuiltin(CombatCoachPlugin.class,MilestoneScreenshotsPlugin.class);
        System.out.println("COMBAT COACH 4.0.1-rc.1 - both plugins registered");
        RuneLite.main(args);
    }
}
