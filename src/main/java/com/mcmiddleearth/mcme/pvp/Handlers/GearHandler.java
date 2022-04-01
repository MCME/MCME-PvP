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
package com.mcmiddleearth.mcme.pvp.Handlers;
import com.mcmiddleearth.mcme.pvp.Gamemode.*;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.PVP.Team;
import java.util.Arrays;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Eric
 */
public class GearHandler {
    
    public enum SpecialGear{
        ONEINTHEQUIVER, INFECTED, RINGBEARER, NONE
    }
    
    public static void giveGear(Player p, ChatColor c, SpecialGear sg){
        ItemStack[] items;
        
        if(sg == SpecialGear.ONEINTHEQUIVER){
            items = new ItemStack[] {new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE), 
                new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.IRON_AXE), new ItemStack(Material.BOW)};
        }
        else{
            items = new ItemStack[] {new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE), 
                new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.IRON_SWORD), new ItemStack(Material.BOW), new ItemStack(Material.SHIELD), new ItemStack(Material.COMPASS)};
        }
        
        for(int i = 0; i <= 5; i++){
            if(i<=3){
                LeatherArmorMeta lam = (LeatherArmorMeta) items[i].getItemMeta();
                switch(c){
                    
                    case AQUA:
                        lam.setColor(DyeColor.LIGHT_BLUE.getColor());
                        break;
                    case BLUE:
                        lam.setColor(DyeColor.BLUE.getColor());
                        break;
                    case DARK_AQUA:
                        lam.setColor(DyeColor.CYAN.getColor());
                        break;
                    case DARK_GREEN:
                        lam.setColor(DyeColor.GREEN.getColor());
                        break;
                    case DARK_PURPLE:
                        lam.setColor(DyeColor.PURPLE.getColor());
                        break;
                    case DARK_RED:
                        lam.setColor(DyeColor.RED.getColor());
                        break;
                    case GOLD:
                        lam.setColor(DyeColor.LIGHT_GRAY.getColor());
                        break;
                    case GREEN:
                        lam.setColor(DyeColor.LIME.getColor());
                        break;
                    case LIGHT_PURPLE:
                        lam.setColor(DyeColor.MAGENTA.getColor());
                        break;
                    case RED:
                        lam.setColor(DyeColor.RED.getColor());
                        break;
                    case YELLOW:
                        lam.setColor(DyeColor.YELLOW.getColor());
                        break;
                    case BLACK:
                        lam.setColor(DyeColor.BLACK.getColor());
                }

                items[i].setItemMeta(lam);
            }
            else{
            }
            ItemMeta itemmeta;
            itemmeta = items[i].getItemMeta();
            itemmeta.setUnbreakable(true);
            items[i].setItemMeta(itemmeta);
        }
        p.getInventory().clear();
        p.getInventory().setHelmet(new ItemStack(Material.AIR));
        p.getInventory().setChestplate(new ItemStack(Material.AIR));
        p.getInventory().setLeggings(new ItemStack(Material.AIR));
        p.getInventory().setBoots(new ItemStack(Material.AIR));
        
        if(sg == SpecialGear.RINGBEARER ){
            p.getInventory().setHelmet(new ItemStack(Material.GLOWSTONE, 1));
            p.getInventory().setChestplate(items[1]);
            p.getInventory().setLeggings(items[2]);
            p.getInventory().setBoots(items[3]);
        }


        if(sg != SpecialGear.INFECTED){
            p.getInventory().setHelmet(items[0]);
        }
        
        
        if(sg == SpecialGear.INFECTED){
            p.getInventory().setChestplate(items[1]);
            p.getInventory().addItem(items[6]);
            p.getInventory().addItem(items[7]);
        }
        else{
            p.getInventory().setChestplate(items[1]);
            p.getInventory().setLeggings(items[2]);
            p.getInventory().setBoots(items[3]);
            
        }
        
        p.getInventory().addItem(items[4]);
        p.getInventory().addItem(items[5]);

        if(sg == SpecialGear.ONEINTHEQUIVER) {
            ItemStack Arrow = new ItemStack(Material.ARROW, 1);
            p.getInventory().addItem(Arrow);
        }
        else {
            ItemStack Arrows = new ItemStack(Material.ARROW, 24);
            p.getInventory().addItem(Arrows);
        }
        
        if(sg == SpecialGear.RINGBEARER){
            giveCustomItem(p, CustomItem.RING);
        }

        
    }
    
    public enum CustomItem{
        RING, PIPE, TNT
    }
    
    public static void giveCustomItem(Player p, CustomItem i){
        ItemMeta im;
        switch(i){
            
            case RING:
                ItemStack ring = new ItemStack(Material.GOLD_NUGGET);
                im = ring.getItemMeta();
                im.setDisplayName("The Ring");
                im.setLore(Arrays.asList(new String[] {"The One Ring of power...", "1 of 2"}));
                ring.setItemMeta(im);
        
                p.getInventory().addItem(ring);
                break;
            case PIPE:
                p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 1));
            case TNT:
                /*ItemStack tnt = new ItemStack(Material.TNT);
                im = tnt.getItemMeta();
                im.setDisplayName("BOMB");
                tnt.setItemMeta(im);
                p.getInventory().addItem(tnt);
                p.sendMessage(ChatColor.RED + "You have the BOMB!");
                p.sendMessage(ChatColor.RED + "Place it on the mycelium by the river gate to blow the wall!");*/
        }
        
    }
    public static class Gearpvp implements Listener{
        
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent e){

            if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
                final Player p = e.getPlayer();
                ItemStack item;
                if(p.getInventory().getItemInMainHand() != null){
                    item = p.getInventory().getItemInMainHand();
                }else{
                    return;
                }

                if(item.getType().equals(Material.GHAST_TEAR)){
                    p.getWorld().playEffect(p.getLocation().add(Dir('x', p.getLocation().getYaw())+ 0.5, 1.0, Dir('z', p.getLocation().getYaw())), Effect.SMOKE, 4);
                    return;
                }
                
                if(PVPCommand.getRunningGame() != null){
                    if(item.getItemMeta() == null){
                        return;
                    }
                    if(item.getItemMeta().getDisplayName() == null){
                        return;
                    }
                    
                    if(item.getItemMeta().getDisplayName().equalsIgnoreCase("The Ring") && 
                            PVPCommand.getRunningGame().getGm().getPlayers().contains(e.getPlayer()) &&
                            PVPCommand.getRunningGame().getGm() instanceof Ringbearer){
                        
                        if(p.getExp() >= 0.84f){
                            p.setExp(0);
                            p.sendMessage(ChatColor.YELLOW + "You are now invisible!");
                            p.sendMessage(ChatColor.GRAY + "Don't hold anything in your hand, or you'll be seen!");
                            p.getInventory().setHeldItemSlot(5);
                            
                            p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 500, 0, true, false));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 500, 0, true, true));
                            
                            p.getInventory().setHelmet(new ItemStack(Material.AIR));
                            p.getInventory().setChestplate(new ItemStack(Material.AIR));
                            p.getInventory().setLeggings(new ItemStack(Material.AIR));
                            p.getInventory().setBoots(new ItemStack(Material.AIR));
                            p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                            
                            com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler.removeFromBukkitTeam(p);
                            
                            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), new Runnable(){
                                
                                @Override
                                public void run() {
                                    p.sendMessage(ChatColor.YELLOW + "You are no longer invisible!");
                                   
                                    p.getInventory().clear();
                                    
                                    if(Team.getRed().getMembers().contains(p)){
                                        GearHandler.giveGear(p, ChatColor.RED, SpecialGear.RINGBEARER);
                                    }
                                    else{
                                        GearHandler.giveGear(p, ChatColor.BLUE, SpecialGear.RINGBEARER);
                                    }
                                    p.getInventory().setHeldItemSlot(1);
                                }
                            }, 500);
                        }else{
                            p.sendMessage(ChatColor.GRAY + "You must have at least 1 xp level to use the ring");
                        }
                    }
                    if(item.getType().equals(Material.TNT) &&
                            PVPCommand.getRunningGame().getTitle().equals("Helms_Deep") &&
                            PVPCommand.getRunningGame().getGm().getPlayers().contains(p) &&
                            (PVPCommand.getRunningGame().getGm() instanceof TeamSlayer ||
                            PVPCommand.getRunningGame().getGm() instanceof TeamConquest)){
                        
                        if(e.getClickedBlock().getType().equals(Material.MYCELIUM)){
                            Block toTnt = p.getWorld().getBlockAt(e.getClickedBlock().getLocation().add(0, 1, 0));
                            
                            toTnt.setType(Material.TNT);
                            
                            for(ItemStack i : p.getInventory().getContents()){
                                if(i != null && i.getType().equals(Material.TNT)){
                                    p.getInventory().remove(i);
                                }
                            }
                        }
                        
                    }
                }
            }
        }

        //handle tnt on death
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent playerDeathEvent){
            if(PVPCommand.getRunningGame() != null){
                
                if(PVPCommand.getRunningGame().getTitle().equals("Helms_Deep") &&
                        (PVPCommand.getRunningGame().getGm() instanceof TeamSlayer ||
                        PVPCommand.getRunningGame().getGm() instanceof TeamConquest)){
                    
                    Player player = playerDeathEvent.getEntity();
                    PlayerInventory inv = player.getInventory();
                    
                    if(inv.contains(Material.TNT)){
                        player.sendMessage(ChatColor.RED + "You no longer have the BOMB");
                        
                        for(ItemStack i : inv.getContents()){
                            if(i!= null && i.getType().equals(Material.TNT)){
                                inv.remove(i);
                            }
                        }
                        
                        Random r = new Random();
                        Player newTntHolder = (Player) Team.getRed().getMembers().toArray()[r.nextInt(Team.getRed().size())];
                        
                        giveCustomItem(newTntHolder, CustomItem.TNT);
                    }
                }
            }
        }
        public double Dir(char dir, double yaw){
            if(dir == 'x')
                return -0.8 *Math.sin(Math.toRadians(yaw));
            return 0.8 * Math.cos(Math.toRadians(yaw));
        }
    }
}
