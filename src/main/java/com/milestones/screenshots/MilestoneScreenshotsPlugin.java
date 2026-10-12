package com.milestones.screenshots;

import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageCapture;

@PluginDescriptor(name = "Milestone Screenshots", description = "Save screenshots only at selected skill levels", tags = {"screenshot", "level", "milestone"})
public class MilestoneScreenshotsPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ImageCapture imageCapture;
    @Inject private MilestoneScreenshotsConfig config;

    private final EnumMap<Skill, Integer> knownLevels = new EnumMap<>(Skill.class);
    private final Deque<Capture> captures = new ArrayDeque<>();
    private boolean initialized;
    private int ticks;

    @Provides
    MilestoneScreenshotsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MilestoneScreenshotsConfig.class);
    }

    @Override
    protected void startUp()
    {
        clearSession();
    }

    @Override
    protected void shutDown()
    {
        clearSession();
    }

    private void clearSession()
    {
        knownLevels.clear();
        captures.clear();
        initialized = false;
        ticks = 0;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        // Keep values across brief hops; re-establish baselines after a logout.
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            clearSession();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        ticks++;
        if (!initialized)
        {
            for (Skill skill : Skill.values())
            {
                if (skill != Skill.OVERALL)
                {
                    int level = client.getRealSkillLevel(skill);
                    if (level > 0) knownLevels.put(skill, level);
                }
            }
            initialized = true;
        }
        while (!captures.isEmpty() && captures.peekFirst().dueTick <= ticks)
        {
            Capture capture = captures.removeFirst();
            String file = capture.skill.name().toLowerCase() + "-level-" + capture.level + "-" + System.currentTimeMillis();
            imageCapture.takeScreenshot("Milestones", file, config.includeFrame(), config.notifyOnScreenshot(), false);
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !initialized) return;
        Skill skill = event.getSkill();
        if (skill == Skill.OVERALL) return;
        int now = event.getLevel();
        Integer before = knownLevels.put(skill, now);
        if (before == null || now <= before) return;
        Set<Integer> milestones = parseLevels(config.levels());
        for (int level = before + 1; level <= now; level++)
        {
            if (milestones.contains(level))
            {
                // Wait roughly two ticks for the level-up chat/dialog to render.
                captures.addLast(new Capture(skill, level, ticks + 2));
            }
        }
    }

    static Set<Integer> parseLevels(String raw)
    {
        Set<Integer> out = new HashSet<>();
        if (raw == null) return out;
        for (String token : raw.split(","))
        {
            try
            {
                int level = Integer.parseInt(token.trim());
                if (level >= 2 && level <= 99) out.add(level);
            }
            catch (NumberFormatException ignored) { /* Ignore invalid entries */ }
        }
        return out;
    }

    private static final class Capture
    {
        final Skill skill;
        final int level;
        final int dueTick;
        Capture(Skill skill, int level, int dueTick)
        {
            this.skill = skill;
            this.level = level;
            this.dueTick = dueTick;
        }
    }
}
