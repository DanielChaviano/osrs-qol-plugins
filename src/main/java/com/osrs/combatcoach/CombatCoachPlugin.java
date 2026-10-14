package com.osrs.combatcoach;

import com.google.inject.Provides;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import net.runelite.client.util.Filepath;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(internalName="combat-coach",name="Combat Coach",description="Gear-aware training guide with premium goals and capture confirmation",tags={"combat","training","xp","map"})
public class CombatCoachPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private EventBus eventBus;
    @Inject private ItemManager itemManager;
    @Inject private WorldMapPointManager mapPoints;
    @Inject private CombatCoachConfig config;
    @Inject private ConfigManager configManager;
    @Inject private OverlayManager overlayManager;
    @Inject private Gson gson;
    @Inject private ScheduledExecutorService executor;
    private CombatCoachPanel panel;
    private NavigationButton button;
    private WorldMapPoint destination;
    private CombatCoachOverlay hud;
    private final CoachSession session=new CoachSession();
    private java.util.List<CoachCaptureRecord> recent=java.util.Collections.emptyList();
    private final java.util.Set<String> loadedThumbs=new java.util.HashSet<>();
    private String routeStatus="";
    private net.runelite.api.coords.WorldPoint routePoint;
    private volatile CoachUiState uiState=CoachUiState.waiting();
    private final CombatXpTracker xpTracker=new CombatXpTracker();
    private final CombatSpotTracker spotTracker=new CombatSpotTracker();
    private final EnumMap<Skill,Integer> knownXp=new EnumMap<>(Skill.class);
    private Skill active=Skill.ATTACK, tracked;
    private boolean initialized;
    private volatile boolean running;
    private TrainingSpot currentSpot;
    private CombatLoadout loadout;
    private String prediction="", lastCapture="", notice="";
    private long noticeUntil, testRequested;
    private static final BufferedImage PIN=makePin();
    private CoachImageActions imageActions;

    @Provides CombatCoachConfig provideConfig(ConfigManager manager){return manager.getConfig(CombatCoachConfig.class);}
    @Override protected void startUp(){
        imageActions=new CoachImageActions(executor,message->clientThread.invoke(()->{if(running){setNotice(message);publish();}}));
        running=true;initialized=false;knownXp.clear();xpTracker.reset();spotTracker.reset();session.reset();loadedThumbs.clear();
        recent=CoachCaptureHistory.load(configManager,gson);if(!recent.isEmpty())lastCapture=recent.get(0).file;
        panel=new CombatCoachPanel((action,value)->clientThread.invoke(()->handleUi(action,value)));
        button=NavigationButton.builder().tooltip("Combat Coach").priority(7).panel(panel).icon(ImageUtil.resizeImage(PIN,24,24)).build();
        toolbar.addNavigation(button);
        hud=new CombatCoachOverlay(this,()->uiState);
        overlayManager.add(hud);loadThumbnails();
        clientThread.invoke(()->{if(client.getGameState()==GameState.LOGGED_IN){initialize();refresh();}});
    }
    @Override protected void shutDown(){
        running=false;clearMarker();
        if(imageActions!=null){imageActions.close();imageActions=null;}
        if(hud!=null)overlayManager.remove(hud);
        if(button!=null)toolbar.removeNavigation(button);
        CombatCoachPanel old=panel;panel=null;if(old!=null)SwingUtilities.invokeLater(old::disposePanel);
        xpTracker.reset();spotTracker.reset();knownXp.clear();initialized=false;uiState=CoachUiState.waiting();
    }
    private void initialize(){
        knownXp.clear();
        for(Skill s:Skill.values())if(s!=Skill.OVERALL)knownXp.put(s,client.getSkillExperience(s));
        initialized=true;tracked=selectedSkill();xpTracker.select(tracked);session.select(tracked.name());
    }
    @Subscribe public void onGameStateChanged(GameStateChanged e){
        if(e.getGameState()==GameState.LOGIN_SCREEN||e.getGameState()==GameState.HOPPING){
            initialized=false;knownXp.clear();xpTracker.reset();spotTracker.reset();loadout=null;currentSpot=null;clearMarker();
            session.reset();routeStatus="";routePoint=null;publishWaiting();
        }
        if(e.getGameState()==GameState.LOGGED_IN)clientThread.invoke(()->{if(!initialized)initialize();refresh();});
    }
    @Subscribe public void onGameTick(GameTick e){
        if(!running||client.getGameState()!=GameState.LOGGED_IN)return;
        if(!initialized)initialize();
        Skill s=selectedSkill();selectTracker(s);
        if(client.getLocalPlayer()!=null&&client.getLocalPlayer().getInteracting() instanceof NPC){
            NPC npc=(NPC)client.getLocalPlayer().getInteracting();long now=System.currentTimeMillis();
            xpTracker.observeCombat(s,now);spotTracker.fighting(npc.getName(),now);
        }
        if(testRequested>0&&System.currentTimeMillis()-testRequested>45000){testRequested=0;setNotice("No capture response. Enable Milestone Screenshots in RuneLite configuration, then test again.");}
        if(routePoint!=null&&client.getLocalPlayer()!=null){
            net.runelite.api.coords.WorldPoint here=client.getLocalPlayer().getWorldLocation();
            if(here.getPlane()==routePoint.getPlane()&&here.distanceTo(routePoint)<=12)routeStatus="At the training area";
            else routeStatus="Route requested - follow manually";
        }
        if(currentSpot==null||loadout==null)refresh();
        else if(client.getTickCount()%2==0)publish();
    }
    @Subscribe public void onStatChanged(StatChanged e){
        if(!running||client.getGameState()!=GameState.LOGGED_IN||!initialized)return;
        Skill s=e.getSkill();Integer old=knownXp.put(s,e.getXp());
        if(old==null||e.getXp()<=old)return;
        if(isTrainingSkill(s)){
            if(config.focus()==CombatCoachConfig.Focus.AUTO)active=s;
            Skill chosen=selectedSkill();selectTracker(chosen);
            if(s==chosen){
                long now=System.currentTimeMillis();
                if(client.getLocalPlayer()!=null&&client.getLocalPlayer().getInteracting() instanceof NPC)
                    spotTracker.fighting(((NPC)client.getLocalPlayer().getInteracting()).getName(),now);
                xpTracker.addXp(s,e.getXp()-old,now);spotTracker.gain(e.getXp()-old,now);session.gain(e.getXp()-old,now);
            }
        }
        if(isTrainingSkill(s)||s==Skill.HITPOINTS||s==Skill.PRAYER)refresh();
    }
    private void selectTracker(Skill s){if(tracked!=s){tracked=s;xpTracker.select(s);spotTracker.reset();session.select(s.name());}}
    @Subscribe public void onItemContainerChanged(ItemContainerChanged e){
        if(running&&initialized&&e.getContainerId()==InventoryID.EQUIPMENT.getId()){xpTracker.reset();spotTracker.reset();session.reset();refresh();}
    }
    @Subscribe public void onConfigChanged(ConfigChanged e){
        if(!running)return;
        if("combatcoach".equals(e.getGroup())||"milestoneScreenshots".equals(e.getGroup()))
            clientThread.invoke(()->{
                if("focus".equals(e.getKey())||"members".equals(e.getKey())){xpTracker.reset();spotTracker.reset();session.reset();tracked=null;}
                if("premiumCaptureHistoryV4".equals(e.getKey())){recent=CoachCaptureHistory.load(configManager,gson);loadThumbnails();}
                refresh();
            });
    }
    private static boolean isTrainingSkill(Skill s){return s==Skill.ATTACK||s==Skill.STRENGTH||s==Skill.DEFENCE||s==Skill.RANGED||s==Skill.MAGIC;}
    private Skill selectedSkill(){
        switch(config.focus()){
            case ATTACK:return Skill.ATTACK;case STRENGTH:return Skill.STRENGTH;case DEFENCE:return Skill.DEFENCE;
            case RANGED:return Skill.RANGED;case MAGIC:return Skill.MAGIC;default:return active;
        }
    }
    private void refresh(){
        if(!running)return;
        if(client.getGameState()!=GameState.LOGGED_IN){publishWaiting();return;}
        Skill chosen=selectedSkill();selectTracker(chosen);
        loadout=CombatLoadout.read(client,itemManager);
        TrainingSpot next=TrainingPlanner.choose(chosen,client.getRealSkillLevel(chosen),loadout,config);
        if(currentSpot!=null&&!currentSpot.point.equals(next.point)){routePoint=null;routeStatus="";}
        currentSpot=next;
        prediction=CombatXpEstimator.compare(chosen,loadout,config.members());
        if(!config.worldMapPin())clearMarker();
        else if(destination==null||!destination.getWorldPoint().equals(currentSpot.point)){
            clearMarker();destination=new WorldMapPoint(currentSpot.point,PIN);destination.setName("Combat Coach: "+currentSpot.name);mapPoints.add(destination);
        }
        publish();
    }
    private int goal(Skill skill){
        String value=configManager.getConfiguration("combatcoach","premiumGoal"+skill.name());
        try{return Math.max(2,Math.min(99,Integer.parseInt(value)));}catch(Exception ignored){return 20;}
    }
    private boolean captureEnabled(){return !"false".equalsIgnoreCase(configManager.getConfiguration("milestoneScreenshots","captureEnabled"));}
    private void publish(){
        if(currentSpot==null||loadout==null){publishWaiting();return;}
        Skill s=selectedSkill();long now=System.currentTimeMillis();
        String visibleNotice=now<noticeUntil?notice:"";
        uiState=new CoachUiState(true,s.getName(),client.getRealSkillLevel(s),client.getSkillExperience(s),goal(s),xpTracker.rate(now),
            new int[]{loadout.attack,loadout.strength,loadout.defence,loadout.hitpoints,loadout.ranged,loadout.magic,loadout.prayer},
            currentSpot.name,currentSpot.reason,currentSpot.requirements,spotTracker.best(),prediction,
            config.premiumHud(),config.premiumPopups(),config.premiumReducedMotion(),captureEnabled(),lastCapture,visibleNotice,
            preferences(),recent,session.gained(),session.elapsed(now),session.idle(now),session.trend(xpTracker.rate(now)),routeStatus);
        sendToPanel();
    }
    private void publishWaiting(){
        uiState=new CoachUiState(false,selectedSkill().getName(),1,0,goal(selectedSkill()),-1,new int[7],"Log in to begin","","","","",
            config.premiumHud(),config.premiumPopups(),config.premiumReducedMotion(),captureEnabled(),lastCapture,System.currentTimeMillis()<noticeUntil?notice:"",
            preferences(),recent,0,0,-1,0,"");
        sendToPanel();
    }
    private boolean screenshotOption(String key,boolean fallback){String value=configManager.getConfiguration("milestoneScreenshots",key);return value==null?fallback:Boolean.parseBoolean(value);}
    private CoachPreferences preferences(){return new CoachPreferences(config.premiumTheme(),config.premiumEffects(),config.premiumReducedMotion(),config.premiumHudCompact(),config.premiumCompactLayout(),screenshotOption("includeCelebration",true),screenshotOption("saveCard",true),screenshotOption("includeFrame",false));}
    private void loadThumbnails(){
        for(CoachCaptureRecord record:recent){if(!loadedThumbs.add(record.id))continue;
            executor.execute(()->{try{Filepath file=CoachScreenshotFiles.resolve(record.cardFile.isEmpty()?record.file:record.cardFile);BufferedImage im=CoachThumbnails.load(file,150,180);
                SwingUtilities.invokeLater(()->{if(running&&panel!=null)panel.thumbnail(record.id,im);});
            }catch(Exception ignored){}});
        }
    }
    private void sendToPanel(){CoachUiState snapshot=uiState;CombatCoachPanel target=panel;SwingUtilities.invokeLater(()->{if(running&&target!=null&&target==panel)target.update(snapshot);});}
    private void setNotice(String message){notice=message;noticeUntil=System.currentTimeMillis()+16000;}
    private void handleUi(String action,String value){
        if(!running)return;
        if(action.startsWith("view:")||action.startsWith("card:")){
            boolean card=action.startsWith("card:");String id=action.substring(5);
            for(CoachCaptureRecord record:recent)if(record.id.equals(id)){String path=card?record.cardFile:record.file;if(!path.isEmpty())openFile(path);break;}return;
        }
        switch(action){
            case "goal":try{int target=Math.max(2,Math.min(99,Integer.parseInt(value)));configManager.setConfiguration("combatcoach","premiumGoal"+selectedSkill().name(),target);}catch(NumberFormatException ignored){}break;
            case "focus":try{configManager.setConfiguration("combatcoach","focus",CombatCoachConfig.Focus.valueOf(value));}catch(IllegalArgumentException ignored){}break;
            case "hud":configManager.setConfiguration("combatcoach","premiumHud",!config.premiumHud());break;
            case "popups":configManager.setConfiguration("combatcoach","premiumPopups",!config.premiumPopups());break;
            case "motion":configManager.setConfiguration("combatcoach","premiumReducedMotion",!config.premiumReducedMotion());break;
            case "captures":configManager.setConfiguration("milestoneScreenshots","captureEnabled",!captureEnabled());break;
            case "theme":configManager.setConfiguration("combatcoach","premiumTheme",CoachPreferences.Theme.parse(value));break;
            case "effects":try{configManager.setConfiguration("combatcoach","premiumEffects",Math.max(0,Math.min(2,Integer.parseInt(value))));}catch(NumberFormatException ignored){}break;
            case "hudCompact":configManager.setConfiguration("combatcoach","premiumHudCompact",!config.premiumHudCompact());break;
            case "compactLayout":configManager.setConfiguration("combatcoach","premiumCompactLayout",!config.premiumCompactLayout());break;
            case "includeCelebration":configManager.setConfiguration("milestoneScreenshots","includeCelebration",!screenshotOption("includeCelebration",true));break;
            case "saveCard":configManager.setConfiguration("milestoneScreenshots","saveCard",!screenshotOption("saveCard",true));break;
            case "includeFrame":configManager.setConfiguration("milestoneScreenshots","includeFrame",!screenshotOption("includeFrame",false));break;
            case "navigate":navigate();break;
            case "testCapture":
                if(client.getGameState()!=GameState.LOGGED_IN){setNotice("Log into the game before taking a test capture.");break;}
                testRequested=System.currentTimeMillis();setNotice("Preparing the animated card, then capturing it. Keep RuneLite visible.");
                Map<String,Object> data=new HashMap<>();data.put("skill",selectedSkill().name());
                eventBus.post(new PluginMessage("milestoneScreenshots","testCapture",data));break;
            case "openFolder":if(imageActions!=null)imageActions.copyFolder(lastCapture);break;
            case "openLatest":if(!lastCapture.isEmpty())openFile(lastCapture);else setNotice("No screenshot has been confirmed in this session.");break;
            case "refresh":setNotice("Equipment and recommendation refreshed.");break;
            default:break;
        }
        refresh();
    }
    @Subscribe public void onMilestoneCaptureEvent(MilestoneCaptureEvent event){
        clientThread.invoke(()->{
            if(!running)return;testRequested=0;
            if(event.success&&event.file!=null){lastCapture=event.file.toString();setNotice(event.test?"Test screenshot saved. The popup shows the actual file.":"Milestone screenshot saved.");}
            else setNotice(event.message);
            recent=CoachCaptureHistory.load(configManager,gson);loadThumbnails();publish();
        });
    }
    private void openFile(String path){
        if(imageActions!=null)imageActions.preview(path);
    }
    private void navigate(){
        if(client.getLocalPlayer()==null||currentSpot==null)return;
        Map<String,Object> data=new HashMap<>();data.put("start",client.getLocalPlayer().getWorldLocation());data.put("target",currentSpot.point);
        if(!config.members()){
            Map<String,Object> options=new HashMap<>();
            for(String key:new String[]{"useAgilityShortcuts","useGrappleShortcuts","useFairyRings","useGnomeGliders","useCharterShips","useMinecarts","useQuetzals","useSpiritTrees","useTeleportationItems","useTeleportationLevers","useTeleportationPortals","useTeleportationSpells","useWildernessObelisks"})
                options.put(key,key.equals("useTeleportationItems")?"None":false);
            data.put("config",options);
        }
        eventBus.post(new PluginMessage("shortestpath","path",data));
        routePoint=currentSpot.point;routeStatus="Route requested - follow manually";
        setNotice("Route requested from Shortest Path. Follow it with your own clicks.");
    }
    private void clearMarker(){if(destination!=null){mapPoints.remove(destination);destination=null;}}
    private static BufferedImage makePin(){
        BufferedImage icon=CoachArt.image("brand");if(icon!=null)return icon;
        BufferedImage im=new BufferedImage(28,28,BufferedImage.TYPE_INT_ARGB);java.awt.Graphics2D g=im.createGraphics();g.setColor(CoachArt.GOLD);g.fillOval(3,3,22,22);g.dispose();return im;
    }
}
