/*
 * This file is part of MCME-pvp.
 * 
 * MCME-pvp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MCME-pvp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MCME-pvp.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */
package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.SpecialGear;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

/**
 *
 * @author Eric
 */
public class FreeForAll extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private boolean pvpRegistered = false;
    
    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[] {
        "PlayerSpawn"
    }));
    
    private GameState state;
    
    Map map;
    
    private int count;
    
    private Objective Points;
    
    private GamemodeHandlers FFAHandlers;
    
    private HashMap<String, String> playerDeaths = new HashMap<String, String>();
    
    private HashMap<String, ChatColor> hasPlayed = new HashMap<String, ChatColor>();
    
    private HashMap<Player, Long> healing = new HashMap<>();
    
    private int time;
    
    private boolean midgameJoin = true;
    
    private final ChatColor[] chatColors = new ChatColor[]{
            ChatColor.AQUA,
            ChatColor.BLUE,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_PURPLE,
            ChatColor.DARK_RED,
            ChatColor.GOLD,
            ChatColor.GREEN,
            ChatColor.LIGHT_PURPLE,
            ChatColor.RED,
            ChatColor.YELLOW       
    };
    
    private EventLocation[] spawns;
    
    public FreeForAll(){
        state = GameState.IDLE;
    }

    public void timer(){
        new BukkitRunnable(){
            @Override
            public void run() {
                time--;

                if(time < 60 ){
                    Points.setDisplayName("Time: "+ time + "s");
                }else{
                    Points.setDisplayName("Time: "+(time / 60) + "m "+time%60+"s");
                }

                if(time == 30){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + "30 seconds remaining!");
                    }

                }
                else if(time <= 10 && time > 1){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " seconds remaining!");
                    }

                }
                else if(time == 1){

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.GREEN + String.valueOf(time) + " second remaining!");
                    }

                }

                if(time <= 0){
                    End(map);
                }

                boolean healed = false;

                for(Player p : healing.keySet()){

                    if(System.currentTimeMillis() < healing.get(p)){
                        p.setHealth(20);
                        healed = true;
                    }

                }
                if(!healed){
                    healing.clear();
                }
            }
        }.runTaskTimer(PVPPlugin.getPlugin(),0, 20);
    }
    
    @Override
    public void Start(Map m, int parameter){
        super.Start(m, parameter);
        map = m;
        count = PVPPlugin.getCountdownTime();
        time = parameter;
        state = GameState.COUNTDOWN;
        List<EventLocation> spawnsTemp = new ArrayList<>();
        spawnsTemp.add(map.getImportantPoints().get("PlayerSpawn"));
        for(int i = 1;i < map.getImportantPoints().size();i++){
            spawnsTemp.add(map.getImportantPoints().get("PlayerSpawn"+i));
        }
        spawns = spawnsTemp.toArray(new EventLocation[0]);
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }
        
        if(!pvpRegistered){
            FFAHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(FFAHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        int c = 0;
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(players.contains(p)){
                p.teleport(spawns[c].toBukkitLoc().add(0, 1, 0));
                freezePlayer(p, 140);
                if(spawns.length == (c + 1)){
                    c = 0;
                }
                else{
                    c++;
                }
            }
            else{
                Team.getSpectator().add(p);
                p.teleport(map.getSpawn().toBukkitLoc());
            }
            
        }
        
        new BukkitRunnable(){
                @Override
                public void run() {

                    if(count == 0){
                        if(state == GameState.RUNNING){
                            cancel();
                        }
                        state = GameState.RUNNING;
                        int k = 0;
                        Points = getScoreboard().registerNewObjective("Kills", "dummy", "Time: " + time + "m");
                        time *= 60;
                        Points.setDisplaySlot(DisplaySlot.SIDEBAR);

                        FFAHandlers.spawn = new Random().nextInt(spawns.length-1);

                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                            p.setScoreboard(getScoreboard());
                        }

                        for(Player p : players){

                            p.setGameMode(GameMode.ADVENTURE);

                            ChatHandler.getPlayerPrefixes().put(p.getName(), chatColors[k] + "Player");
                            ChatHandler.getPlayerColors().put(p.getName(), chatColors[k]);
                            hasPlayed.put(p.getName(), chatColors[k]);

                            Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);

                            if(p.getName().length() < 14){
                                p.setPlayerListName(chatColors[k] + p.getName());
                            }
                            else{
                                String newName = p.getName().substring(0,13);
                                p.setPlayerListName(chatColors[k] + newName);
                            }
                            GearHandler.giveGear(p, chatColors[k], SpecialGear.NONE);
                            BukkitTeamHandler.addToBukkitTeam(p, chatColors[k]);

                            if(chatColors.length == (k+1)){
                                k = 0;
                            }
                            else{
                                k++;
                            }
                        }
                        count = -1;

                        for(Player p : players){
                            p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/unstuck" + ChatColor.GRAY + " if you're stuck in a block!");
                        }
                        timer();
                    }
                    else if(count != -1){
                        for(Player p : Bukkit.getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game begins in " + count);
                        }
                        count--;
                    }
                    else{
                        cancel();
                    }
                }
            }.runTaskTimer(PVPPlugin.getPlugin(),40, 20);
    }
    
    public void End(Map m){
        PlayerStat.addGameSpectatedAll();
        state = GameState.IDLE;
        hasPlayed.clear();

        ArrayList<String> killMessages = new ArrayList<>();
        for(java.util.Map.Entry<Player, Integer> player : getTopKillsMap().entrySet()){
            killMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " with " + player.getValue() + " kills!");
        }
        ArrayList<String> deathMessages = new ArrayList<>();
        for(java.util.Map.Entry<Player, Integer> player : getTopDeathsMap().entrySet()){
            deathMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }
        ArrayList<String> KDMessages = new ArrayList<>();
        for(java.util.Map.Entry<Player, Integer> player : getTopKDMap().entrySet()){
            KDMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.GREEN + "Winner: ");
            player.sendMessage(killMessages.get(0));
            player.sendMessage(ChatColor.GREEN + "Highest KD: ");
            for (String message: KDMessages) {
                player.sendMessage(message);
            }
            player.sendMessage(ChatColor.GREEN + "Most Deaths: ");
            for (String message: deathMessages) {
                player.sendMessage(message);
            }
        }
        
        for(Player p : Bukkit.getOnlinePlayers()){
            BukkitTeamHandler.removeFromBukkitTeam(p);
        }
        
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        playerDeaths.clear();
        
        PVPCommand.queueNextGame();
        super.End(m);
    }
    
    @Override
    public boolean midgamePlayerJoin(Player p){
        Random random = new Random();
        ChatColor color;
        Team.removeFromTeam(p);
        if(!hasPlayed.containsKey(p.getName())){
            color = chatColors[random.nextInt(chatColors.length)];
            ChatHandler.getPlayerColors().put(p.getName(), color);
            ChatHandler.getPlayerPrefixes().put(p.getName(), color + "Player");
            if (state == GameState.RUNNING) Points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);
            hasPlayed.put(p.getName(), color);
            
        }
        else{
            color = hasPlayed.get(p.getName());
            ChatHandler.getPlayerColors().put(p.getName(), color);
        }
        
        if(p.getName().length() < 14){
            p.setPlayerListName(color + p.getName());
        }
        else{
            String newName = p.getName().substring(0,13);
            p.setPlayerListName(color + newName);
        }
        
        p.teleport(spawns[FFAHandlers.spawn++].toBukkitLoc().add(0, 1, 0));
        p.setGameMode(GameMode.ADVENTURE);
        p.setScoreboard(getScoreboard());
        
        GearHandler.giveGear(p, color, SpecialGear.NONE);
        BukkitTeamHandler.addToBukkitTeam(p, color);
        
        super.midgamePlayerJoin(p);
        
        return true;
    }
    
    public String requiresParameter(){
        return "time in minutes";
    }
    
    private class GamemodeHandlers implements Listener{

        int spawn = 0;
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){

            if(e.getEntity() instanceof Player && e.getEntity().getKiller() != null && state == GameState.RUNNING){
               
                    if(e.getEntity().getKiller() instanceof Player){
                        int tempDeaths;
                        Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).setScore(Points.getScore(ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName()).getScore() + 1);

                        if(playerDeaths.containsKey(e.getEntity().getName())){
                            tempDeaths = Integer.parseInt(playerDeaths.get(e.getEntity().getName()));
                            playerDeaths.remove(e.getEntity().getName());
                            playerDeaths.put(e.getEntity().getName(), String.valueOf(tempDeaths + 1));
                        }
                        else{
                            playerDeaths.put(e.getEntity().getName(), "1");
                        }
                }
            }
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){

            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                /*
                Random random = new Random();

                e.setRespawnLocation(spawns[random.nextInt(spawns.length)].toBukkitLoc().add(0, 2, 0));

                 */
                e.setRespawnLocation(spawns[spawn++].toBukkitLoc().add(0, 1, 0));

                if(spawn >= spawns.length){
                    spawn = 0;
                }
            
                //healing.put(e.getPlayer(), System.currentTimeMillis() + 7500);
            }
            Logger.getLogger("PVP").log(Level.INFO, e.getRespawnLocation().toString());
        }
    }

    @Override
    public ArrayList<String> getNeededPoints() {
        return NeededPoints;
    }

    @Override
    public GameState getState() {
        return state;
    }

    public Objective getPoints() {
        return Points;
    }

    @Override
    public boolean isMidgameJoin() {
        return midgameJoin;
    }
}
