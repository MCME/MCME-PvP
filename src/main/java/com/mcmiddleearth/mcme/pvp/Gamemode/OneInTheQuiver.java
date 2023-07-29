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
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.SpecialGear;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.Util.EventLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import lombok.Getter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Eric
 */
public class OneInTheQuiver extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private boolean pvpRegistered = false;

    @Getter
    private final ArrayList<String> NeededPoints = new ArrayList<>(Arrays.asList("PlayerSpawn"));
    
    private GameState state;
    
    Map map;
    
    private int count;
    
    private Objective points;

    private GamemodeHandlers OITQHandlers;

    private HashMap<String, String> playerDeaths = new HashMap<>();
    
    private HashMap<String, ChatColor> hasPlayed = new HashMap<String, ChatColor>();
    
    private HashMap<Player, Long> healing = new HashMap<>();
    
    private boolean midgameJoin = true;
    
    private final ChatColor[] chatColors = new ChatColor[]{
            ChatColor.AQUA,
            ChatColor.BLUE,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_PURPLE,
            ChatColor.DARK_RED,
            ChatColor.GOLD,
            ChatColor.GREEN,
            ChatColor.LIGHT_PURPLE,
            ChatColor.RED,
            ChatColor.YELLOW       
    };
    
    private EventLocation[] spawns;
    
    public OneInTheQuiver(){
        state = GameState.IDLE;
    }
    
    @Override
    public void Start(Map m, int parameter){
        super.Start(m, parameter);
        map = m;
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        List<EventLocation> spawnsTemp = new ArrayList<>();
        spawnsTemp.add(map.getImportantPoints().get("PlayerSpawn"));
        for(int i = 1;i < map.getImportantPoints().size();i++){
            spawnsTemp.add(map.getImportantPoints().get("PlayerSpawn"+i));
        }
        spawns = spawnsTemp.toArray(new EventLocation[0]);
        if(!map.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game cannot start! Not all needed points have been added!");
            }
            End(m);
        }
        
        if(!pvpRegistered){
            OITQHandlers = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(OITQHandlers, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        int c = 0;
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(players.contains(p)){
                p.teleport(spawns[c].toBukkitLoc().add(0, 1, 0));
                freezePlayer(p, 140);
                if(spawns.length == (c + 1)){
                    c = 0;
                }else{
                    c++;
                }
            }else{
                Team.getSpectator().add(p);
                p.teleport(map.getSpawn().toBukkitLoc());
            }
            
        }
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), () -> {
            if(count == 0){
                if(state == GameState.RUNNING){
                    return;
                }
                int k = 0;

                points = getScoreboard().registerNewObjective("Kills", "dummy");
                points.setDisplayName("Kills");
                points.setDisplaySlot(DisplaySlot.SIDEBAR);

                OITQHandlers.spawn = new Random().nextInt(spawns.length-1);

                for(Player p : Bukkit.getServer().getOnlinePlayers()){
                    p.sendMessage(ChatColor.GREEN + "Game Start!");
                    p.setScoreboard(getScoreboard());
                }

                for(Player p : players){

                    p.setGameMode(GameMode.ADVENTURE);

                    ChatHandler.getPlayerPrefixes().put(p.getName(), chatColors[k] + "Player");
                    ChatHandler.getPlayerColors().put(p.getName(), chatColors[k]);
                    hasPlayed.put(p.getName(), chatColors[k]);

                    points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);

                    if(p.getName().length() < 14){
                        p.setPlayerListName(chatColors[k] + p.getName());
                    }else{
                        String newName = p.getName().substring(0,13);
                        p.setPlayerListName(chatColors[k] + newName);
                    }
                    GearHandler.giveGear(p, chatColors[k], SpecialGear.ONEINTHEQUIVER);
                    BukkitTeamHandler.addToBukkitTeam(p, chatColors[k]);

                    if(chatColors.length == (k+1)){
                        k = 0;
                    }else{
                        k++;
                    }
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
        }, 40, 20);
    }
    
    public void End(Map m){
        PlayerStat.addGameSpectatedAll();
        state = GameState.IDLE;
        hasPlayed.clear();

        ArrayList<String> killMessages = new ArrayList<>();
        for(Entry<Player, Integer> player : getTopKillsMap().entrySet()){
            killMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " with " + player.getValue() + " kills!");
        }
        ArrayList<String> deathMessages = new ArrayList<>();
        for(Entry<Player, Integer> player : getTopDeathsMap().entrySet()){
            deathMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }
        ArrayList<String> KDMessages = new ArrayList<>();
        for(Entry<Player, Double> player : getTopKDMap().entrySet()){
            KDMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.GREEN + "Winner: ");
            player.sendMessage(killMessages.get(0));
            player.sendMessage(ChatColor.GREEN + "Highest KD: ");
            for (String message: KDMessages) {
                player.sendMessage(message);
            }
            player.sendMessage(ChatColor.GREEN + "Most Deaths: ");
            for (String message: deathMessages) {
                player.sendMessage(message);
            }
        }
        
        for(Player p : Bukkit.getOnlinePlayers()){
            BukkitTeamHandler.removeFromBukkitTeam(p);
        }
        
        getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        m.playerLeaveAll();
        playerDeaths.clear();

        PVPCommand.queueNextGame();
        super.End(m);
    }
    
    @Override
    public boolean midgamePlayerJoin(Player p){
        Random random = new Random();
        ChatColor color;
        Team.removeFromTeam(p);
        if(!hasPlayed.containsKey(p.getName())){
            
            color = chatColors[random.nextInt(chatColors.length)];
            ChatHandler.getPlayerColors().put(p.getName(), color);
            ChatHandler.getPlayerPrefixes().put(p.getName(), color + "Player");
            if (state == GameState.RUNNING) points.getScore(ChatHandler.getPlayerColors().get(p.getName()) + p.getName()).setScore(0);
            hasPlayed.put(p.getName(), color);
            
        }
        else{
            color = hasPlayed.get(p.getName());
            ChatHandler.getPlayerColors().put(p.getName(), color);
        }
        for(Player pl : players){
            pl.setPlayerListName(ChatHandler.getPlayerColors().get(pl.getName()) + pl.getName());
        }
        
        if(p.getName().length() < 14){
            p.setPlayerListName(color + p.getName());
        }else{
            String newName = p.getName().substring(0,13);
            p.setPlayerListName(color + newName);
        }
        
        p.teleport(spawns[OITQHandlers.spawn++].toBukkitLoc().add(0, 1, 0));
        p.setGameMode(GameMode.ADVENTURE);
        p.setScoreboard(getScoreboard());
        super.midgamePlayerJoin(p);
        
        GearHandler.giveGear(p, color, SpecialGear.ONEINTHEQUIVER);
        BukkitTeamHandler.addToBukkitTeam(p, color);
        
        return true;
    }
    
    public String requiresParameter(){
        return "none";
    }
    
    private class GamemodeHandlers implements Listener{

        int spawn = 0;
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent playerDeathEvent){
            int tempDeaths;

            if(playerDeathEvent.getEntity().getKiller() != null && state == GameState.RUNNING){
                    points.getScore(ChatHandler.getPlayerColors().get(playerDeathEvent.getEntity().getKiller().getName()) + playerDeathEvent.getEntity().getKiller().getName()).setScore(points.getScore(ChatHandler.getPlayerColors().get(playerDeathEvent.getEntity().getKiller().getName()) + playerDeathEvent.getEntity().getKiller().getName()).getScore() + 1);
                    PlayerInventory killerInv = playerDeathEvent.getEntity().getKiller().getInventory();
                    ItemStack Arrow = new ItemStack(Material.ARROW, 1);
                    killerInv.remove(Arrow);
                    if(!killerInv.contains(Arrow)){
                        killerInv.setItem(8, Arrow);
                    }
                    if(playerDeaths.containsKey(playerDeathEvent.getEntity().getName())){
                        tempDeaths = Integer.parseInt(playerDeaths.get(playerDeathEvent.getEntity().getName()));
                        playerDeaths.remove(playerDeathEvent.getEntity().getName());
                        playerDeaths.put(playerDeathEvent.getEntity().getName(), String.valueOf(tempDeaths + 1));
                    }else{
                        playerDeaths.put(playerDeathEvent.getEntity().getName(), "1");
                    }
                    if(points.getScore(ChatHandler.getPlayerColors().get(playerDeathEvent.getEntity().getKiller().getName()) + playerDeathEvent.getEntity().getKiller().getName()).getScore() == 21){
                        End(map);
                        playerDeathEvent.getEntity().teleport(PVPPlugin.getLobby());
                    }
            }
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){

            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                e.getPlayer().getInventory().remove(Material.BOW);
                e.getPlayer().getInventory().addItem(new ItemStack(Material.BOW,1));
                if(!e.getPlayer().getInventory().contains(Material.ARROW, 1)){
                    e.getPlayer().getInventory().addItem(new ItemStack(Material.ARROW,1));
                }
                e.setRespawnLocation(spawns[spawn++].toBukkitLoc().add(0, 1, 0));

                if(spawn >= spawns.length){
                    spawn = 0;
                }
            }
        }
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

