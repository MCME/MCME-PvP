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

import com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler;
import com.mcmiddleearth.mcme.pvp.Handlers.GearHandler.GearType;
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
import java.util.Random;

import static org.bukkit.potion.PotionEffectType.GLOWING;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class Ringbearer extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {//Handled by plugin
    
    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[] {
        "RedSpawn",
        "BlueSpawn",
    }));
    
    Map map;
    
    private int count;
    
    private GameState state;
    private GamemodeHandlers pvp;

    private Player redBearer = null;
    private Player blueBearer = null;
    
    private boolean redCanRespawn;
    private boolean redBearerHasRespawned;
    
    private boolean blueCanRespawn;
    private boolean blueBearerHasRespawned;

    private boolean pvpRegistered = false;
    
    private Objective Points;
    
    public Ringbearer(){
        state = GameState.IDLE;
    }
    
    Runnable exp = new Runnable(){

            @Override
            public void run() {
                if(state == GameState.RUNNING){
                    if(redBearer != null){
                        
                        if(!redBearer.hasPotionEffect(PotionEffectType.INVISIBILITY) && redBearer.getExp() < 1f){
                            redBearer.setExp(redBearer.getExp() + .006f);
                        }
                        
                        if((redBearer.getInventory().getHelmet() == null || redBearer.getInventory().getHelmet().getType() != Material.GLOWSTONE) &&
                                redCanRespawn&& (!redBearer.hasPotionEffect(PotionEffectType.INVISIBILITY))){
                            redBearer.getInventory().setHelmet(new ItemStack(Material.GLOWSTONE));
                        }
                        
                    }
                    if(blueBearer != null){
                        
                        if(!blueBearer.hasPotionEffect(PotionEffectType.INVISIBILITY) && blueBearer.getExp() < 1f){
                            blueBearer.setExp(blueBearer.getExp() + .006f);
                        }
                        
                        if((blueBearer.getInventory().getHelmet() == null || blueBearer.getInventory().getHelmet().getType() != Material.GLOWSTONE) &&
                                blueCanRespawn && (!blueBearer.hasPotionEffect(PotionEffectType.INVISIBILITY))){
                            blueBearer.getInventory().setHelmet(new ItemStack(Material.GLOWSTONE));
                        }
                        
                    }
                }
            }
            
        };
    
    @Override
    public void Start(Map m,int parameter) {
        kdSort();
        super.Start(m,parameter);
        count = PVPPlugin.getCountdownTime();
        state = GameState.COUNTDOWN;
        this.map = m;
        if(!m.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.RED + "Game Cannot Start! Not all needed points have been added!");
            }
            End(m);
            return;
        }
        
        if(!pvpRegistered){
            pvp = new GamemodeHandlers();
            PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
            pm.registerEvents(pvp, PVPPlugin.getPlugin());
            pvpRegistered = true;
        }
        
        blueCanRespawn = true;
        blueBearerHasRespawned = false;
        redCanRespawn = true;
        redBearerHasRespawned = false;
        
        Points = getScoreboard().registerNewObjective("Remaining", "dummy");
        Points.setDisplayName("Remaining");
        Points.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        for(Player p : Bukkit.getOnlinePlayers()){
            p.setScoreboard(getScoreboard());
            if(players.contains(p)){
                if(Team.getBlue().size() >= Team.getRed().size()){
                    Team.getRed().add(p);
                    p.teleport(m.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 1, 0));
                    freezePlayer(p, 140);
                }else if(Team.getBlue().size() < Team.getRed().size()){
                    Team.getBlue().add(p);
                    p.teleport(m.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 1, 0));
                    freezePlayer(p, 140);
                }
            }else{
                Team.getSpectator().add(p);
                p.teleport(m.getSpawn().toBukkitLoc().add(0, 1, 0));
            }
        }
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
                @Override
                public void run() {
                    if(count == 0){
                        if(state == GameState.RUNNING){
                            return;
                        }
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                            
                        }
                        
                        Random r = new Random();
                        
                        setRingbearer(Teams.RED, Team.getRed().getMembers().get(r.nextInt(Team.getRed().size())));
                        setRingbearer(Teams.BLUE, Team.getBlue().getMembers().get(r.nextInt(Team.getBlue().size())));
                        
                        for(Player p : Team.getRed().getMembers()){
                            
                            if(!p.equals(redBearer)){
                                GearHandler.giveGear(p, ChatColor.RED, GearType.STANDARD);
                            }
                            
                        }
                        
                        for(Player p : Team.getBlue().getMembers()){

                            if(!p.equals(blueBearer)){
                                GearHandler.giveGear(p, ChatColor.BLUE, GearType.STANDARD);
                            }
                            
                        }
                        
                        Points.getScore(ChatColor.BLUE + "Blue:").setScore(Team.getBlue().size());
                        Points.getScore(ChatColor.RED + "Red:").setScore(Team.getRed().size());
                        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), exp, 0, 20);
                        
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
    
    private void setRingbearer(Teams t, Player p){
        
        if(t == Teams.BLUE){
            blueBearer = p;
            GearHandler.giveGear(p, ChatColor.BLUE, GearType.RINGBEARER);
            
            blueBearer.sendMessage(ChatColor.BLUE + "You are Blue Team's Bearer!");
            blueBearer.sendMessage(ChatColor.BLUE + "Stay alive as long as you can!");
            
            for(Player pl : Team.getBlue().getMembers()){
                
                if(!pl.equals(blueBearer)){
                    pl.sendMessage(ChatColor.BLUE + blueBearer.getName() + " is your team's Ringbearer!");
                }
                
            }
            
        }
        else if(t == Teams.RED){
            redBearer = p;
            GearHandler.giveGear(p, ChatColor.RED, GearType.RINGBEARER);
            
            redBearer.sendMessage(ChatColor.RED + "You are Red Team's Bearer!");
            redBearer.sendMessage(ChatColor.RED + "Stay alive as long as you can!");
            
            for(Player pl : Team.getRed().getMembers()){
                
                if(!pl.equals(redBearer)){
                    pl.sendMessage(ChatColor.RED + p.getName() + " is your team's Ringbearer!");
                }
                
            }
        }
    }
    
    @Override
    public void End(Map m){
        state = GameState.IDLE;

        ArrayList<String> deathMessages = new ArrayList<>();
        for(java.util.Map.Entry<Player, Integer> player : getTopDeathsMap().entrySet()){
            deathMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }
        ArrayList<String> KDMessages = new ArrayList<>();
        for(java.util.Map.Entry<Player, Double> player : getTopKDMap().entrySet()){
            KDMessages.add(ChatHandler.getPlayerColors().get(player.getKey().getName()) + player.getKey().getName() + ChatColor.GREEN + " " + player.getValue());
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendMessage(ChatColor.GREEN + "Highest KD: ");
            for (String message: KDMessages) {
                player.sendMessage(message);
            }
            player.sendMessage(ChatColor.GREEN + "Most Deaths: ");
            for (String message: deathMessages) {
                player.sendMessage(message);
            }
        }

        redBearer = null;
        blueBearer = null;
        
        m.playerLeaveAll();

        PVPCommand.queueNextGame();
        super.End(m);
    }
        
    public boolean isMidgameJoin(){
        
        if(redCanRespawn || blueCanRespawn){
            return true;
        }else{
            return false;
        }
    }
    
    public boolean midgamePlayerJoin(Player p){
        if(Team.getRed().getAllMembers().contains(p)){
            addToTeam(p, Teams.RED);
        }
        else if(Team.getBlue().getAllMembers().contains(p)){
            addToTeam(p, Teams.BLUE);
        }
        
        else if((redCanRespawn && !blueCanRespawn)){
            return false;
        }
        else if((blueCanRespawn && !redCanRespawn) || Team.getBlue().getAllMembers().contains(p)){
            return false;
        }
        else if((!redCanRespawn && !blueCanRespawn)){
            return false;
        }
        else if(redCanRespawn && blueCanRespawn){
            
            if(Team.getRed().size() >= Team.getBlue().size()){
                addToTeam(p, Teams.BLUE);
            }
            else if(Team.getRed().size() < Team.getBlue().size()){
                addToTeam(p, Teams.RED);
            }
            
        }
        super.midgamePlayerJoin(p);
        return true;
    }
    
    private void addToTeam(Player p, Teams t){
        if(t == Teams.RED){
            Team.getRed().add(p);
            p.teleport(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 1, 0));
            Points.getScore(ChatColor.RED + "Red:").setScore(Points.getScore(ChatColor.RED + "Red:").getScore() + 1);
            GearHandler.giveGear(p, ChatColor.RED, GearType.STANDARD);
        }
        else{
            Team.getBlue().add(p);
            p.teleport(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 1, 0));
            Points.getScore(ChatColor.BLUE + "Blue:").setScore(Points.getScore(ChatColor.BLUE + "Blue:").getScore() + 1);
            GearHandler.giveGear(p, ChatColor.BLUE, GearType.STANDARD);
        }
    }

    public void checkWin(){
        if(Team.getRed().size() <= 0){

            for(Player pl : Bukkit.getOnlinePlayers()){
                pl.sendMessage(ChatColor.BLUE + "Game over!");
                pl.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
            }
            PlayerStat.addGameWon(Teams.BLUE);
            PlayerStat.addGameLost(Teams.RED);
            PlayerStat.addGameSpectatedAll();
            End(map);
        }
        else if(Team.getBlue().size() <= 0){

            for(Player pl : Bukkit.getOnlinePlayers()){
                pl.sendMessage(ChatColor.RED + "Game over!");
                pl.sendMessage(ChatColor.RED + "Red Team Wins!");
            }
            PlayerStat.addGameWon(Teams.RED);
            PlayerStat.addGameLost(Teams.BLUE);
            PlayerStat.addGameSpectatedAll();
            End(map);
        }
    }
    public String requiresParameter(){
        return "none";
    }
    private class GamemodeHandlers implements Listener{
        
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent playerDeathEvent){
            
            if(state == GameState.RUNNING){
                Player player = playerDeathEvent.getEntity();
                
                if(Team.getRed().getMembers().contains(player)){
                    
                    if(redBearer.equals(player) && redCanRespawn){
                        redCanRespawn = false;
                        GearHandler.giveGear(player, ChatColor.RED, GearType.STANDARD);
                        BukkitTeamHandler.addToBukkitTeam(player, ChatColor.RED);
                        if(blueCanRespawn){
                        getBlueBearer().addPotionEffect(new PotionEffect(GLOWING, Integer.MAX_VALUE, 1));
                        }
                        
                        for(Player pl : Bukkit.getOnlinePlayers()){
                            pl.sendMessage(ChatColor.RED + "Red Team's Ringbearer has been killed!");
                            pl.sendMessage(ChatColor.RED + "They can't respawn!");
                        }
                    }
                    else if(player.equals(redBearer) && redBearerHasRespawned){
                        Team.getSpectator().add(player);
                    }
                    
                    else if(!redCanRespawn){
                        Team.getSpectator().add(player);
                    }
                    for(PotionEffect effect:player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                    
                }
                else if(Team.getBlue().getMembers().contains(player)){
                    
                    if(blueBearer.equals(player) && blueCanRespawn){
                        blueCanRespawn = false;
                        GearHandler.giveGear(player, ChatColor.BLUE, GearType.STANDARD);
                        BukkitTeamHandler.addToBukkitTeam(player, ChatColor.BLUE);
                        if(redCanRespawn){
                            getRedBearer().addPotionEffect(new PotionEffect(GLOWING, Integer.MAX_VALUE, 1));
                        }
                        for(Player pl : Bukkit.getOnlinePlayers()){
                            pl.sendMessage(ChatColor.BLUE + "Blue Team's Ringbearer has been killed!");
                            pl.sendMessage(ChatColor.BLUE + "They can't respawn!");
                        }
                    }
                    
                    else if(player.equals(blueBearer) && blueBearerHasRespawned){
                        Team.getSpectator().add(player);
                    }
                    
                    else if(!blueCanRespawn){
                        Team.getSpectator().add(player);
                    }
                    for(PotionEffect effect:player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                }
                
                Points.getScore(ChatColor.BLUE + "Blue:").setScore(Team.getBlue().size());
                Points.getScore(ChatColor.RED + "Red:").setScore(Team.getRed().size());
                
                if(Team.getRed().size() <= 0){
                    
                    for(Player pl : Bukkit.getOnlinePlayers()){
                        pl.sendMessage(ChatColor.BLUE + "Game over!");
                        pl.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                }
                else if(Team.getBlue().size() <= 0){
                    
                    for(Player pl : Bukkit.getOnlinePlayers()){
                        pl.sendMessage(ChatColor.RED + "Game over!");
                        pl.sendMessage(ChatColor.RED + "Red Team Wins!");
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
            
            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                if(Team.getRed().getMembers().contains(e.getPlayer())){
                    e.setRespawnLocation(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 1, 0));
                    
                    if(e.getPlayer().equals(redBearer) && !redBearerHasRespawned){
                        redBearerHasRespawned = true;
                        
                    }
                    
                }
                else if(Team.getBlue().getMembers().contains(e.getPlayer())){
                    e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 1, 0));
                    
                    if(e.getPlayer().equals(blueBearer) && !blueBearerHasRespawned){
                        blueBearerHasRespawned = true;
                        
                    }
                }
                else{
                    e.setRespawnLocation(map.getSpawn().toBukkitLoc().add(0, 1, 0));
                }
            }
        }
        
        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent e){

            if(state == GameState.RUNNING || state == GameState.COUNTDOWN){
                Random rand = new Random();
                Team.removeFromTeam(e.getPlayer());
                
                if(e.getPlayer().equals(redBearer) && redCanRespawn){
                    int bearerNum = rand.nextInt(Team.getRed().size());
                    int loop = 0;
                    
                    for(Player p : Team.getRed().getMembers()){
                        if(loop == bearerNum){
                            if(!p.equals(e.getPlayer())){
                                setRingbearer(Teams.RED, p);
                                break;
                            }else{
                                bearerNum++;
                            }
                            
                        }
                        loop++;
                    }
                    
                }
                else if(e.getPlayer().equals(blueBearer) && blueCanRespawn){
                    int bearerNum = rand.nextInt(Team.getRed().size());
                    int loop = 0;
                    
                    for(Player p : Team.getBlue().getMembers()){
                        if(loop == bearerNum){
                            if(!p.equals(e.getPlayer())){
                                setRingbearer(Teams.BLUE, p);
                                break;
                            }else{
                                bearerNum++;
                            }
                        }
                        loop++;
                    }
                }
                
                Points.getScore(ChatColor.BLUE + "Blue:").setScore(Team.getBlue().size());
                Points.getScore(ChatColor.RED + "Red:").setScore(Team.getRed().size());
                
                if(Team.getBlue().size() <= 0){
                    
                    for(Player pl : Bukkit.getOnlinePlayers()){
                        pl.sendMessage(ChatColor.RED + "Game over!");
                        pl.sendMessage(ChatColor.RED + "Red Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.RED);
                    PlayerStat.addGameLost(Teams.BLUE);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                    
                }
                else if(Team.getRed().size() <= 0){
                    
                    for(Player pl : Bukkit.getOnlinePlayers()){
                        pl.sendMessage(ChatColor.BLUE + "Game over!");
                        pl.sendMessage(ChatColor.BLUE + "Blue Team Wins!");
                    }
                    PlayerStat.addGameWon(Teams.BLUE);
                    PlayerStat.addGameLost(Teams.RED);
                    PlayerStat.addGameSpectatedAll();
                    End(map);
                    
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

    public Player getRedBearer() {
        return redBearer;
    }

    public Player getBlueBearer() {
        return blueBearer;
    }

}
