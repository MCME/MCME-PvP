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

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.sk89q.worldedit.entity.Entity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupArrowEvent;

/**
 *
 * @author barteldvn
 */
public class ArrowHandler implements Listener{

    /**
     * Removes arrow when it hits a block.
     * @param projectileHitEvent Projectile hitting an object event.
     */
    @EventHandler
    public void onArrowHitBlock(ProjectileHitEvent projectileHitEvent){
        Projectile projectile = projectileHitEvent.getEntity();
        if (projectile instanceof  Arrow && projectileHitEvent.getHitBlock() != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPPlugin.getPlugin(), () -> projectile.remove(), 200);
        }
    }

    /**
     * Cancels players picking up arrows.
     * @param arrowPickupEvent Player arrow pickup event.
     */
    @EventHandler
    public void onArrowPickup (PlayerPickupArrowEvent arrowPickupEvent){
        arrowPickupEvent.setCancelled(true);
    }
    
}
