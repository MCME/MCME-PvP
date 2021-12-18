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
package com.mcmiddleearth.mcme.pvp.PVP;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Handlers.JoinLeaveHandler;
import com.mcmiddleearth.mcme.pvp.PVP.Team.Teams;
import com.mcmiddleearth.mcme.pvp.Util.DBmanager;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a player's stats.
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class PlayerStat {
    
    private ArrayList<String> playersKilled = new ArrayList<String>();
    private int Kills = 0;
    private int Deaths = 0;
    private int gamesPlayed = 0;
    private int gamesWon = 0;
    private int gamesLost = 0;
    private int gamesSpectated = 0;
    private static HashMap<String, PlayerStat> playerStats = new HashMap<>();
    
    @JsonIgnore
    private UUID uuid;
    
    public PlayerStat(){}
    
    public PlayerStat(UUID uuid){this.uuid = uuid;}

    /**
     * Checks if the player already has a playerstat file saved.
     * If the file exists load the playerstat.
     * If the file does not exist create the playerstat file using the player's uuid.
     *
     * @param player Represents a player.
     */
    public static void loadStat(OfflinePlayer player){
        System.out.println(player);
        File loc = new File(PVPPlugin.getStatDirectory() + PVPPlugin.getFileSep() + player.getUniqueId());
        if(loc.exists()){
            PlayerStat ps = (PlayerStat) DBmanager.loadObj(PlayerStat.class, loc);
            ps.setUuid(player.getUniqueId());
            try {
                System.out.println("Loaded: " + DBmanager.getJSonParser().writeValueAsString(ps));
            } catch (JsonProcessingException ex) {
                Logger.getLogger(JoinLeaveHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            playerStats.put(player.getName(), ps);
        }else{
            playerStats.put(player.getName(), new PlayerStat(player.getUniqueId()));
        }
    }

    /**
     * Writes playerstats to existing playerstats file.
     * The playerstats file is unique for each uuid.
     */
    public void saveStat(){
        File loc = PVPPlugin.getStatDirectory();
        try {
            System.out.println("Saved: " + DBmanager.getJSonParser().writeValueAsString(this));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(JoinLeaveHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        DBmanager.saveObj(this, loc, uuid.toString());
    }
    
    public void addDeath(){Deaths++;}
    public void addPlayerKilled(String k){playersKilled.add(k);}
    public void addKill(){Kills++;}
    public void addPlayedGame(){gamesPlayed++;}
    public void addGameWon(){gamesWon++;}
    public void addGameLost(){gamesLost++;}
    public void addGameSpectated(){gamesSpectated++;}

    /**
     * Increments gameswon stat for all players in the team.
     *
     * @param team Specifies the team.
     */
    public static void addGameWon(Teams team){
        
        switch(team){
            case RED:
                for(Player p : Team.getRed().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
                
            case BLUE:
                for(Player p : Team.getBlue().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
            case INFECTED:
                for(Player p : Team.getInfected().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
            case SURVIVORS:
                for(Player p : Team.getSurvivor().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
            case DEATH:
                for(Player p : Team.getDeath().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
            case RUNNER:
                for(Player p : Team.getRunner().getMembers()) {
                    PlayerStat.getPlayerStats().get(p.getName()).addGameWon();
                }
                break;
        }
        
    }

    /**
     * Increments gameslost stat for all players in the team.
     *
     * @param team Specifies the team.
     */
    public static void addGameLost(Teams team){
        switch(team){
            case RED:
                for(Player p : Team.getRed().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
            case BLUE:
                for(Player p : Team.getBlue().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
            case INFECTED:
                for(Player p : Team.getInfected().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
            case SURVIVORS:
                for(Player p : Team.getSurvivor().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
            case DEATH:
                for(Player p : Team.getDeath().getMembers()){
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
            case RUNNER:
                for(Player p : Team.getRunner().getMembers()) {
                    PlayerStat.getPlayerStats().get(p.getName()).addGameLost();
                }
                break;
        }
        
    }

    /**
     * Increments spectated stat by one for all online players in Team Spectator.
     */
    public static void addGameSpectatedAll(){
        for(Player player : Team.getSpectator().getMembers()){
            if(player!=null && player.isOnline()) {
                PlayerStat.getPlayerStats().get(player.getName()).addGameSpectated();
            }
        }
    }

    /**
     * Gets the player's KD ratio.
     *
     * @param player Represents a player.
     * @return double representing the player's KD ratio
     */
    public static double getKD(Player player) {
        double kills = PlayerStat.getPlayerStats().get(player.getName()).getKills();
        double deaths = PlayerStat.getPlayerStats().get(player.getName()).getDeaths();
        if (deaths == 0)
            return kills;
        else
            return kills / deaths;
    }

    public static class StatListener implements Listener{


        /**
         * @EventHandler for player deaths.
         * Increments death stat for player that died, if a player killed him, increment his kill stat.
         *
         * @param pdEvent Thrown whenever a player dies.
         */
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent pdEvent){
            if(PVPCommand.getRunningGame() != null){
                Player p1 = pdEvent.getEntity();
                if(PVPCommand.getRunningGame().getGm().getPlayers().contains(p1)){
                    PlayerStat ps = PlayerStat.getPlayerStats().get(p1.getName());
                    if(p1.getKiller() != null){
                        Player p2 = p1.getKiller();
                        if(PVPCommand.getRunningGame().getGm().getPlayers().contains(p2)){
                            if(!PlayerStat.getPlayerStats().get(p2.getName()).getPlayersKilled().contains(p1.getName())){
                                PlayerStat.getPlayerStats().get(p2.getName()).addPlayerKilled(p1.getName());
                            }
                        }
                        PlayerStat.getPlayerStats().get(p2.getName()).addKill();
                    }
                    ps.addDeath();
                }
            }
        }
    }


    public ArrayList<String> getPlayersKilled() {
        return playersKilled;
    }

    public int getKills() {
        return Kills;
    }

    public void setKills(int kills) {
        Kills = kills;
    }

    public int getDeaths() {
        return Deaths;
    }

    public void setDeaths(int deaths) {
        Deaths = deaths;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int getGamesSpectated() {
        return gamesSpectated;
    }

    public void setGamesSpectated(int gamesSpectated) {
        this.gamesSpectated = gamesSpectated;
    }

    public static HashMap<String, PlayerStat> getPlayerStats() {
        return playerStats;
    }

    public static void setPlayerStats(HashMap<String, PlayerStat> playerStats) {
        PlayerStat.playerStats = playerStats;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
