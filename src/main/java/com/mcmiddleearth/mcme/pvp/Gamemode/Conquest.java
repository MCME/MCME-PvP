package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Jubo
 */
public class Conquest extends BasePluginGamemode {

    private int target = 900; // 15 mins

    private boolean pvpRegistered = false;

    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[]{
            "RedSpawn1",
            "RedSpawn2",
            "RedSpawn3",
            "BlueSpawn1",
            "BlueSpawn2",
            "BlueSpawn3",
            "CapturePoint1",
            "CapturePoint2",
            "CapturePoint3"
    }));

    private int area = 1;

    private GameState state;

    Map map;

    private boolean midgameJoin = true;

    private int count;

    private Objective Points;

    private GamemodeHandlers GMHandlers;

    // TODO:
    //  area can be 1,2 or 3
    //  decides which CapturePoint can be accessed
    //  also decides which spawn is used
    //  reworking capturing with PlayerMoveEvent
    //  Maybe even capture faster when more people in area

    public Conquest(){ state = GameState.IDLE; }

    @Override
    public void Start(Map m, int parameter){
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m,parameter);
        this.map = m;
        target = parameter;

        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }

        if(!pvpRegistered){
            GMHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(GMHandlers,PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        for(Player p:players){
            if (Team.getBlue().size() <= Team.getBlue().size()){
                Team.getRed().add(p);
                p.teleport(m.getImportantPoints().get("Redspawn1").toBukkitLoc().add(0,2,0));
                freezePlayer(p,140);
            }
        }
    }

    public String requiresParameter(){
        return "";
    }

    private class GamemodeHandlers implements Listener {
        private HashMap<String, Location> points = new HashMap<>();
        private HashMap<Location,Integer> capAmount = new HashMap<>();

        public GamemodeHandlers(){

            for(java.util.Map.Entry<String, EventLocation> e: map.getImportantPoints().entrySet()){
                if(e.getKey().contains("Point")){
                    points.put(e.getKey(),e.getValue().toBukkitLoc());
                    capAmount.put(e.getValue().toBukkitLoc(),0);
                }
            }
        }
    }

    @Override
    public ArrayList<String> getNeededPoints() { return NeededPoints; }

    @Override
    public GameState getState(){ return state; }

    public Objective getPoints(){ return Points; }

    @Override
    public boolean isMidgameJoin(){ return midgameJoin; }
}
