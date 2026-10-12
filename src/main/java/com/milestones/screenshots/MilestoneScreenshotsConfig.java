package com.milestones.screenshots;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("milestoneScreenshots")
public interface MilestoneScreenshotsConfig extends Config
{
    @ConfigItem(keyName = "levels", name = "Screenshot levels", description = "Comma-separated skill levels between 2 and 99")
    default String levels() { return "20,40,60,80,99"; }

    @ConfigItem(keyName = "includeFrame", name = "Include RuneLite frame", description = "Include client frame and sidebar in screenshots")
    default boolean includeFrame() { return false; }

    @ConfigItem(keyName = "notify", name = "Notify on screenshot", description = "Show a notification when a screenshot is saved")
    default boolean notifyOnScreenshot() { return false; }
}
