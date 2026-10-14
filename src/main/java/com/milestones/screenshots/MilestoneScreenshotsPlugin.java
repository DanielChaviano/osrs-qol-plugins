package com.milestones.screenshots;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrs.combatcoach.CoachCaptureHistory;
import com.osrs.combatcoach.CoachCaptureRecord;
import com.osrs.combatcoach.CoachMilestoneArt;
import com.osrs.combatcoach.CoachPreferences;
import com.osrs.combatcoach.CoachThumbnails;
import com.osrs.combatcoach.CoachToastOverlay;
import com.osrs.combatcoach.CombatCoachConfig;
import com.osrs.combatcoach.MilestoneCaptureEvent;
import com.osrs.combatcoach.MilestoneCaptureSequence;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import net.runelite.client.util.Filepath;
import com.osrs.combatcoach.CoachScreenshotFiles;
import com.osrs.combatcoach.CoachImageActions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ScreenshotTaken;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageCapture;

@PluginDescriptor(internalName="combat-coach-milestones",name="Milestone Screenshots",description="Capture fully rendered milestone cards and confirm saved images",tags={"screenshot","level","milestone"})
public class MilestoneScreenshotsPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ImageCapture imageCapture;
    @Inject private MilestoneScreenshotsConfig config;
    @Inject private ConfigManager configManager;
    @Inject private EventBus eventBus;
    @Inject private Notifier notifier;
    @Inject private OverlayManager overlays;
    @Inject private MouseManager mouseManager;
    @Inject private ScheduledExecutorService executor;
    @Inject private Gson gson;
    private final EnumMap<Skill,Integer> levels=new EnumMap<>(Skill.class);
    private final Deque<Capture> queue=new ArrayDeque<>();
    private volatile Capture active;
    private volatile boolean running;
    private volatile CoachPreferences preferences=CoachPreferences.defaults();
    private volatile boolean showPopups=true;
    private volatile Dimension canvasSize=new Dimension(765,503);
    private CoachToastOverlay popup;
    private boolean initialized;
    private int tick;
    private volatile int generation;
    private CoachImageActions imageActions;
    private long lastTest,nextCapture;
    @Provides MilestoneScreenshotsConfig config(ConfigManager manager){return manager.getConfig(MilestoneScreenshotsConfig.class);}
    @Override protected void startUp(){
        clear();running=true;readPresentationPreferences();
        imageActions=new CoachImageActions(executor,message->clientThread.invoke(()->{if(running)client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","Milestone captures: "+message,null);}));
        popup=new CoachToastOverlay(this,()->preferences,()->showPopups,()->canvasSize,
            id->clientThread.invokeLater(()->cardPainted(id)),this::open);
        overlays.add(popup);mouseManager.registerMouseListener(popup.mouse);
    }
    @Override protected void shutDown(){running=false;clear();if(imageActions!=null){imageActions.close();imageActions=null;}if(popup!=null){overlays.remove(popup);mouseManager.unregisterMouseListener(popup.mouse);popup.dismiss();popup=null;}}
    private void clear(){generation++;levels.clear();queue.clear();active=null;initialized=false;tick=0;nextCapture=0;if(popup!=null)popup.dismiss();}
    private void readPresentationPreferences(){
        CombatCoachConfig settings=configManager.getConfig(CombatCoachConfig.class);
        preferences=new CoachPreferences(settings.premiumTheme(),settings.premiumEffects(),settings.premiumReducedMotion(),settings.premiumHudCompact(),settings.premiumCompactLayout(),config.includeCelebration(),config.saveCard(),config.includeFrame());
        showPopups=settings.premiumPopups();
    }
    @Subscribe public void onGameStateChanged(GameStateChanged e){if(e.getGameState()==GameState.LOGIN_SCREEN||e.getGameState()==GameState.HOPPING)clear();}
    @Subscribe public void onGameTick(GameTick e){
        if(!running||client.getGameState()!=GameState.LOGGED_IN)return;tick++;readPresentationPreferences();
        if(client.getCanvas()!=null)canvasSize=new Dimension(client.getCanvas().getWidth(),client.getCanvas().getHeight());
        if(!initialized){for(Skill skill:Skill.values())if(skill!=Skill.OVERALL)levels.put(skill,client.getRealSkillLevel(skill));initialized=true;}
        long now=System.currentTimeMillis();
        Capture current=active;
        if(current!=null&&current.sequence.timedOut(now)){
            if(current.file!=null)finish(current,"Game screenshot saved, but the optional card-only file was not confirmed.");
            else fail(current,"No complete capture was confirmed. Keep the game visible, then try Test capture again.");
        }
        if(active==null&&now>=nextCapture&&!queue.isEmpty()&&queue.peekFirst().due<=tick){Capture c=queue.removeFirst();if(c.test||config.captureEnabled())begin(c);}
    }
    @Subscribe public void onStatChanged(StatChanged e){
        if(!running||!initialized||client.getGameState()!=GameState.LOGGED_IN||e.getSkill()==Skill.OVERALL)return;
        Skill skill=e.getSkill();int real=client.getRealSkillLevel(skill);Integer before=levels.put(skill,real);
        if(before==null||real<=before||!config.captureEnabled())return;
        Set<Integer> milestones=parseLevels(config.levels());
        for(int level=before+1;level<=real;level++)if(milestones.contains(level)&&queue.size()<20)queue.add(new Capture(skill,level,tick+2,false));
    }
    @Subscribe public void onPluginMessage(PluginMessage e){
        if(!"milestoneScreenshots".equals(e.getNamespace())||!"testCapture".equals(e.getName()))return;
        clientThread.invoke(()->{
            if(!running||client.getGameState()!=GameState.LOGGED_IN)return;long now=System.currentTimeMillis();
            if(now-lastTest<2500||active!=null||!queue.isEmpty())return;lastTest=now;
            Skill skill=Skill.STRENGTH;try{Object value=e.getData().get("skill");if(value!=null)skill=Skill.valueOf(value.toString());}catch(IllegalArgumentException ignored){}
            if(skill!=Skill.OVERALL)queue.add(new Capture(skill,client.getRealSkillLevel(skill),tick+1,true));
        });
    }
    private void begin(Capture c){
        c.epoch=generation;c.include=config.includeCelebration();c.saveCard=config.saveCard();c.includeFrame=config.includeFrame();c.style=preferences;
        c.created=System.currentTimeMillis();c.sequence=new MilestoneCaptureSequence(c.include);c.sequence.present(c.created);active=c;
        popup.prepare(c.record(),c.include);
        if(!c.include)requestGame(c);
    }
    private void cardPainted(String id){
        Capture c=active;if(!running||c==null||!c.id.equals(id)||c.epoch!=generation)return;
        if(c.sequence.painted(System.currentTimeMillis(),true))requestGame(c);
    }
    private void requestGame(Capture c){
        if(active!=c||!c.sequence.request(System.currentTimeMillis()))return;
        popup.capturing();
        try{
            // ImageCapture installs a NEXT-frame listener, after the complete card has already painted twice.
            imageCapture.takeScreenshot("Milestones",c.token,c.includeFrame,false,false);
        }catch(RuntimeException error){fail(c,"Game capture request failed: "+error.getClass().getSimpleName());}
    }
    @Subscribe public void onScreenshotTaken(ScreenshotTaken e){
        Capture c=active;if(e.getPath()==null||c==null||!running)return;
        final Filepath file;
        try{file=CoachScreenshotFiles.resolve(e.getPath().toString());}catch(IllegalArgumentException rejected){return;}
        final boolean cardFile=matches(file,c.token+"-card"),gameFile=matches(file,c.token);
        if(!cardFile&&!gameFile)return;
        executor.execute(()->{
            boolean found;
            try{found=file.isFile()&&file.size()>0;}catch(java.io.IOException error){found=false;}
            final boolean valid=found;
            clientThread.invoke(()->{
                if(!running||active!=c||c.epoch!=generation)return;
                if(!valid){if(c.file==null)fail(c,"The image was not confirmed on disk.");else finish(c,"The game screenshot is saved, but the clean card failed.");return;}
                if(gameFile&&c.sequence.stage()==MilestoneCaptureSequence.Stage.REQUESTED){
                    c.file=file;c.sequence.gameSaved(System.currentTimeMillis());
                    if(c.saveCard)saveCard(c);else finish(c,"");
                }else if(cardFile&&c.sequence.stage()==MilestoneCaptureSequence.Stage.CARD_REQUESTED){c.cardFile=file;finish(c,"");}
            });
        });
    }
    private static boolean matches(Filepath file,String token){String name=file.getFileName();return name.startsWith(token+" ")||name.equals(token+".png");}
    private void saveCard(Capture c){
        if(!c.sequence.requestCard(System.currentTimeMillis()))return;
        executor.execute(()->{
            if(!running||active!=c||c.epoch!=generation)return;
            try{BufferedImage card=CoachMilestoneArt.keepsake(c.record(),c.style);imageCapture.saveScreenshot(card,c.token+"-card","Milestones",false,false);}
            catch(RuntimeException error){clientThread.invoke(()->{if(running&&active==c)finish(c,"Game image saved; clean-card creation failed.");});}
        });
    }
    private void finish(Capture c,String warning){
        if(active!=c||c.file==null)return;c.sequence.complete(System.currentTimeMillis());
        CoachCaptureRecord record=c.record();CoachCaptureHistory.add(configManager,gson,record);popup.saved(record,warning);
        String message="Milestone screenshot saved: "+record.file+(record.cardFile.isEmpty()?"":" | Card PNG: "+record.cardFile);
        if(!warning.isEmpty())message+=" | "+warning;
        client.addChatMessage(ChatMessageType.GAMEMESSAGE,"",message,null);
        if(config.notifyOnScreenshot())try{notifier.notify("Milestone captured: "+c.skill.getName()+" "+c.level+(c.cardFile==null?"":" (game + card)"));}catch(RuntimeException ignored){}
        eventBus.post(new MilestoneCaptureEvent(c.id,c.skill.getName(),c.level,c.file,c.cardFile,c.test,true,c.include,c.created,message));
        executor.execute(()->{try{BufferedImage thumb=CoachThumbnails.load(c.file,190,110);clientThread.invoke(()->{if(running&&popup!=null)popup.setThumbnail(c.id,thumb);});}catch(Exception ignored){}});
        active=null;nextCapture=System.currentTimeMillis()+3500;
    }
    private void fail(Capture c,String error){
        if(active!=c)return;c.sequence.fail(System.currentTimeMillis());popup.failed(c.record(),error);
        client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","Milestone capture: "+error,null);
        eventBus.post(new MilestoneCaptureEvent(c.id,c.skill.getName(),c.level,null,null,c.test,false,false,c.created,error));
        active=null;nextCapture=System.currentTimeMillis()+2500;
    }
    private void open(String action,String path){
        CoachImageActions actions=imageActions;if(actions==null)return;
        if("folder".equals(action))actions.copyFolder(path);else actions.preview(path);
    }
    static Set<Integer> parseLevels(String value){Set<Integer> result=new HashSet<>();if(value==null)return result;for(String part:value.split(","))try{int level=Integer.parseInt(part.trim());if(level>=2&&level<=99)result.add(level);}catch(NumberFormatException ignored){}return result;}
    private static final class Capture {
        final Skill skill;final int level,due;final boolean test;final String id,token;
        boolean include,saveCard,includeFrame;int epoch;long created;Filepath file,cardFile;CoachPreferences style;MilestoneCaptureSequence sequence;
        Capture(Skill skill,int level,int due,boolean test){this.skill=skill;this.level=level;this.due=due;this.test=test;id=UUID.randomUUID().toString();token=(test?"test-":"")+skill.name().toLowerCase(Locale.ROOT)+"-level-"+level+"-"+id.substring(0,8);}
        CoachCaptureRecord record(){return new CoachCaptureRecord(id,skill.getName(),level,test,created,file==null?"":file.toString(),cardFile==null?"":cardFile.toString(),include);}
    }
}
