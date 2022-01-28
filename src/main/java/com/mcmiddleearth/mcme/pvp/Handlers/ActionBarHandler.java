package com.mcmiddleearth.mcme.pvp.Handlers;

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.Permissions;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarHandler {

    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public static void sendLockedMessage() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (PVPCommand.isLocked()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission(Permissions.JOIN.getPermissionNode())) {
                            ActionBarHandler.sendActionBar(player, ChatColor.RED + "Locked");
                        }
                    }
                }else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                            ActionBarHandler.sendActionBar(player, "");
                    }
                            this.cancel();
                        }
                    }
            }.runTaskTimer(PVPPlugin.getPlugin(), 0,20);
    }
}
