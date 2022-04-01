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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Permissions;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class Locker implements Listener{
    
    private static final String Message = "PvP-server Locked";


    @EventHandler (priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent e){
        if(PVPCommand.isLocked()){
            e.setMotd(e.getMotd() + "\n" + ChatColor.BLUE + Message);
            e.setMaxPlayers(0);
        }
    }
    
    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent e){
///Logger.getGlobal().info("Player login: "  +locked+ " "+e.getPlayer().hasPermission(Permissions.LOCKER.getPermissionNode()));
//Logger.getGlobal().info("Player allowed: "+(locked && !e.getPlayer().hasPermission(Permissions.LOCKER.getPermissionNode())));
        if(PVPCommand.isLocked() && !e.getPlayer().hasPermission(Permissions.JOIN.getPermissionNode())){
///Logger.getGlobal().info("Player kick! ");
            e.getPlayer().sendMessage(Message);
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendPlayerToMain(e.getPlayer());
                }
            }.runTaskLater(PVPPlugin.getPlugin(),1);
            //e.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.BLUE + Message);
        }
    }
    
    
    private void sendPlayerToMain(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(player.getName());
        out.writeUTF("world");
        player.sendPluginMessage(PVPPlugin.getPlugin(), "BungeeCord", out.toByteArray());
    }
}
