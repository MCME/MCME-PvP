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
import com.mcmiddleearth.mcme.pvp.Handlers.ActionBarHandler;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author donoa_000
 */
public abstract class BasePluginGamemode implements com.mcmiddleearth.mcme.pvp.Gamemode.Gamemode {


    @JsonIgnore
    ArrayList<Player> players = new ArrayList<>();
    HashSet<UUID> frozen = new HashSet<>();

    HashMap<Player, Integer> killCounter = new HashMap<>();
    HashMap<Player, Integer> deathCounter = new HashMap<>();

    /**
     * IDLE = /pvp game quickstart map-gm has been performed, players can now do /pvp join to join the game.
     * COUNTDOWN = /pvp game start has been performed, 5-second countdown before the game starts.
     * RUNNING = The game is running.
     */
    public enum GameState {
        IDLE, COUNTDOWN, RUNNING
    }

    private static Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    private static org.bukkit.scoreboard.Team team = scoreboard.registerNewTeam("collision");
    
    public void playerLeave(Player p){
        players.remove(p);
        checkWin();
    }
    
    @Override
    public void Start(Map m, int parameter){

        PVPCommand.toggleVoxel("true");
        for(Player p : players) {
            killCounter.put(p, 0);
            deathCounter.put(p, 0);
            PlayerStat.getPlayerStats().get(p.getName()).addPlayedGame();
        }
        HashMap<Player, Location> lastLocation = new HashMap<>();
        HashMap<String, Long> lastOutOfBounds = new HashMap<>();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
            @Override
            public void run() {
                for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                    Location currentLoc = player.getLocation();
                    if (!PVPCommand.getRunningGame().getRegion().contains(BlockVector3.at(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ()))) {
                        player.teleport(lastLocation.get(player));
                        if (!lastOutOfBounds.containsKey(player.getName())) {
                            player.sendMessage(ChatColor.RED + "You aren't allowed to leave the map!");
                            lastOutOfBounds.put(player.getName(), System.currentTimeMillis());
                        } else if (System.currentTimeMillis() - lastOutOfBounds.get(player.getName()) > 3000) {
                            player.sendMessage(ChatColor.RED + "You aren't allowed to leave the map!");
                            lastOutOfBounds.put(player.getName(), System.currentTimeMillis());
                        }
                    }
                    lastLocation.put(player, player.getLocation());
                }
            }
        }, 0, 10);
    }
    
    @Override
    public void End(Map m) {
        killCounter.clear();
        deathCounter.clear();
        PVPCommand.setRunningGame(null);
        PVPCommand.toggleVoxel("false");
        
        Team.resetAllTeams();
        
        Bukkit.getScheduler().cancelTasks(PVPPlugin.getPlugin());
        for(Objective o : scoreboard.getObjectives()){
            o.unregister();
        }
        
        ChatHandler.getPlayerColors().clear();
        
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            ActionBarHandler.sendActionBarMessage(p, "");
            BukkitTeamHandler.removeFromBukkitTeam(p);
            ChatHandler.getPlayerColors().put(p.getName(), ChatColor.WHITE);
            p.teleport(PVPPlugin.getLobby());
            p.setDisplayName(ChatColor.WHITE + p.getName());
            p.setPlayerListName(ChatColor.WHITE + p.getName());
            p.getInventory().clear();
            p.setTotalExperience(0);
            p.setExp(0);
            p.setGameMode(GameMode.ADVENTURE);
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            ChatHandler.getPlayerPrefixes().remove(p.getName());
            
            if(!p.isDead()){
                p.setHealth(20);
            }
        }
    }

    /**
     * Sorts players by kd.
     */
    public void kdSort(){
        players.sort((Player p1, Player p2) -> {
            double offset1 = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            double offset2 = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            if (PlayerStat.getKD(p1) + offset1 > PlayerStat.getKD(p2) + offset2)
                return 1;
            else
                return -1;
        });
    }

    public boolean midgamePlayerJoin(Player p){
        killCounter.putIfAbsent(p, 0);
        deathCounter.putIfAbsent(p, 0);
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
            message = ChatColor.DARK_RED + p.getName() + ChatColor.GRAY + " has joined the fight as a " + ChatColor.DARK_RED + "Infected!";
        }
        for(Player pl : Bukkit.getOnlinePlayers()){
            pl.sendMessage(message);
        }
        
        return true;
    }

    public void checkWin(){
    }

    public void freezePlayer(Player p, int ticks){
        team.addPlayer(p);
        p.setAllowFlight(true);
        p.teleport(p.getLocation().add(0,0.1,0));
        p.setFlying(true);
        p.setFlySpeed(0);
        frozen.add(p.getUniqueId());
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15));
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), () -> unFreezePlayer(p), ticks);
    }

    private void unFreezePlayer(Player p){
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        team.removePlayer(p);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setFlySpeed(0.1F);
        frozen.remove(p.getUniqueId());
    }

    @Override
    public void incrementPlayerKills(Player player) {
        this.killCounter.put(player, this.killCounter.getOrDefault(player, 0) + 1);
    }

    @Override
    public void incrementPlayerDeaths(Player player) {
        this.deathCounter.put(player, this.deathCounter.getOrDefault(player, 0) + 1);
    }

    @Override
    public HashMap<Player, Double> getTopKDMap() {
        HashMap<Player, Double> kdMap = new HashMap<>();
        for(Player player :  players){
            double kills = killCounter.get(player);
            double deaths = deathCounter.get(player);
            double kdRatio = (deaths == 0) ? kills : Math.round((kills / deaths) * 100.00) / 100.00;
            kdMap.put(player, kdRatio);
        }
        return getTopPlayerIntegerMap(kdMap, 3);
    }

    @Override
    public HashMap<Player, Integer> getTopDeathsMap() {
        return getTopPlayerIntegerMap(deathCounter, 3);
    }

    @Override
    public HashMap<Player, Integer> getTopKillsMap() {
        return getTopPlayerIntegerMap(killCounter, 3);
    }

    /**
     * Returns a Map of the top players given a HashMap of players linked to integers. The amount dictates the amount of
     * players it will return. If multiple players share the top place they will all be returned, regardless of the amount specified.
     * @param kdMap Map of players linked to integers.
     * @param amount amount of players to return.
     */
    private <T extends Comparable<T>> HashMap<Player, T> getTopPlayerIntegerMap(HashMap<Player, T> kdMap, int amount) {
        PriorityQueue<Player> pq = new PriorityQueue<>((p1, p2) -> kdMap.get(p2).compareTo(kdMap.get(p1)));
        pq.addAll(kdMap.keySet());
        HashMap<Player, T> topThreePlayers = new LinkedHashMap<>();
        while (topThreePlayers.size() < amount && !pq.isEmpty()) {
            Player player = pq.poll();
            T kdValue = kdMap.get(player);
            topThreePlayers.put(player, kdValue);
        }

        return topThreePlayers;
    }

    public boolean isFrozen(Player p){
        return frozen.contains(p.getUniqueId());
    }

    @Override
    public ArrayList<Player> getPlayers() {
        return players;
    }

    public static Scoreboard getScoreboard() {
        return scoreboard;
    }

    public static void setTeamRule(){
        team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
    }
}
