package com.osrs.combatcoach;

import com.google.inject.Provides;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.game.ItemManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name="Combat Coach", description="Adaptive combat training destinations", tags={"combat","training","xp","map"})
public class CombatCoachPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private EventBus eventBus;
    @Inject private ItemManager itemManager;
    @Inject private WorldMapPointManager mapPoints;
    @Inject private CombatCoachConfig config;
    private CombatCoachPanel panel;
    private NavigationButton button;
    private WorldMapPoint destination;
    private Skill active = Skill.ATTACK;
    private long lastSkillGain = 0;
    private boolean ready;
    private final CombatXpTracker xpTracker = new CombatXpTracker();
    private final CombatSpotTracker spotTracker = new CombatSpotTracker();
    private String currentNpcName = null;
    private int currentXp = 0;
    private Skill measuredSkill;
    private int sessionStartXp = -1;
    private long sessionStartMs;
    private int recentXp = -1;
    private TrainingSpot currentSpot;
    private static final BufferedImage PIN = makePin();

    @Provides CombatCoachConfig provideConfig(ConfigManager manager) { return manager.getConfig(CombatCoachConfig.class); }

    @Override protected void startUp() {
        panel = new CombatCoachPanel(e -> clientThread.invoke(this::refresh),
            e -> clientThread.invoke(this::navigateToRecommendation),
            e -> clientThread.invoke(this::refresh));
        button = NavigationButton.builder().tooltip("Combat Coach").priority(7).panel(panel)
            .icon(ImageUtil.resizeImage(PIN, 24, 24)).build();
        toolbar.addNavigation(button);
        ready = client.getGameState() == GameState.LOGGED_IN;
        refresh();
    }
    @Override protected void shutDown() {
        clearMarker(); toolbar.removeNavigation(button); panel=null; ready=false;
    }
    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        ready = event.getGameState() == GameState.LOGGED_IN;
        if (!ready) { clearMarker(); if (panel != null) SwingUtilities.invokeLater(() -> panel.clear()); }
        else { measuredSkill=null; sessionStartXp=-1; recentXp=-1; xpTracker.reset(); spotTracker.reset(); currentNpcName=null; refresh(); }
    }
    @Subscribe public void onStatChanged(StatChanged event) {
        Skill s=event.getSkill();
        if (!combatSkill(s) || !ready) return;
        // XP changes, not login stat initialization, decide the automatic training focus.
        int xp=event.getXp();
        Integer oldXp=knownXp[s.ordinal()];
        knownXp[s.ordinal()] = xp;
        if (oldXp == null || xp <= oldXp) return;
        if (s == Skill.HITPOINTS) return;
        if (s == selectedSkill() || config.focus() == CombatCoachConfig.Focus.AUTO) {
            xpTracker.addXp(s, xp-oldXp, System.currentTimeMillis());
            spotTracker.gain(xp-oldXp, System.currentTimeMillis());
        }
        active=s;
        if (measuredSkill != s || sessionStartXp < 0) {
            measuredSkill=s;
            sessionStartXp=oldXp;
            sessionStartMs=System.currentTimeMillis();
        }
        recentXp=xp;
        lastSkillGain=System.currentTimeMillis();
        refresh();
    }
    private final Integer[] knownXp = new Integer[Skill.values().length];
    @Subscribe public void onGameTick(GameTick tick) {
        if (!ready || panel==null || client.getLocalPlayer()==null) return;
        Skill chosen=selectedSkill();
        xpTracker.select(chosen);
        if (client.getLocalPlayer().getInteracting() instanceof net.runelite.api.NPC) {
            net.runelite.api.NPC npc=(net.runelite.api.NPC)client.getLocalPlayer().getInteracting();
            currentNpcName=npc.getName();
            xpTracker.observeCombat(chosen,System.currentTimeMillis());
            spotTracker.fighting(currentNpcName,System.currentTimeMillis());
        }
        if (client.getTickCount()%2==0) {
            int xp=client.getSkillExperience(chosen);
            long rate=xpTracker.rate(System.currentTimeMillis());
            SwingUtilities.invokeLater(() -> { if (panel!=null) panel.updateProgress(xp,rate,spotTracker.best(),spotTracker.summary()); });
        }
    }
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {
        if (ready && event.getContainerId() == net.runelite.api.InventoryID.EQUIPMENT.getId()) refresh();
    }
    @Subscribe public void onConfigChanged(ConfigChanged event) {
        if ("combatcoach".equals(event.getGroup())) clientThread.invoke(this::refresh);
    }
    private static boolean combatSkill(Skill s) {
        return s==Skill.ATTACK || s==Skill.STRENGTH || s==Skill.DEFENCE || s==Skill.RANGED || s==Skill.MAGIC || s==Skill.HITPOINTS;
    }
    private Skill selectedSkill() {
        switch(config.focus()) {
            case ATTACK: return Skill.ATTACK;
            case STRENGTH: return Skill.STRENGTH;
            case DEFENCE: return Skill.DEFENCE;
            case RANGED: return Skill.RANGED;
            case MAGIC: return Skill.MAGIC;
            default: return active;
        }
    }
    private void refresh() {
        if (!ready || panel == null) return;
        Skill chosen=selectedSkill();
        int level=client.getRealSkillLevel(chosen);
        CombatLoadout loadout=CombatLoadout.read(client, itemManager);
        TrainingSpot next=TrainingPlanner.choose(chosen, level, loadout, config);
        boolean replace = currentSpot == null || !currentSpot.name.equals(next.name) ||
            !currentSpot.point.equals(next.point) || (destination == null && config.worldMapPin());
        currentSpot=next;
        if (!config.worldMapPin()) clearMarker();
        else if (replace) {
            clearMarker();
            destination = new WorldMapPoint(next.point, PIN);
            destination.setName("Combat Coach: " + next.name);
            mapPoints.add(destination);
        }
        SwingUtilities.invokeLater(() -> { if (panel != null) panel.showRecommendation(chosen.getName(),level,next,loadout,rateFor(chosen)); });
    }
    private String rateFor(Skill chosen) {
        long rate = xpTracker.rate(System.currentTimeMillis());
        return rate < 0 ? "Waiting for combat XP" : String.format(java.util.Locale.US, "%,d XP/hr",rate);
    }
    /* Legacy session-rate implementation retained below for reference only. */
    private String oldSessionRateFor(Skill chosen) {
        if (measuredSkill != chosen || sessionStartXp < 0 || recentXp < sessionStartXp) {
            return "Measured XP/hr: waiting for XP gains";
        }
        long ms = System.currentTimeMillis() - sessionStartMs;
        if (ms < 60000) return "Measured XP/hr: collecting data (1 min minimum)";
        long rate = Math.round((recentXp - sessionStartXp) * 3600000.0 / ms);
        return "Measured XP/hr: " + String.format(java.util.Locale.US, "%,d", rate);
    }
    private void navigateToRecommendation() {
        if (!ready || currentSpot == null || client.getLocalPlayer() == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("start", client.getLocalPlayer().getWorldLocation());
        data.put("target", currentSpot.point);
        // Disable member-only transport methods when configured as F2P.
        // This does NOT make the underlying terrain graph F2P-only.
        if (!config.members()) {
            Map<String, Object> options = new HashMap<>();
            for (String key : new String[]{"useAgilityShortcuts", "useGrappleShortcuts", "useFairyRings",
                    "useGnomeGliders", "useCharterShips", "useMinecarts", "useQuetzals",
                    "useSpiritTrees", "useTeleportationItems", "useTeleportationLevers",
                    "useTeleportationPortals", "useTeleportationSpells", "useWildernessObelisks"}) {
                options.put(key, key.equals("useTeleportationItems") ? "None" : false);
            }
            data.put("config", options);
        }
        eventBus.post(new PluginMessage("shortestpath", "path", data));
    }
    private void clearMarker() { if (destination != null) { mapPoints.remove(destination); destination=null; } }
    private static BufferedImage makePin() {
        BufferedImage i=new BufferedImage(28,28,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=i.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0,0,0,180));g.fillOval(1,1,26,26);
        g.setColor(new Color(60,190,255));g.fillOval(3,3,22,22);
        g.setColor(Color.WHITE);g.fillOval(10,10,8,8);
        g.dispose(); return i;
    }
}
