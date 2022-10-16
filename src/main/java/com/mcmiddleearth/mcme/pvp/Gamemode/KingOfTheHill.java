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
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class KingOfTheHill extends com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode {
    
    private Objective Points;
    
    private int target = 50;
    
    private final ArrayList<String> NeededPoints = new ArrayList<String>(Arrays.asList(new String[] {
        "RedSpawn",
        "BlueSpawn",
        "Hill"
    }));
    
    Map map;
    
    int count = 10;
    
    private boolean midgameJoin = false;
    
    private GameState state;
    
    Gamepvp pvp;
    
    @Override
    public void Start(Map m, int parameter) {
        super.Start(m,parameter);
        count = 10;
        state = GameState.COUNTDOWN;
        this.map = m;
        if(!m.getImportantPoints().keySet().containsAll(NeededPoints)){
            for(Player p : players){
                p.sendMessage(ChatColor.GREEN + "Game Cannot Start! Map maker f**ked up!");
            }
            End(m);
            return;
        }
        pvp = new KingOfTheHill.Gamepvp();
        for(Location l : pvp.points){
            l.getBlock().setType(Material.BEACON);
        }
        PluginManager pm = PVPPlugin.getServerInstance().getPluginManager();
        pm.registerEvents(pvp, PVPPlugin.getPlugin());
        for(Player p : players){
            if(Team.getBlue().size() < 16 && Team.getRed().size() < 16){
                if(Team.getBlue().size() >= Team.getRed().size()){
                    Team.getRed().add(p);
                    p.teleport(m.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
                    freezePlayer(p, 140);
                }else if(Team.getBlue().size() < Team.getRed().size()){
                    Team.getBlue().add(p);
                    p.teleport(m.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
                    freezePlayer(p, 140);
                }
            }else{
                Team.getSpectator().add(p);
                p.teleport(m.getImportantPoints().get("SpectatorSpawn").toBukkitLoc().add(0, 2, 0));
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPPlugin.getPlugin(), new Runnable(){
                @Override
                public void run() {
                    if(count == 0){
                        if(state == GameState.RUNNING){
                            return;
                        }

                        Points = getScoreboard().registerNewObjective("Points", "dummy");
                        Points.setDisplayName("Points");
                        Points.getScore(ChatColor.BLUE + "Blue:").setScore(0);
                        Points.getScore(ChatColor.RED + "Red:").setScore(0);
                        Points.setDisplaySlot(DisplaySlot.SIDEBAR);
                        
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game Start!");
                        }
                        
                        for(Player p : Team.getRed().getMembers()){
                            
                            p.teleport(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
                            p.setScoreboard(getScoreboard());
                            ItemStack[] armor = new ItemStack[] {new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE), 
                                new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS)};
                            for(int i = 0; i <= 3; i++){
                                armor[i].addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            }
                            p.getInventory().clear();
                            p.getInventory().setHelmet(armor[0]);
                            p.getInventory().setChestplate(armor[1]);
                            p.getInventory().setLeggings(armor[2]);
                            p.getInventory().setBoots(armor[3]);
                            ItemStack sword = new ItemStack(Material.IRON_SWORD);
                            sword.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            p.getInventory().addItem(sword);
                            ItemStack bow = new ItemStack(Material.BOW);
                            bow.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            p.getInventory().addItem(bow);
                            ItemStack Arrows = new ItemStack(Material.ARROW);
                            Arrows.setAmount(64);
                            p.getInventory().addItem(Arrows);
                            p.getInventory().addItem(Arrows);
                        }
                        for(Player p : Team.getBlue().getMembers()){

                            p.teleport(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
                            p.setScoreboard(getScoreboard());
                            ItemStack[] armor = new ItemStack[] {new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE), 
                                new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS)};
                            for(int i = 0; i <= 3; i++){
                                armor[i].addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            }
                            p.getInventory().clear();
                            p.getInventory().setHelmet(armor[0]);
                            p.getInventory().setChestplate(armor[1]);
                            p.getInventory().setLeggings(armor[2]);
                            p.getInventory().setBoots(armor[3]);
                            ItemStack sword = new ItemStack(Material.IRON_SWORD);
                            sword.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            p.getInventory().addItem(sword);
                            ItemStack bow = new ItemStack(Material.BOW);
                            bow.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            p.getInventory().addItem(bow);
                            ItemStack Arrows = new ItemStack(Material.ARROW);
                            Arrows.setAmount(64);
                            p.getInventory().addItem(Arrows);
                            p.getInventory().addItem(Arrows);
                        }
                        state = GameState.RUNNING;
                        count = -1;
                    }else if(count != -1){
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            p.sendMessage(ChatColor.GREEN + "Game begins in " + count);
                        }
                        count--;
                    }
                }

            }, 40, 11);
    }
    
    @Override
    public void End(Map m){
        state = GameState.IDLE;
        
        for(Location l : pvp.points){
            l.getBlock().setType(Material.AIR);
            l.getBlock().getRelative(0, 1, 0).setType(Material.AIR);
        }
        for(Player p : players){
            Team.removeFromTeam(p);
        }
        m.playerLeaveAll();
        PVPCommand.queueNextGame();
        super.End(m);

    }
    
    public boolean midgamePlayerJoin(Player p){
        return false;
    }
    
    public String requiresParameter(){
        return null;
    }
    
    private class Gamepvp implements Listener{
        
        private ArrayList<Location> points = new ArrayList<>();
        
        HashMap<Location, Integer> capAmount = new HashMap<>();//red = +; blue = -
        
        int HillCapAmount = 0;
        
        ArrayList<Player> InHill = new ArrayList<>();
        
        public Gamepvp(){
            points.add(map.getImportantPoints().get("Hill").toBukkitLoc());
            capAmount.put(map.getImportantPoints().get("Hill").toBukkitLoc(), 0);
        }
        
        @EventHandler
        public void onPlayerMove(PlayerMoveEvent e){
            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                Player p = e.getPlayer();
                Location cent = map.getImportantPoints().get("Hill").toBukkitLoc();
                if(p.getLocation().distance(cent) < 4){
                    if(Team.getRed().getMembers().contains(p)){
                        if(HillCapAmount > 0){
                            HillCapAmount++;
                        }
                    }else if(Team.getBlue().getMembers().contains(p)){
                        
                    }
                    
                }else if(InHill.contains(p)){
                    InHill.remove(p);
                    HillCapAmount--;
                }
            }
        }
        
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e){
            System.out.println("koth");
            if(state == GameState.RUNNING && players.contains(e.getPlayer())){
                if(Team.getRed().getMembers().contains(e.getPlayer())){
                    e.setRespawnLocation(map.getImportantPoints().get("RedSpawn").toBukkitLoc().add(0, 2, 0));
                }else if(Team.getBlue().getMembers().contains(e.getPlayer())){
                    e.setRespawnLocation(map.getImportantPoints().get("BlueSpawn").toBukkitLoc().add(0, 2, 0));
                }
            }else{
                System.out.println("This is in koth");
            }
        }
        
        private void PlayerCapture(Player p, Location cent){
            int cap = capAmount.get(cent);
            if(cap < 200){
                cap++;
                p.sendMessage(ChatColor.RED + "Cap at " + (cap/2) + "%");
                if(cap >= 200){
                    if(!Team.getRed().getCapturedPoints().contains(cent)){
                        Team.getRed().getCapturedPoints().add(cent);
                        p.sendMessage(ChatColor.RED + "Point Captured!");
                    }
                    if(Team.getBlue().getCapturedPoints().contains(cent)){
                        Team.getBlue().getCapturedPoints().remove(cent);
                    }
                }else{
                    capAmount.put(cent, cap);
                }
            }
            if(cap > -200){
                cap--;
                p.sendMessage(ChatColor.BLUE + "Cap at " + (cap/-2) + "%");
                if(cap <= -200){
                    if(!Team.getBlue().getCapturedPoints().contains(cent)){
                        Team.getBlue().getCapturedPoints().add(cent);
                        p.sendMessage(ChatColor.BLUE + "Point Captured!");
                    }
                    if(Team.getRed().getCapturedPoints().contains(cent)){
                        Team.getRed().getCapturedPoints().remove(cent);
                    }
                }else{
                    capAmount.put(cent, cap);
                }
            }
        }
    }

    public Objective getPoints() {
        return Points;
    }

    public int getTarget() {
        return target;
    }

    @Override
    public ArrayList<String> getNeededPoints() {
        return NeededPoints;
    }

    @Override
    public boolean isMidgameJoin() {
        return midgameJoin;
    }

    @Override
    public GameState getState() {
        return state;
    }
}
