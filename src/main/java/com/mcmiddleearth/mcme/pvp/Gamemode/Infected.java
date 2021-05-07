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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Eric
 */
public class Infected extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private boolean pvpRegistered = false;
    
    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[] {
        "InfectedSpawn",
        "SurvivorSpawn",
    }));
    
    private GameState state;
    
    Map map;
    
    private int count;
    
    private Objective Points;
    
    private Gamepvp pvp;
    
    private int time;
    
    public Infected(){
        state = GameState.IDLE;
    }
    
    Runnable tick = new Runnable(){
        @Override
        public void run(){
            time--;
            
            if(time % 60 == 0){
                Points.setDisplayName("Time: " + (time / 60) + "m");
            }else if(time < 60){
                Points.setDisplayName("Time: " + time + "s");
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
            
            if(time == 0){
                String remainingPlayers = "";
                int loopnum = 0;
                for(Player p : Team.getSurvivor().getMembers()){
                    if(Team.getSurvivor().size() > 1 && loopnum == (Team.getSurvivor().size() - 1)){
                
                        remainingPlayers += (", and " + p.getName());
                    }
                    else if(Team.getSurvivor().size() == 1 || loopnum == 0){
                        remainingPlayers += (" " + p.getName());
                    }
                    else{
                        remainingPlayers += (", " + p.getName());
                    }
            
                    loopnum++;
                }
                
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendMessage(ChatColor.BLUE + "Game over!");
                    p.sendMessage(ChatColor.BLUE + "Survivors win!");
                    p.sendMessage(ChatColor.BLUE + "Remaining:" + ChatColor.AQUA + remainingPlayers);
                }
                
                PlayerStat.addGameWon(Teams.SURVIVORS);
                PlayerStat.addGameLost(Teams.INFECTED);
                PlayerStat.addGameSpectatedAll();
                End(map);
            }
        }
    };
    
    @Override
    public void Start(Map m, int parameter){
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m, parameter);
        this.map = m;
        time = parameter;
        
        Random rand = new Random();
        
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }
        
        if(!pvpRegistered){
            pvp = new Gamepvp();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(pvp, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        int c = 0;
        int infected = rand.nextInt(players.size());
        for(Player p : players){
            
            if(c == infected){
                Team.getInfected().add(p);
                p.teleport(m.getImportantPoints().get("InfectedSpawn").toBukkitLoc());
            }
            
            else{
                Team.getSurvivor().add(p);
                p.teleport(m.getImportantPoints().get("SurvivorSpawn").toBukkitLoc());
            }
            
            c++;
        }
        
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getInfected().getMembers().contains(player) && !Team.getSurvivor().getMembers().contains(player)){
                Team.getSpectator().add(player);
                player.teleport(m.getSpawn().toBukkitLoc().add(0, 2, 0));
            }
        }
            Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
                @Override
                public void run() {
                    if(count == 0){
                        if(state == GameState.RUNNING){
                            return;
                        }
                        
                        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), tick, 0, 20);
                        
                        Points = getScoreboard().registerNewObjective("Remaining", "dummy");
                        Points.setDisplayName("Time: " + time + "m");
                        time *= 60;
                        Points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size());
                        Points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size());
                        Points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                        }
                        
                        for(Player p : Team.getSurvivor().getMembers()){
                            p.setScoreboard(getScoreboard());
                            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
                            }
                        for(Player p : Team.getInfected().getMembers()){

                            p.setScoreboard(getScoreboard());
                            GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
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

        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();

        PVPCommand.queueNextGame();
        super.End(m);
    }
    
    public String requiresParameter(){
        return "time in minutes";
    }
    
    public boolean isMidgameJoin(){
        if(time >= 120){
            return true;
        }
        else{
            return false;
        }
    }
    
    private class Gamepvp implements Listener{
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){
            
            if(e.getEntity() instanceof Player && state == GameState.RUNNING){
                Player p = e.getEntity();
                
                if(Team.getSurvivor().getMembers().contains(p)){
                    e.setDeathMessage(ChatColor.BLUE + p.getName() + ChatColor.GRAY + " was infected by " + ChatColor.DARK_RED + p.getKiller().getName());
                    Points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size() - 1);
                    Points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size() + 1);

                    GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
                    Team.getInfected().add(p);
                    if(Team.getSurvivor().size() < 1){

                        for(Player player : Bukkit.getOnlinePlayers()){
                            player.sendMessage(ChatColor.DARK_RED + "Game over!");
                            player.sendMessage(ChatColor.DARK_RED + "Infected Wins!");

                        }
                        PlayerStat.addGameWon(Teams.INFECTED);
                        PlayerStat.addGameLost(Teams.SURVIVORS);
                        PlayerStat.addGameSpectatedAll();
                        End(map);
                    }
                }
            }
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){
            
            final Player p = e.getPlayer();

            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                e.setRespawnLocation(map.getImportantPoints().get("InfectedSpawn").toBukkitLoc().add(0, 2, 0));
                
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), new Runnable(){
                    
                    @Override
                    public void run(){
                     
                        GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
                        Team.getInfected().add(p);
                    }
                    
                }, 40);
            }
        }
        
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){

            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                
                Team.removeFromTeam(e.getPlayer());
                
                Points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size());
                Points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size());
                
                if(Team.getSurvivor().size() <= 0){
                
                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.sendMessage(ChatColor.DARK_RED + "Game over!");
                        player.sendMessage(ChatColor.DARK_RED + "Infected Wins!");
                    
                    }
                    PlayerStat.addGameWon(Teams.INFECTED);
                    PlayerStat.addGameLost(Teams.SURVIVORS);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(Team.getInfected().size() <= 0){
                    
                    String remainingPlayers = "";
                    int loopnum = 0;
                    for(Player p : Team.getSurvivor().getMembers()){
                        if(Team.getSurvivor().size() > 1 && loopnum == (Team.getSurvivor().size() - 1)){
                
                            remainingPlayers += (", and " + p.getName());
                        }
                        else if(Team.getSurvivor().size() == 1 || loopnum == 0){
                            remainingPlayers += (" " + p.getName());
                        }
                        else{
                            remainingPlayers += (", " + p.getName());
                        }
            
                        loopnum++;
                    }
                    
                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.sendMessage(ChatColor.BLUE + "Game over!");
                        player.sendMessage(ChatColor.BLUE + "Survivors Win!");
                        player.sendMessage(ChatColor.BLUE + "Remaining:" + ChatColor.AQUA + remainingPlayers);
                    }
                    PlayerStat.addGameWon(Teams.SURVIVORS);
                    PlayerStat.addGameLost(Teams.INFECTED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
            }
        }
    }
    
    @Override
    public boolean midgamePlayerJoin(Player p){
        if(time >= 120 && (!Team.getInfected().getAllMembers().contains(p) || !Team.getSurvivor().getAllMembers().contains(p))){
            if(Team.getInfected().getAllMembers().contains(p)){
                Team.getInfected().add(p);
                p.teleport(map.getImportantPoints().get("InfectedSpawn").toBukkitLoc().add(0, 2, 0));
                Points.getScore(ChatColor.DARK_RED + "Survivors:").setScore(Team.getInfected().size());
                super.midgamePlayerJoin(p);
                
                GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
                
                return true;
            }
            
            Team.getSurvivor().add(p);
            p.teleport(map.getImportantPoints().get("SurvivorSpawn").toBukkitLoc().add(0, 2, 0));
            Points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size());
            super.midgamePlayerJoin(p);
            
            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
            
            return true;
        }else{
            return false;
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
}
