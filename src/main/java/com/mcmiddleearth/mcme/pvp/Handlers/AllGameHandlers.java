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
package com.mcmiddleearth.mcme.pvp.Handlers;

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode.GameState;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.Util.DBmanager;
import com.sk89q.worldedit.math.BlockVector3;
import java.io.File;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class AllGameHandlers implements Listener{
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        if(e.getEntity().getKiller() == null){
            return;
        }
        
        e.setDeathMessage(com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler.getPlayerColors().get(e.getEntity().getName()) + e.getEntity().getName() + ChatColor.GRAY + " was killed by " + com.mcmiddleearth.mcme.pvp.Handlers.ChatHandler.getPlayerColors().get(e.getEntity().getKiller().getName()) + e.getEntity().getKiller().getName());
        
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e){
        if(PVPCommand.getRunningGame() != null){
            if(PVPCommand.getRunningGame().getGm().getPlayers().contains(e.getPlayer())){
                Map m = PVPCommand.getRunningGame();
                if(m != null){
                    if(m.getFbt()){
                        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 2));
                    }
                    if(m.getName().contains("HD")){
                        if(e.getPlayer().getInventory().contains(new ItemStack(Material.TNT))){
                            e.getPlayer().getInventory().remove(Material.TNT);
                            e.getPlayer().getLocation().getWorld().dropItem(e.getPlayer().getLocation(), new ItemStack(Material.TNT));
                        }
                    }
                }
            }
        }else{   
            e.setRespawnLocation(PVPPlugin.getSpawn());
        }
    }
    
    @EventHandler
    public void onWorldSave(WorldSaveEvent e){
        for(String mn : Map.maps.keySet()){
            Map m = Map.maps.get(mn);
            DBmanager.saveObj(m, new File(PVPPlugin.getPluginDirectory() + PVPPlugin.getFileSep() + "maps"), mn);
        }
    }
    
    HashMap<String, Long> lastOutOfBounds = new HashMap<>();
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){
        Location from = e.getFrom();
        Location to = e.getTo();
        
        if(PVPCommand.getRunningGame() != null){
            if(PVPCommand.getRunningGame().getGm().getState() == GameState.COUNTDOWN && !Team.getSpectator().getMembers().contains(e.getPlayer())){
                if(from.getX() != to.getX() || from.getZ() != to.getZ()){
                    e.setTo(new Location(to.getWorld(), from.getX(), to.getY(), from.getZ()));
                    return;
                }
            }
            if(!PVPCommand.getRunningGame().getRegion().contains(BlockVector3.at(to.getX(), to.getY(), to.getZ()))){
                e.setTo(new Location(to.getWorld(), from.getX(), to.getY(), from.getZ()));
                
                if(!lastOutOfBounds.containsKey(e.getPlayer().getName())){
                    e.getPlayer().sendMessage(ChatColor.RED + "You aren't allowed to leave the map!");
                    lastOutOfBounds.put(e.getPlayer().getName(), System.currentTimeMillis());
                }
                
                else if(System.currentTimeMillis() - lastOutOfBounds.get(e.getPlayer().getName()) > 3000){
                    e.getPlayer().sendMessage(ChatColor.RED + "You aren't allowed to leave the map!");
                    lastOutOfBounds.put(e.getPlayer().getName(), System.currentTimeMillis());
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent e){
        Player damagee = null;
        Player damager = null;
        
        if(PVPCommand.getRunningGame() == null){
            e.setCancelled(true);
            return;
        }
        else{
            if(PVPCommand.getRunningGame().getGm().getState() != GameState.RUNNING){
                e.setCancelled(true);
                return;
            }
        }
        
        if(e.getEntity() instanceof Player){
            damagee = (Player) e.getEntity();
        }
        else{
            return;
        }
        
        if(e.getDamager() instanceof Player){
            damager = (Player) e.getDamager();
        }
        else if(e.getDamager() instanceof Arrow){
            if(((Arrow) e.getDamager()).getShooter() instanceof Player){
                damager =  (Player) ((Arrow) e.getDamager()).getShooter();
            }
        }
        else{
            return;
        }
        
        if(Team.areTeamMates(damagee, damager)){
            e.setCancelled(true);
        }
        
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e){
        if(e.getEntity() instanceof Player){
            if(PVPCommand.getRunningGame() == null){
                e.setCancelled(true);
            }
            else if(PVPCommand.getRunningGame().getGm().getState() != GameState.RUNNING){
                e.setCancelled(true);
            }
            else if (e.getCause().equals(EntityDamageEvent.DamageCause.CONTACT)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        if(PVPCommand.getRunningGame() != null){
            if(e.getClickedBlock().getType().equals(Material.BEACON) || e.getClickedBlock().getType().equals(Material.ANVIL) || e.getClickedBlock().getType().equals(Material.CHEST) || e.getClickedBlock().getType().equals(Material.TRAPPED_CHEST) || e.getClickedBlock().getType().equals(Material.CRAFTING_TABLE) || e.getClickedBlock().getType().equals(Material.SHULKER_BOX)){
                e.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

}
