package com.milestones.screenshots;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("milestoneScreenshots")
public interface MilestoneScreenshotsConfig extends Config {
    @ConfigItem(keyName="captureEnabled",name="Automatic milestone captures",description="Save configured level milestones automatically",position=0)
    default boolean captureEnabled(){return true;}
    @ConfigItem(keyName="levels",name="Screenshot levels",description="Comma-separated real skill levels from 2 to 99",position=1)
    default String levels(){return "20,40,60,80,99";}
    @ConfigItem(keyName="includeFrame",name="Include RuneLite frame",description="Include client frame and sidebar",position=2)
    default boolean includeFrame(){return false;}
    @ConfigItem(keyName="notify",name="Desktop notification",description="Additional system notification after a saved screenshot",position=3)
    default boolean notifyOnScreenshot(){return true;}
    @ConfigItem(keyName="includeCelebration",name="Include level-up card",description="Wait for the new milestone card to be fully painted before taking the game screenshot",position=4)
    default boolean includeCelebration(){return true;}
    @ConfigItem(keyName="saveCard",name="Save clean card PNG",description="Also save a high-resolution card-only PNG alongside the actual game screenshot",position=5)
    default boolean saveCard(){return true;}
}

