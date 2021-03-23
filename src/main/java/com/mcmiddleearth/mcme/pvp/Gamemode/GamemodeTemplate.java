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

public class GamemodeTemplate extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {

    /*
    This is a template to be used for future PVP gamemode development.
    Make sure to add appropriate references in PVPPlugin and PVPCommands after finishing your gamemode.
     */

    //for this example teams are Red and Blue, but feel free to create your own team names for different gamemodes

    private int target;//points or time or other condition needed to end the game

    private boolean pvpRegistered = false;

    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[] {
            "RedSpawn1",
            "BlueSpawn1",
    }));//spawns, write in this template
    //for adding more spawns, follow the same template "RedSpawn3"e.g.
    //After that, wherever you use them make a switch/case loop and cycle through all the spawns

    private GameState state;

    Map map;

    private int count;

    private Objective Points;

    private Gamepvp pvp;

    private boolean midgameJoin = true;

    //setting variables for later use
    //also map/gamemode specific variables are set here

    public GamemodeTemplate(){
        state = GameState.IDLE;
    }

    @Override
    public void Start(Map m, int parameter){
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        super.Start(m, parameter);
        this.map = m;
        target = parameter;

        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }//if not all points are set this error pops up

        if(!pvpRegistered){
            pvp = new Gamepvp();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(pvp, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        for(Player p : players) {//this distributes players evenly across teams
            if (Team.getRed().size() <= Team.getBlue().size()) {
                Team.getRed().add(p);
                p.teleport(m.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
            }//case goes from 1 to x,where x is the number of spawns
            //cycles through different spawn points

            else if (Team.getBlue().size() < Team.getRed().size()) {
                Team.getBlue().add(p);
                p.teleport(m.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
            }//case goes from 1 to x,where x is the number of spawns
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(!Team.getBlue().getMembers().contains(player) && !Team.getRed().getMembers().contains(player)){
                Team.getSpectator().add(player);
                player.teleport(m.getSpawn().toBukkitLoc().add(0, 2, 0));
            }
        }//players that didn't join become spectators

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
            @Override
            public void run() {
                if(count == 0){
                    if(state == GameState.RUNNING){
                        return;
                    }

                    Points = getScoreboard().registerNewObjective("Score", "dummy");
                    Points.setDisplayName("Name of score/time");
                    Points.getScore(ChatColor.WHITE + "Goal:").setScore(target);
                    //can repeat this to add more data, like each team's points or time or team members
                    Points.setDisplaySlot(DisplaySlot.SIDEBAR);
                    //feel free to customize scoreboard according to gamemode and preference

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
                    //gear distributors
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
                }//writes in chat : "game starts in 3,2,1..."
            }

        }, 40, 20);
    }

    public void End(Map m){
        state = GameState.IDLE;

        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);
    }//can also reset variables if needed here
    //finishes the map and processes running in it

    @Override
    public void playerLeave(Player p){
        Team.removeFromTeam(p);
    }

    public boolean midgamePlayerJoin(Player p){//player joins in the middle of the game

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
    }// optinal fucntion for joining midgame. Can add more logic for balance reasons, like midGameJoinPointThreshold

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
        return "whatever it is the gamemode needs to end, the goal basically, like kills or time";
    }

    private class Gamepvp implements Listener{
        /*
        This is where the logic of the game goes. This example below is Team Slayer, but put your own work in
         */
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){//when a player dies

            if(state == GameState.RUNNING){
                //logic for when a player dies
            }
        }
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){//when a player disconnects from thr server/node or leaves the game

            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                Team.removeFromTeam(e.getPlayer());

                if(Team.getRed().size() <= 0){//if all players from a team leave, they lose and game ends
                    //you need 2 of these at least, or however many teams you have

                    for(Player p : Bukkit.getOnlinePlayers()){
                        p.sendMessage(ChatColor.BLUE + "Game over!");
                        p.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
            }
        }
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){//player respawns

            if(state == GameState.RUNNING){
                if(Team.getRed().getMembers().contains(e.getPlayer())){

                    e.setRespawnLocation(map.getImportantPoints().get("RedSpawn1").toBukkitLoc().add(0, 2, 0));
                }
            }
            if(Team.getBlue().getMembers().contains(e.getPlayer())){
                e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn1").toBukkitLoc().add(0, 2, 0));
            }
        }
        //handles the respawning of players. If you have multiple spawns, do a random int and start a switch/case loop to cycle through different spawns
        //also any other logic for respawning could go here, but usually you should have it under player death.
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
