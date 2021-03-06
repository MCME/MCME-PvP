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
 * m
 * You should have received a copy of the GNU General Public License
 * along with MCME-pvp.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */
package com.mcmiddleearth.mcme.pvp.Gamemode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Handlers.ArrowHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.Gamemode.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;

/**
 *
 * @author donoa_000
 */
public abstract class BasePluginGamemode implements com.mcmiddleearth.mcme.pvp.Gamemode.Gamemode {

    @JsonIgnore
    ArrayList<Player> players = new ArrayList<>();
    public enum GameState {
        IDLE, COUNTDOWN, RUNNING
    }

    
    private static Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    
    public void playerLeave(Player p){
        players.remove(p);
    }
    
    @Override
    public void Start(Map m, int parameter){
        PVPCommand.toggleVoxel("true");
        
        ArrowHandler.initializeArrowHandling();
        
        for(Player p : players){
            PlayerStat.getPlayerStats().get(p.getName()).addPlayedGame();
        }
        /*if(m.getResourcePackURL() != null){
            for(Player p : Bukkit.getOnlinePlayers()){
                if(!players.contains(p)){
                    try{
                        p.setResourcePack(m.getResourcePackURL());
                    }
                    catch(NullPointerException e){
                        p.sendMessage(ChatColor.RED + "No resource pack was set for this map!");
                    }
                }
            }
        }*/
        
    }
    
    @Override
    public void End(Map m){
        PVPCommand.setRunningGame(null);
        PVPCommand.toggleVoxel("false");
        
        /*for(Player p : Bukkit.getOnlinePlayers()){
            p.setResourcePack("http://www.mcmiddleearth.com/content/Eriador.zip");
        }*/
        
        Team.resetAllTeams();
        
        Bukkit.getScheduler().cancelTasks(PVPPlugin.getPlugin());
        for(Objective o : scoreboard.getObjectives()){
            o.unregister();
        }
        
        ChatHandler.getPlayerColors().clear();
        
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            BukkitTeamHandler.removeFromBukkitTeam(p);
            ChatHandler.getPlayerColors().put(p.getName(), ChatColor.WHITE);
            p.teleport(PVPPlugin.getSpawn());
            p.setDisplayName(ChatColor.WHITE + p.getName());
            p.setPlayerListName(ChatColor.WHITE + p.getName());
            p.getInventory().clear();
            p.setTotalExperience(0);
            p.setExp(0);
            p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR),
            new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
            p.setGameMode(GameMode.ADVENTURE);
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            ChatHandler.getPlayerPrefixes().remove(p.getName());
            
            if(!p.isDead()){
                p.setHealth(20);
            }
        }
        for(Arrow arrow : Bukkit.getWorld(m.getSpawn().getWorld()).getEntitiesByClass(Arrow.class)) {
            arrow.remove();
        }
    }
    
    
    public boolean midgamePlayerJoin(Player p){
        PlayerStat.getPlayerStats().get(p.getName()).addPlayedGame();
        String message = "";
        
        if(PVPCommand.getRunningGame() == null){
            return false;
        }
        
        if(PVPCommand.getRunningGame().getGm() instanceof com.mcmiddleearth.mcme.pvp.Gamemode.FreeForAll ||
                PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver){
            
            message = ChatHandler.getPlayerColors().get(p.getName()) + p.getName() + ChatColor.GRAY + " has joined the fight!";
            
        }
        
        else if(PVPCommand.getRunningGame().getGm() instanceof Ringbearer ||
                PVPCommand.getRunningGame().getGm() instanceof TeamConquest ||
                PVPCommand.getRunningGame().getGm() instanceof TeamDeathmatch ||
                PVPCommand.getRunningGame().getGm() instanceof TeamSlayer){
            
             if(Team.getRed().getMembers().contains(p)){
                 message = ChatColor.RED + p.getName() + ChatColor.GRAY + " has joined the fight on " + ChatColor.RED + "Red Team!";
             }
             else if(Team.getBlue().getMembers().contains(p)){
                 message = ChatColor.BLUE + p.getName() + ChatColor.GRAY + " has joined the fight on " + ChatColor.BLUE + "Blue Team!";
             }
            
        }
        
        else if(PVPCommand.getRunningGame().getGm() instanceof Infected){
            
            message = ChatColor.BLUE + p.getName() + ChatColor.GRAY + " has joined the fight as a " + ChatColor.BLUE + "Survivor!";
            
        }
        for(Player pl : Bukkit.getOnlinePlayers()){
            pl.sendMessage(message);
        }
        
        return true;
    }

    @Override
    public ArrayList<Player> getPlayers() {
        return players;
    }

    public static Scoreboard getScoreboard() {
        return scoreboard;
    }
}
