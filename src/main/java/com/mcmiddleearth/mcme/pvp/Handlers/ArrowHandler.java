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

import com.mcmiddleearth.mcme.pvp.Gamemode.DeathRun;
import com.mcmiddleearth.mcme.pvp.Gamemode.Infected;
import com.mcmiddleearth.mcme.pvp.Gamemode.OneInTheQuiver;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author barteldvn
 */
public class ArrowHandler implements Listener {

    /**
     * Removes arrow 5 seconds after it hits a block.
     *
     * @param projectileHitEvent Projectile hitting an object event.
     */
    @EventHandler
    public void onArrowHitBlock(ProjectileHitEvent projectileHitEvent) {
        Projectile projectile = projectileHitEvent.getEntity();
        if (projectile instanceof Arrow && projectileHitEvent.getHitBlock() != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), () -> projectile.remove(), 100);
        }
    }

    /**
     * Cancels players picking up any arrows in OITQ and Deathrun.
     * Cancels players picking up more than 24 arrows in all GM.
     *
     * @param arrowPickupEvent Player arrow pickup event.
     */
    @EventHandler
    public void onArrowPickup(PlayerPickupArrowEvent arrowPickupEvent) {
        if (PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver || PVPCommand.getRunningGame().getGm() instanceof DeathRun || arrowPickupEvent.getPlayer().getInventory().contains(Material.ARROW, 24)) {
            arrowPickupEvent.setCancelled(true);
        }
    }

    /**
     * (If not OITQ and only if INF) Adds arrow back to player's inventory after shooting one.
     *
     * @param entityShootBowEvent Entity shoots bow.
     */
    @EventHandler
    public void onArrowShoot(EntityShootBowEvent entityShootBowEvent) {
        if (entityShootBowEvent.getEntity() instanceof Player) {
            Player shooter = (Player) entityShootBowEvent.getEntity();
            ItemStack Arrow = new ItemStack(Material.ARROW, 1);
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (!(PVPCommand.getRunningGame().getGm() instanceof OneInTheQuiver) && !shooter.getInventory().contains(Material.ARROW) && ((PVPCommand.getRunningGame().getGm() instanceof Infected) || (PVPCommand.getRunningGame().getGm() instanceof DeathRun))) {
                        shooter.getInventory().addItem(Arrow);
                    }
                }

            }, 1);
        }
    }


}