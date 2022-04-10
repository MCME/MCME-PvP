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
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.SpecialGear;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVP.Team.Teams;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */

public class TeamConquest extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {//Handled by plugin, should be done needs testing
    
    private Objective Points;
    
    private final int target = 100;
    
    private final int midgameJoinPointThreshold = 20;
    
    private final int giveTntPointThreshold = 60;
    
    private boolean givenTnt = false;
    
    private boolean pvpRegistered = false;
    
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList(new String[]{
            "RedSpawn",
            "BlueSpawn",
            "CapturePoint1",
            "CapturePoint2",
            "CapturePoint3"
    }));
    
    Map map;
    
    private int count;
    
    private boolean midgameJoin = true;
    
    private GameState state;
    
    private GamemodeHandlers TCHandlers;
    
    private int goal;
    
    public TeamConquest(){
        state = GameState.IDLE;
    }
    
    @Override
    public void Start(Map m, int parameter) {
        kdSort();
        super.Start(m, parameter);
        goal = parameter;
        givenTnt = false;
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        this.map = m;
        if(!m.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game Cannot Start! Not all needed points have been added!");
            }
            End(m);
            return;
        }
        
        if(!pvpRegistered){
            TCHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(TCHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        for(Location l : TCHandlers.points){
            l.getBlock().setType(Material.BEACON);
            
            l.getBlock().getRelative(0, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(0, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(0, -1, 1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(1, -1, 1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, -1).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, 0).setType(Material.IRON_BLOCK);
            l.getBlock().getRelative(-1, -1, 1).setType(Material.IRON_BLOCK);
        }
        
        for(Player p : Bukkit.getOnlinePlayers()){
            if(players.contains(p)){
                if(Team.getBlue().size() >= Team.getRed().size()){
                    Team.getRed().add(p);
                    p.teleport(m.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
                }else if(Team.getBlue().size() < Team.getRed().size()){
                    Team.getBlue().add(p);
                    p.teleport(m.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
                }
            }else{
                Team.getSpectator().add(p);
                p.teleport(m.getMapSpectatorSpawn().toBukkitLoc().add(0, 2, 0));
            }
        }
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
                @Override
                public void run() {
                    if(count == 0){
                        if(state == GameState.RUNNING){
                            return;
                        }
                        
                        Points = getScoreboard().registerNewObjective("Score", "dummy");
                        Points.setDisplayName("Score");
                        Points.getScore(ChatColor.BLUE + "Blue:").setScore(0);
                        Points.getScore(ChatColor.RED + "Red:").setScore(0);
                        Points.getScore(ChatColor.WHITE + "Goal:").setScore(goal);
                        Points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                            p.setScoreboard(getScoreboard());
                        }
                        
                        for(Player p : Team.getRed().getMembers()){
                            GearHandler.giveGear(p, ChatColor.RED, SpecialGear.NONE);
                        }
                        for(Player p : Team.getBlue().getMembers()){
                            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
                        }
                        state = GameState.RUNNING;
                        count = -1;
                        
                        for(Player p : players){
                            p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/unstuck" + ChatColor.GRAY + " if you're stuck in a block!");
                        }
                    }
                    else if(count != -1){
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game begins in " + count);
                        }
                        count--;
                    }
                }

            }, 40, 20);
    }
    
    
    
    @Override
    public void End(Map m){
        state = GameState.IDLE;
        
        for(Location l : TCHandlers.points){
            l.getBlock().setType(Material.AIR);
            l.getBlock().getRelative(0, 1, 0).setType(Material.AIR);
        }
        
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);

    }
    
    public String requiresParameter(){
        return "point goal";
    }
    
    private class GamemodeHandlers implements Listener{
        
        private ArrayList<Location> points = new ArrayList<>();
        
        HashMap<Location, Integer> capAmount = new HashMap<>();//red = +; blue = -
        
        public GamemodeHandlers(){
            for(Entry<String, EventLocation> e : map.getImportantPoints().entrySet()){
                if(e.getKey().contains("Point")){
                    points.add(e.getValue().toBukkitLoc());
                    capAmount.put(e.getValue().toBukkitLoc(), 0);
                }
            }
        }
        
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent e){
            if(state == GameState.RUNNING && players.contains(e.getPlayer()) &&
                    e.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
                if(e.getClickedBlock().getType().equals(Material.BEACON)){
                    e.setUseInteractedBlock(Event.Result.DENY);
                    int cap = capAmount.get(e.getClickedBlock().getLocation());
                    Player p = e.getPlayer();
                    if(Team.getRed().getMembers().contains(p)){
                        if(cap == 0){
                            p.sendMessage(ChatColor.GRAY + "Point is neutral!");
                                
                            Block b = e.getClickedBlock().getLocation().add(0, 1, 0).getBlock();
                            b.setType(Material.AIR);
                             
                            if(Team.getBlue().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                Team.getBlue().getCapturedPoints().remove(e.getClickedBlock().getLocation());
                            }
                        }
                        
                        if(cap < 50){
                            cap++;
                            p.sendMessage(ChatColor.RED + "Cap at " + (cap * 2) + "%");
                            
                            if(cap >= 50){
                                p.sendMessage(ChatColor.RED + "Point Captured!");
                                if(!Team.getRed().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                    Team.getRed().getCapturedPoints().add(e.getClickedBlock().getLocation());
                                    Block b = e.getClickedBlock().getLocation().add(0, 1, 0).getBlock();
                                    b.setType(Material.RED_STAINED_GLASS);
                                    for(Player pl : players){
                                        pl.sendMessage(ChatColor.RED + "Red Team captured a point!");
                                    }
                                }
                                if(Team.getBlue().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                    Team.getBlue().getCapturedPoints().remove(e.getClickedBlock().getLocation());
                                }
                            }else{
                                capAmount.put(e.getClickedBlock().getLocation(), cap);
                            }
                        }
                    }else if(Team.getBlue().getMembers().contains(p)){
                        if(cap == 0){
                            p.sendMessage(ChatColor.GRAY + "Point is neutral!");
                                
                            Block b = e.getClickedBlock().getLocation().add(0, 1, 0).getBlock();
                            b.setType(Material.AIR);
                              
                            if(Team.getRed().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                Team.getRed().getCapturedPoints().remove(e.getClickedBlock().getLocation());
                            }
                        }
                        
                        if(cap > -50){
                            cap--;
                            p.sendMessage(ChatColor.BLUE + "Cap at " + (cap * -2) + "%");
                            if(cap <= -50){
                                p.sendMessage(ChatColor.BLUE + "Point Captured!");
                                if(!Team.getBlue().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                    Team.getBlue().getCapturedPoints().add(e.getClickedBlock().getLocation());
                                    Block b = e.getClickedBlock().getLocation().add(0, 1, 0).getBlock();
                                    b.setType(Material.BLUE_STAINED_GLASS);
                                    for(Player pl : players){
                                        pl.sendMessage(ChatColor.BLUE + "Blue Team captured a point!");
                                    }
                                }
                                if(Team.getRed().getCapturedPoints().contains(e.getClickedBlock().getLocation())){
                                    Team.getRed().getCapturedPoints().remove(e.getClickedBlock().getLocation());
                                }
                            }else{
                                capAmount.put(e.getClickedBlock().getLocation(), cap);
                            }
                        }
                    }
                }
            }
        }
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){

            if(state == GameState.RUNNING && e.getEntity() instanceof Player){
                
                if(Team.getRed().getMembers().contains(e.getEntity())){
                    
                    int pointModifier = Team.getBlue().getCapturedPoints().size() - Team.getRed().getCapturedPoints().size();
                    
                    if(pointModifier > 0){
                        Points.getScore(ChatColor.BLUE + "Blue:").setScore(Points.getScore(ChatColor.BLUE + "Blue:").getScore() + pointModifier);
                    }
                    
                }
                else if(Team.getBlue().getMembers().contains(e.getEntity())){
                    
                    int pointModifier = Team.getRed().getCapturedPoints().size() - Team.getBlue().getCapturedPoints().size();
                    
                    if(pointModifier > 0){
                        Points.getScore(ChatColor.RED + "Red:").setScore(Points.getScore(ChatColor.RED + "Red:").getScore() + pointModifier);
                    }
                    
                }
                
                if(PVPCommand.getRunningGame().getTitle().equals("Helms_Deep") &&
                        Points.getScore(ChatColor.RED + "Red:").getScore() >= giveTntPointThreshold &&
                        !givenTnt){
                    Random r = new Random();
                    
                    Player randomPlayer = (Player) Team.getRed().getMembers().toArray()[r.nextInt(Team.getRed().getMembers().size())];
                    
                    GearHandler.giveCustomItem(randomPlayer, GearHandler.CustomItem.TNT);
                    givenTnt = true;
                    
                }
                
                if(Points.getScore(ChatColor.BLUE + "Blue:").getScore() >= goal){
                    
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.BLUE + "Game over!");
                        p.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(Points.getScore(ChatColor.RED + "Red:").getScore() >= goal){
                    
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.RED + "Game over!");
                        p.sendMessage(ChatColor.RED + "Red Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.RED);
                    PlayerStat.addGameLost(Teams.BLUE);
                    PlayerStat.addGameSpectatedAll();
                    PVPCommand.queueNextGame();
                    End(map);
                    
                }
                
            }
            
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){

            if(state == GameState.RUNNING && Team.getRed().getMembers().contains(e.getPlayer())) {
                e.setRespawnLocation(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
            }
            if(state == GameState.RUNNING && Team.getBlue().getMembers().contains(e.getPlayer())){
                e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
            }
        }
        
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){
            
            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                
                Team.removeFromTeam(e.getPlayer());
                
                if(Team.getRed().size() <= 0){
                    
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.BLUE + "Game over!");
                        p.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(Team.getBlue().size() <= 0){
                    
                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.RED + "Game over!");
                        p.sendMessage(ChatColor.RED + "Red Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.RED);
                    PlayerStat.addGameLost(Teams.BLUE);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                    
                }
            }
            
        }
    }
    
    @Override
    public boolean midgamePlayerJoin (Player p){
        
        if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
            
            if(Team.getRed().getAllMembers().contains(p)){
                addToTeam(p, Teams.RED);
            }
            else if(Team.getBlue().getAllMembers().contains(p)){
                addToTeam(p, Teams.BLUE);
            }

            else{
                if(Team.getRed().size() >= Team.getBlue().size()){
                    addToTeam(p, Teams.BLUE);
                }
                else{
                    addToTeam(p, Teams.RED);
                }
            }
            super.midgamePlayerJoin(p);
            return true;
        }
        else{
            return false;
        }
        
    }
    
    private void addToTeam(Player p, Teams t){
        if(t == Teams.RED){
            Team.getRed().add(p);  
            p.teleport(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
            GearHandler.giveGear(p, ChatColor.RED, SpecialGear.NONE);
        }
        else{
            Team.getBlue().add(p);  
            p.teleport(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
        }
    }

    public Objective getPoints() {
        return Points;
    }

    public int getTarget() {
        return target;
    }

    @Override
    public ArrayList<String> getNeededPoints() {
        return NeededPoints;
    }

    @Override
    public boolean isMidgameJoin() {
        return midgameJoin;
    }

    @Override
    public GameState getState() {
        return state;
    }
}
