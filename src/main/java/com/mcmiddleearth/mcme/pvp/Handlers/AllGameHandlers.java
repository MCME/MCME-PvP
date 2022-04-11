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

import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode.GameState;
import com.mcmiddleearth.mcme.pvp.Gamemode.DeathRun;
import com.mcmiddleearth.mcme.pvp.Gamemode.OneInTheQuiver;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Permissions;
import com.mcmiddleearth.mcme.pvp.Util.DBmanager;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;

/**
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
                    if(!(PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver || PVPCommand.getRunningGame().getGm() instanceof DeathRun)){
                        if(!e.getPlayer().getInventory().contains(Material.ARROW, 24)){
                            ItemStack Arrows = new ItemStack(Material.ARROW, 24);
                            e.getPlayer().getInventory().setItem(8, Arrows);
                        }
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
            e.setRespawnLocation(PVPPlugin.getLobby());
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
                    e.setTo(new Location(to.getWorld(), from.getX(), to.getY(), from.getZ(), e.getPlayer().getEyeLocation().getYaw(), e.getPlayer().getEyeLocation().getPitch()));
                    return;
                }
            }
            if(!PVPCommand.getRunningGame().getRegion().contains(BlockVector3.at(to.getX(), to.getY(), to.getZ()))){
                e.setTo(new Location(to.getWorld(), from.getX(), to.getY(), from.getZ(), e.getPlayer().getEyeLocation().getYaw(), e.getPlayer().getEyeLocation().getPitch()));
                
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
        Player damagee;
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
        else return;

        if(e.getDamager() instanceof Player) {
            damager = (Player) e.getDamager();
        }
        else if(e.getDamager() instanceof Arrow){
            if(((Arrow) e.getDamager()).getShooter() instanceof Player) {
                damager = (Player) ((Arrow) e.getDamager()).getShooter();
                if (damager == damagee)
                    return;
                if(PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver)
                    e.setDamage(50);
            }
        }
        if(Team.areTeamMates(damagee, damager)){
            e.setCancelled(true);
        }
    }

    /**
     * Handles player damage taken, cancels damage event if game isn't running or if they take contact damage.
     * If the damage is enough to kill them, have them respawn.
     *
     * @param damageEvent represents damage event of Player.
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent damageEvent){
        if(damageEvent.getEntity() instanceof Player){
            if(PVPCommand.getRunningGame() == null)
                damageEvent.setCancelled(true);

            else if(PVPCommand.getRunningGame().getGm().getState() != GameState.RUNNING)
                damageEvent.setCancelled(true);

            else if (damageEvent.getCause().equals(EntityDamageEvent.DamageCause.CONTACT))
                damageEvent.setCancelled(true);

        }
    }

    /**
     * Cancels a player, without the RUN permission, trying to fly.
     *
     * @param playerToggleFlightEvent represents a Player toggling flight.
     */
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent playerToggleFlightEvent){
        Player player = playerToggleFlightEvent.getPlayer();
        if(!player.hasPermission(Permissions.RUN.getPermissionNode())){
            playerToggleFlightEvent.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    /**
     * Cancels player, without the RUN permission, interaction with inventory.
     *
     * @param inventoryClickEvent represents Player interacting with any inventory.
     */
    @EventHandler
    public void onInventoryInteract(InventoryClickEvent inventoryClickEvent) {
        if (!inventoryClickEvent.getWhoClicked().hasPermission(Permissions.RUN.getPermissionNode()))
            inventoryClickEvent.setCancelled(true);
    }

    /**
     * Cancels player, without the RUN permission, swapping items to their off-hand.
     *
     * @param swapHandItemEvent represents player swapping an item to their off-hand.
     */
    @EventHandler
    public void onPlayerSwapHandItem(PlayerSwapHandItemsEvent swapHandItemEvent){
        if (!swapHandItemEvent.getPlayer().hasPermission(Permissions.RUN.getPermissionNode()))
            swapHandItemEvent.setCancelled(true);
    }

    /**
     * On playerInteractEvent with chest for all GM except OITQ the player gets new arrows.
     * All other container interactions are blocked.
     *
     * @param playerInteractEvent represents player clicking a material
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent playerInteractEvent){
        Player player = playerInteractEvent.getPlayer();
        if (playerInteractEvent.getClickedBlock() == null)
            return;
        Material material = playerInteractEvent.getClickedBlock().getType();
        if(PVPCommand.getRunningGame() != null){

            if(material.equals(Material.BEACON) || material.equals(Material.ANVIL) || material.equals(Material.CHEST)|| material.equals(Material.FURNACE) || material.equals(Material.TRAPPED_CHEST) || material.equals(Material.CRAFTING_TABLE) || material.equals(Material.SHULKER_BOX)){
                playerInteractEvent.setCancelled(true);
            }

            if(material.equals(Material.CHEST) && !player.getInventory().contains(Material.ARROW, 24) && !(PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver)) {
                playerInteractEvent.setCancelled(true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 5, true, false));
                new BukkitRunnable(){
                    private int countdown = 3;
                    public void run(){
                        if ((countdown > 0)) {
                            ActionBarHandler.sendActionBarMessage(player, ChatColor.WHITE + "Restocking Supplies... " + ChatColor.GOLD + "" + ChatColor.BOLD + countdown);
                            countdown--;
                        }
                        if (countdown == 0) {
                            ActionBarHandler.sendActionBarMessage(player, ChatColor.GREEN + "" + ChatColor.BOLD + "Restocked!");
                            cancel();
                        }
                    }
                }.runTaskTimer(PVPPlugin.getPlugin(), 0, 20);
                ItemStack Arrows = new ItemStack(Material.ARROW, 24);
                player.getInventory().setItem(8, Arrows);
            }
        }
    }

    @EventHandler
    public void returnDroppedItems(PlayerDropItemEvent e){
        if(PVPCommand.getRunningGame() != null){
            e.setCancelled(true);
        }
    }
    
}
