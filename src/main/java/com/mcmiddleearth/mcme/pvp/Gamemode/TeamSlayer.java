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
 * @author Donovan <dallen@dallen.xyz>
 */
public class TeamSlayer extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private int target;
    
    private boolean pvpRegistered = false;
    
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList(
            "RedSpawn1",
            "RedSpawn2",
            "RedSpawn3",
            "BlueSpawn1",
            "BlueSpawn2",
            "BlueSpawn3"
    ));
    
    
    private GameState state;
    
    private final int midgameJoinPointThreshold = 10;
    
    private final int giveTntPointThreshold = 1;//30
    
    private boolean givenTnt = false;
    
    Map map;
    
    private int count;
    
    private Objective points;
    
    private GamemodeHandlers TSHandlers;

    private boolean midgameJoin = true;

    public TeamSlayer(){
        state = GameState.IDLE;
    }
    
    @Override
    public void Start(Map m, int parameter){

    	kdSort();
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        givenTnt = false;
        int lastRedSpawn = 3;
        int lastBlueSpawn = 3;
        super.Start(m, parameter);
        this.map = m;
        target = parameter;
        
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }
        
        if(!pvpRegistered){
            TSHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(TSHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        for(Player p : players){
            if(Team.getRed().size() <= Team.getBlue().size()){
                Team.getRed().add(p);
                switch(lastRedSpawn){
                    case 1:
                        p.teleport(m.getImportantPoints().get("RedSpawn2").toBukkitLoc().add(0, 2, 0));
                        lastRedSpawn = 2;
                        break;
                    case 2:
                        p.teleport(m.getImportantPoints().get("RedSpawn3").toBukkitLoc().add(0, 2, 0));
                        lastRedSpawn = 3;
                        break;
                    case 3:
                        p.teleport(m.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
                        lastRedSpawn = 1;
                        break;
                    default:
                        p.teleport(m.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
                        lastRedSpawn = 1;
                        break;
                }
            }
            else if(Team.getBlue().size() < Team.getRed().size()){
                Team.getBlue().add(p);
                switch(lastBlueSpawn){
                    case 1:
                        p.teleport(m.getImportantPoints().get("BlueSpawn2").toBukkitLoc().add(0, 2, 0));
                        lastBlueSpawn = 2;
                        break;
                    case 2:
                        p.teleport(m.getImportantPoints().get("BlueSpawn3").toBukkitLoc().add(0, 2, 0));
                        lastBlueSpawn = 3;
                        break;
                    case 3:
                        p.teleport(m.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
                        lastBlueSpawn = 1;
                        break;
                    default:
                        p.teleport(m.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
                        lastBlueSpawn = 1;
                        break;
                }
            }
        }
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getBlue().getMembers().contains(player) && !Team.getRed().getMembers().contains(player)){
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

                        points = getScoreboard().registerNewObjective("Score", "dummy");
                        points.setDisplayName("Score");
                        points.getScore(ChatColor.WHITE + "Goal:").setScore(target);
                        points.getScore(ChatColor.BLUE + "Blue:").setScore(0);
                        points.getScore(ChatColor.RED + "Red:").setScore(0);
                        points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
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
    
    public void End(Map m){
        state = GameState.IDLE;
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);
    }
    
    @Override
    public void playerLeave(Player p){
        Team.removeFromTeam(p);
    }
    
    public boolean midgamePlayerJoin(Player p){
        
        if(Team.getRed().getAllMembers().contains(p)){
            addToTeam(p, Teams.RED);
        }
        else if(Team.getBlue().getAllMembers().contains(p)){
            addToTeam(p, Teams.BLUE);
        }
        else if(points.getScore(ChatColor.RED + "Red:").getScore() - points.getScore(ChatColor.BLUE + "Blue:").getScore() >= midgameJoinPointThreshold){
            addToTeam(p, Teams.BLUE);
        }
        else if(points.getScore(ChatColor.BLUE + "Blue:").getScore() - points.getScore(ChatColor.RED + "Red:").getScore() >= midgameJoinPointThreshold){
            addToTeam(p, Teams.RED);
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
    
    private void addToTeam(Player p, Teams t){
        if(t == Teams.RED){
            Team.getRed().add(p);
            p.teleport(map.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
            GearHandler.giveGear(p, ChatColor.RED, SpecialGear.NONE);
        }
        else{
            Team.getBlue().add(p);
            p.teleport(map.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
            GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.NONE);
        }
    }
    
    public String requiresParameter(){
        return "end kill number";
    }
    
    private class GamemodeHandlers implements Listener{

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent playerDeathEvent){
            if(state == GameState.RUNNING){
                int redScore = points.getScore(ChatColor.RED + "Red:").getScore();
                int blueScore = points.getScore(ChatColor.BLUE + "Blue:").getScore();
                Player player = playerDeathEvent.getEntity();
                    if(Team.getRed().getMembers().contains(player)){
                        points.getScore(ChatColor.BLUE + "Blue:").setScore(blueScore + 1);
                    }
                    if(Team.getBlue().getMembers().contains(player)){
                        points.getScore(ChatColor.RED + "Red:").setScore(redScore + 1);
                    }
            
                if(PVPCommand.getRunningGame().getTitle().equals("Helms_Deep") &&
                        points.getScore(ChatColor.RED + "Red:").getScore() >= giveTntPointThreshold &&
                        !givenTnt){
                    Random r = new Random();
                    Player randomPlayer = (Player) Team.getRed().getMembers().toArray()[r.nextInt(Team.getRed().getMembers().size())];
                    GearHandler.giveCustomItem(randomPlayer, GearHandler.CustomItem.TNT);
                    givenTnt = true;
                }
                if(points.getScore(ChatColor.RED + "Red:").getScore() >= target){
                    for(Player players : Bukkit.getOnlinePlayers()){
                        players.sendMessage(ChatColor.RED + "Game over!");
                        players.sendMessage(ChatColor.RED + "Red Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.RED);
                    PlayerStat.addGameLost(Teams.BLUE);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(points.getScore(ChatColor.BLUE + "Blue:").getScore() >= target){
                    for(Player players : Bukkit.getOnlinePlayers()){
                        players.sendMessage(ChatColor.BLUE + "Game over!");
                        players.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
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
            if(Team.getBlue().size() <= 0){
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
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){
            Random random = new Random();
            int spawn = random.nextInt(3) + 1;
            if(state == GameState.RUNNING && Team.getRed().getMembers().contains(e.getPlayer())){
                switch(spawn){
                    case 1:
                        e.setRespawnLocation(map.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
                        break;
                        case 2:
                            e.setRespawnLocation(map.getImportantPoints().get("RedSpawn2").toBukkitLoc().add(0, 2, 0));
                            break;
                        case 3:
                            e.setRespawnLocation(map.getImportantPoints().get("RedSpawn3").toBukkitLoc().add(0, 2, 0));
                            break;
                }
            }
                if(state == GameState.RUNNING && Team.getBlue().getMembers().contains(e.getPlayer())){
                    switch(spawn){
                        case 1:
                            e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
                            break;
                        case 2:
                            e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn2").toBukkitLoc().add(0, 2, 0));
                            break;
                        case 3:
                            e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn3").toBukkitLoc().add(0, 2, 0));
                            break;
                    }
                }
        }
    }

    @Override
    public ArrayList<String> getNeededPoints() {
        return new ArrayList<>(this.NeededPoints);
    }

    @Override
    public GameState getState() {
        return state;
    }

    public Objective getPoints() {
        return points;
    }

    @Override
    public boolean isMidgameJoin() {
        return midgameJoin;
    }
}
