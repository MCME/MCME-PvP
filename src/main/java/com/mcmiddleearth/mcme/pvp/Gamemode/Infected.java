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

import com.mcmiddleearth.mcme.pvp.Handlers.ActionBarHandler;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
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
    
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList("InfectedSpawn",
            "SurvivorSpawn"));
    
    private GameState state;
    
    Map map;
    
    private int count;
    
    private Objective points;
    
    private GameHandlers INFHandlers;
    
    private int time;
    
    public Infected(){
        state = GameState.IDLE;
    }
    
    Runnable tick = new Runnable(){
        @Override
        public void run(){
            time--;
            
            if(time % 60 == 0){
                points.setDisplayName("Time: " + (time / 60) + "m");
            }else if(time < 60){
                points.setDisplayName("Time: " + time + "s");
            }

            if(time == 120){
                for(Player player: Bukkit.getOnlinePlayers()) {
                    ItemStack COMPASS = new ItemStack(Material.COMPASS, 1);
                    player.getInventory().setItem(3, COMPASS);
                }
                Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), compass, 0, 20);
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
            INFHandlers = new GameHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(INFHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        int c = 0;
        int infected = rand.nextInt(players.size());
        for(Player p : players){
            
            if(c == infected){
                Team.getInfected().add(p);
                p.teleport(m.getImportantPoints().get("InfectedSpawn").toBukkitLoc());
                GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
            }
            
            else{
                Team.getSurvivor().add(p);
                p.teleport(m.getImportantPoints().get("SurvivorSpawn").toBukkitLoc());
                GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
            }
            
            c++;
        }
        
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getInfected().getMembers().contains(player) && !Team.getSurvivor().getMembers().contains(player)){
                Team.getSpectator().add(player);
                player.teleport(m.getMapSpectatorSpawn().toBukkitLoc().add(0, 2, 0));
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

                        if (points == null)
                        points = getScoreboard().registerNewObjective("Remaining", "dummy");
                        points.setDisplayName("Time: " + time + "m");
                        time *= 60;
                        points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size());
                        points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size());
                        points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
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

    /**
     * Sets target of compass for each player from team infected to location of the nearest player of team survivor.
     */
    Runnable compass = new Runnable() {
        @Override
        public void run() {
            for (Player player : Team.getInfected().getMembers()) {
                player.setCompassTarget(getNearest(player).getLocation());
                ActionBarHandler.sendActionBarMessage(player, ChatColor.RED + "" + ChatColor.BOLD + "Tracking survivors!");
            }
        }
    };

    /**
     * Determines the closest player of team survivor to the given player.
     *
     * @param sourcePlayer Represents a player.
     * @return Closest survivor to given player.
     */
    public Player getNearest(Player sourcePlayer) {
        double distance = Double.POSITIVE_INFINITY;
        Player target = null;
        for (Player targetPlayer : Team.getSurvivor().getMembers()) {
            double distanceTo = sourcePlayer.getLocation().distance(targetPlayer.getLocation());
            if (distanceTo > distance)
                continue;
            distance = distanceTo;
            target = targetPlayer;
        }
        return target;
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
    @Override
    public void checkWin(){
        if(Team.getSurvivor().size() < 1) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.DARK_RED + "Game over!");
                player.sendMessage(ChatColor.DARK_RED + "Infected Wins!");
            }
            PlayerStat.addGameWon(Teams.INFECTED);
            PlayerStat.addGameLost(Teams.SURVIVORS);
            PlayerStat.addGameSpectatedAll();
            End(map);
            return;
        }
        if(Team.getInfected().size() < 1){
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(ChatColor.DARK_RED + "Game over!");
                    player.sendMessage(ChatColor.DARK_RED + "Survivor Wins!");
            }
            PlayerStat.addGameWon(Teams.SURVIVORS);
            PlayerStat.addGameLost(Teams.INFECTED);
            PlayerStat.addGameSpectatedAll();
            End(map);
        }
    }
    private class GameHandlers implements Listener{
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){
            
            if(state == GameState.RUNNING){
                Player p = e.getEntity();
                
                if(Team.getSurvivor().getMembers().contains(p)){
                    if(p.getKiller() ==  null){
                        e.setDeathMessage(ChatColor.BLUE + p.getName() + ChatColor.GRAY + " was infected by " + ChatColor.DARK_RED + "stupidity");
                    }
                    else {
                        e.setDeathMessage(ChatColor.BLUE + p.getName() + ChatColor.GRAY + " was infected by " + ChatColor.DARK_RED + p.getKiller().getName());
                    }
                    points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size() - 1);
                    points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size() + 1);

                    GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
                    if(time < 120){
                        ItemStack COMPASS = new ItemStack(Material.COMPASS, 1);
                        p.getInventory().setItem(3, COMPASS);
                    }
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
            final Player player = e.getPlayer();
            if(state == GameState.RUNNING && players.contains(player)){
                e.setRespawnLocation(map.getImportantPoints().get("InfectedSpawn").toBukkitLoc().add(0, 2, 0));
            }
        }
        
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){

            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                
                Team.removeFromTeam(e.getPlayer());
                
                points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size());
                points.getScore(ChatColor.BLUE + "Survivors:").setScore(Team.getSurvivor().size());
                
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
        if(time >= 120){
            Team.getInfected().add(p);
            p.teleport(map.getImportantPoints().get("InfectedSpawn").toBukkitLoc().add(0, 2, 0));
            points.getScore(ChatColor.DARK_RED + "Infected:").setScore(Team.getInfected().size());
            super.midgamePlayerJoin(p);

            GearHandler.giveGear(p, ChatColor.DARK_RED, SpecialGear.INFECTED);
            
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
        return points;
    }
}
