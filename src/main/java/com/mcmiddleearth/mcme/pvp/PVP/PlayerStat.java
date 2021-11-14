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
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class PlayerStat {
    
    private ArrayList<String> playersKilled = new ArrayList<String>();
    private int Kills = 0;
    private int Deaths = 0;
    private double KD = 0;
    private int gamesPlayed = 0;
    private int gamesWon = 0;
    private int gamesLost = 0;
    private int gamesSpectated = 0;
    private static HashMap<String, PlayerStat> playerStats = new HashMap<>();
    
    @JsonIgnore
    private UUID uuid;
    
    public PlayerStat(){}
    
    public PlayerStat(UUID uuid){this.uuid = uuid;}
    
    public static boolean loadStat(OfflinePlayer p){
        File loc = new File(PVPPlugin.getStatDirectory() + PVPPlugin.getFileSep() + p.getUniqueId());
        if(loc.exists()){
            PlayerStat ps = (PlayerStat) DBmanager.loadObj(PlayerStat.class, loc);
            ps.setUuid(p.getUniqueId());
            try {
                System.out.println("Loaded: " + DBmanager.getJSonParser().writeValueAsString(ps));
            } catch (JsonProcessingException ex) {
                Logger.getLogger(JoinLeaveHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            playerStats.put(p.getName(), ps);
            return true;
        }else{
            playerStats.put(p.getName(), new PlayerStat(p.getUniqueId()));
            
            return false;
        }
    }
        
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
    public void addKD(){setKD();}
    public void addPlayedGame(){gamesPlayed++;}
    public void addGameWon(){gamesWon++;}
    public void addGameLost(){gamesLost++;};
    public void addGameSpectated(){gamesSpectated++;};
    
    public static void addGameWon(Teams t){
        
        switch(t){
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
    
    public static void addGameLost(Teams t){
        switch(t){
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
    public static void addGameSpectatedAll(){
        for(Player p : Team.getSpectator().getMembers()){
            if(p!=null && p.isOnline()) {
                PlayerStat.getPlayerStats().get(p.getName()).addGameSpectated();
            }
        }
    }
    
    public static class StatListener implements Listener{
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e){
            if(PVPCommand.getRunningGame() != null){
                Player d = e.getEntity();
                if(PVPCommand.getRunningGame().getGm().getPlayers().contains(d)){
                    PlayerStat ps = PlayerStat.getPlayerStats().get(d.getName());
                    if(d.getKiller() != null){
                        Player k = d.getKiller();
                        if(PVPCommand.getRunningGame().getGm().getPlayers().contains(k)){
                            if(!PlayerStat.getPlayerStats().get(k.getName()).getPlayersKilled().contains(d.getName())){
                                PlayerStat.getPlayerStats().get(k.getName()).addPlayerKilled(d.getName());
                            }
                        }
                        PlayerStat.getPlayerStats().get(k.getName()).addKill();
                        PlayerStat.getPlayerStats().get(k.getName()).addKD();
                    }
                    ps.setDeaths(ps.getDeaths()+1);
                    ps.setKD();
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
    public double getKD() {
        return KD;
    }

    public void setKD() {
        KD = Math.floor(((Kills + 1) / (Deaths + 1)) * 100)/100;
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
