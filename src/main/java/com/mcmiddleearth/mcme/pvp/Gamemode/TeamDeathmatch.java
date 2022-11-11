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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Eric
 */
public class TeamDeathmatch extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private boolean pvpRegistered = false;
    
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList("RedSpawn",
            "BlueSpawn"));
    
    private GameState state;
    
    Map map;
    
    private int count;
    private Objective points;
    private GamemodeHandlers TDMHandlers;
    private int startingRedNum;
    private int startingBlueNum;
    
    public TeamDeathmatch(){
        state = GameState.IDLE;
    }
    
    @Override
    public void Start(Map m, int parameter){
        kdSort();
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m, parameter);
        this.map = m;
        
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : Bukkit.getServer().getOnlinePlayers()){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }
        
        if(!pvpRegistered){
            TDMHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(TDMHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        for(Player p : players){
            if(Team.getRed().size() <= Team.getBlue().size()){
                Team.getRed().add(p);
                p.teleport(m.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 1, 0));
                freezePlayer(p, 140);
            }
            else if(Team.getBlue().size() < Team.getRed().size()){
                Team.getBlue().add(p);
                p.teleport(m.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 1, 0));
                freezePlayer(p, 140);
            }
        }
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getBlue().getMembers().contains(player) && !Team.getRed().getMembers().contains(player)){
                Team.getSpectator().add(player);
                player.teleport(m.getSpawn().toBukkitLoc().add(0, 1, 0));
            }
        }
        startingRedNum = Team.getRed().size();
        startingBlueNum = Team.getBlue().size();
            Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
                @Override
                public void run() {
                    if(count == 0){
                        if(state == GameState.RUNNING){
                            return;
                        }

                        points = getScoreboard().registerNewObjective("Remaining", "dummy");
                        points.setDisplayName("Remaining");
                        points.getScore(ChatColor.BLUE + "Blue:").setScore(Team.getBlue().size());
                        points.getScore(ChatColor.RED + "Red:").setScore(Team.getRed().size());
                        points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                            p.setScoreboard(getScoreboard());
                        }
                        
                        for(Player p : Team.getBlue().getMembers()){
                            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
                        }
                        for(Player p : Team.getRed().getMembers()){
                            GearHandler.giveGear(p, ChatColor.RED, SpecialGear.NONE);
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

        for(Player p : Bukkit.getOnlinePlayers()){
            Team.removeFromTeam(p);
        }
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();

        PVPCommand.queueNextGame();
        super.End(m);
    }
    
    public String requiresParameter(){
        return "none";
    }
    
    public boolean isMidgameJoin(){
        if(Team.getRed().size() >= (0.75 * startingRedNum) || Team.getBlue().size() >= (0.75 * startingBlueNum)){
            return true;
        }else{
            return false;
        }
    }
    
    private class GamemodeHandlers implements Listener{
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){

            if(e.getEntity() instanceof Player && state == GameState.RUNNING){
                Player p = e.getEntity();
                
                if(Team.getRed().getMembers().contains(p)){
                    points.getScore(ChatColor.RED + "Red:").setScore(points.getScore(ChatColor.RED + "Red:").getScore() - 1);
                }
                else if(Team.getBlue().getMembers().contains(p)){
                    points.getScore(ChatColor.BLUE + "Blue:").setScore(points.getScore(ChatColor.BLUE + "Blue:").getScore() - 1);
                }
                
                Team.removeFromTeam(p);
                
                if(points.getScore(ChatColor.RED + "Red:").getScore() <= 0){
                
                for(Player player : Bukkit.getServer().getOnlinePlayers()){
                    player.sendMessage(ChatColor.BLUE + "Game over!");
                    player.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    
                }
                PlayerStat.addGameWon(Teams.BLUE);
                PlayerStat.addGameLost(Teams.RED);
                PlayerStat.addGameSpectatedAll();
                End(map);
                }
                else if(points.getScore(ChatColor.BLUE + "Blue:").getScore() <= 0){
                    for(Player player : Bukkit.getServer().getOnlinePlayers()){
                        player.sendMessage(ChatColor.RED + "Game over!");
                        player.sendMessage(ChatColor.RED + "Red Team Wins!");
                    
                    }
                    PlayerStat.addGameWon(Teams.RED);
                    PlayerStat.addGameLost(Teams.BLUE);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                    e.getEntity().teleport(PVPPlugin.getLobby());
                }
                
                if(state == GameState.RUNNING){
                    Team.getSpectator().add(p);
                }
            }
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){

            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                e.setRespawnLocation(map.getSpawn().toBukkitLoc().add(0, 1, 0));
            
                e.getPlayer().getInventory().clear();
                e.getPlayer().getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR),
                   new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
            }
        }
        
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){

            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                
                points.getScore(ChatColor.BLUE + "Blue:").setScore(Team.getBlue().size());
                points.getScore(ChatColor.RED + "Red:").setScore(Team.getRed().size());
                Team.removeFromTeam(e.getPlayer());
                
                if(Team.getRed().size() <= 0){
                
                    for(Player player : Bukkit.getServer().getOnlinePlayers()){
                        player.sendMessage(ChatColor.BLUE + "Game over!");
                        player.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(Team.getBlue().size() <= 0){
                    for(Player player : Bukkit.getServer().getOnlinePlayers()){
                        player.sendMessage(ChatColor.RED + "Game over!");
                        player.sendMessage(ChatColor.RED + "Red Team Wins!");
                    
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
    public boolean midgamePlayerJoin(Player p){
        if(Team.getRed().size() >= (startingRedNum) && Team.getBlue().size() >= (startingBlueNum)){
            
            if(Team.getRed().size() >= Team.getBlue().size()){
                addToTeam(p, Teams.BLUE);
            }
            else if(Team.getBlue().size() > Team.getRed().size()){
                addToTeam(p, Teams.RED);
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
            p.teleport(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 1, 0));
            points.getScore(ChatColor.RED + "Red:").setScore(points.getScore(ChatColor.RED + "Red:").getScore() + 1);
            GearHandler.giveGear(p, ChatColor.RED, SpecialGear.NONE);
        }
        else{
            Team.getBlue().add(p);
            p.teleport(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 1, 0));
            points.getScore(ChatColor.BLUE + "Blue:").setScore(points.getScore(ChatColor.BLUE + "Blue:").getScore() + 1);
            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
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
