package com.osrs.combatcoach;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.config.ConfigManager;

/** Eight recent captures in the existing RuneLite config profile. No screenshot bytes in config. */
public final class CoachCaptureHistory {
    private CoachCaptureHistory(){}
    public static List<CoachCaptureRecord> load(ConfigManager config,Gson gson){
        String raw=config.getConfiguration("milestoneScreenshots","premiumCaptureHistoryV4");
        List<CoachCaptureRecord> result=new ArrayList<>();
        try{
            if(raw!=null&&raw.length()<131072){CoachCaptureRecord[] entries=gson.fromJson(raw,CoachCaptureRecord[].class);
                if(entries!=null)for(CoachCaptureRecord r:entries){if(r==null||r.id==null||r.skill==null||r.file==null||r.file.isEmpty()||r.cardFile==null)continue;result.add(r);if(result.size()==8)break;}}
        }catch(RuntimeException ignored){/* An invalid history must never block gameplay or captures. */}
        return result;
    }
    public static List<CoachCaptureRecord> add(ConfigManager config,Gson gson,CoachCaptureRecord record){
        List<CoachCaptureRecord> next=new ArrayList<>();next.add(record);
        for(CoachCaptureRecord r:load(config,gson))if(!r.id.equals(record.id)&&next.size()<8)next.add(r);
        config.setConfiguration("milestoneScreenshots","premiumCaptureHistoryV4",gson.toJson(next));return Collections.unmodifiableList(next);
    }
}
