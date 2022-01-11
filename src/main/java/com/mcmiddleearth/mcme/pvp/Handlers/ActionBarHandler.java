package com.mcmiddleearth.mcme.pvp.Handlers;

import com.mcmiddleearth.mcme.pvp.PVPPlugin;
import com.mcmiddleearth.mcme.pvp.command.PVPCommand;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ActionBarHandler {

    public static void sendActionBar(Player player, String message){
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public static void sendLockedMessage(Player player){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (PVPCommand.isLocked())
                    ActionBarHandler.sendActionBar(player, ChatColor.RED + "Locked");
                else{
                    ActionBarHandler.sendActionBar(player, "");
                    this.cancel();
                }
            }
        }.runTaskTimer(PVPPlugin.getPlugin(), 0, 20);
    }
}
