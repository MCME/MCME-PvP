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

import com.mcmiddleearth.mcme.pvp.Permissions;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 *
 * @author Donovan <dallen@dallen.xyz>
 */
public class ChatHandler extends PlaceholderExpansion implements Listener {
    
    private static HashMap<String, String> playerPrefixes = new HashMap<String, String>();
    
    private static HashMap<String, ChatColor> playerColors = new HashMap<String, ChatColor>();
    
    public static String formatChat(Player p){
        if(playerPrefixes.containsKey(p.getName())){
            return playerPrefixes.get(p.getName()) + " " + p.getName() + ChatColor.RESET + ": " + "%2$s";
        }else{
            if(p.hasPermission(Permissions.PVP_ADMIN.getPermissionNode())){
                return ChatColor.GOLD + "Staff" + " " + p.getName() + ChatColor.RESET + ": " + "%2$s";
            }if(p.hasPermission(Permissions.RUN.getPermissionNode())){
                return ChatColor.GOLD + "Manager " + p.getName() + ChatColor.RESET + ":%2$s";
            } else{
                return ChatColor.GRAY + "Lobby" + " " + p.getName() + ChatColor.RESET + ": " + "%2$s";
            }
        }
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        //e.setFormat(formatChat(e.getPlayer()));
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if(player==null || !player.isOnline()) {
            return "null player";
        }
        Player p = player.getPlayer();
        switch(identifier) {
            case "prefix":
                if(playerPrefixes.containsKey(p.getName())) {
                    return playerPrefixes.get(p.getName()) + " " + p.getName() + ChatColor.RESET + ": ";
                } else {
                    if(p.hasPermission(Permissions.PVP_ADMIN.getPermissionNode())){
                        return ChatColor.GOLD + "PvP Staff" + " " + p.getName() + ChatColor.RESET + ": " ;
                    }if(p.hasPermission(Permissions.RUN.getPermissionNode())){
                        return ChatColor.GOLD + "Manager " + p.getName() + ChatColor.RESET + ": ";
                    }else{
                        return ChatColor.GRAY + "Lobby" + " " + p.getName() + ChatColor.RESET + ": " ;
                    }
               }
            case "color":
                if(playerPrefixes.containsKey(p.getName())) {
                    return ""+playerColors.get(p.getName());
                } else {
                    if(p.hasPermission(Permissions.PVP_ADMIN.getPermissionNode())){
                        return ""+ChatColor.GOLD;
                    }else{
                        return ""+ChatColor.GRAY;
                    }
                }
        }
        return "";
    }

    public static HashMap<String, String> getPlayerPrefixes() {
        return playerPrefixes;
    }

    public static HashMap<String, ChatColor> getPlayerColors() {
        return playerColors;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mcmepvp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Eriol_Eandur";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.3";
    }
}
