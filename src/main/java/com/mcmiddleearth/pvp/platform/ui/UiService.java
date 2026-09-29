package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.UiNavigator;
import com.mcmiddleearth.pvp.ui.Viewer;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

/**
 * Shows the views' output to players: screens as dialogs, remembered by the tracker so a countdown can close them,
 * and chat menus as chat. Where the server cannot build dialogs, a screen is written as chat instead, so the menus
 * still work. Viewers who went offline are skipped.
 */
public final class UiService implements UiNavigator {

    private final DialogRenderer dialogs;
    private final ChatRenderer chat;
    private final DialogTracker tracker;
    private final Messages messages;
    private final Logger logger;
    private boolean fallbackLogged;

    public UiService(DialogRenderer dialogs, ChatRenderer chat, DialogTracker tracker, Messages messages,
                     Logger logger) {
        this.dialogs = dialogs;
        this.chat = chat;
        this.tracker = tracker;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void show(Viewer viewer, Screen screen) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) {
            return;
        }
        if (!dialogs.supported()) {
            if (!fallbackLogged) {
                fallbackLogged = true;
                logger.warning("Paper dialogs are not available, so menus are shown in chat instead");
            }
            send(player, chat.render(viewer, screen));
            return;
        }
        Dialog dialog = dialogs.render(viewer, screen);
        player.showDialog(dialog);
        // Tracked only once shown: if showDialog throws, a later closeDialog() must not close some other screen
        // of the player's.
        tracker.opened(player.getUniqueId());
    }

    @Override
    public void show(Viewer viewer, ChatMenu menu) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player != null) {
            send(player, chat.render(viewer, menu));
        }
    }

    @Override
    public void message(Viewer viewer, Component message) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player != null) {
            player.sendMessage(message);
        }
    }

    /**
     * Runs {@code command} as the viewer, with the viewer's own permissions. Paper logs only typed commands, so a
     * command run from a menu is logged here, and one that does not run is answered.
     */
    @Override
    public void perform(Viewer viewer, String command) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) {
            return;
        }
        logger.info(player.getName() + " runs /" + command + " from a menu");
        if (!player.performCommand(command)) {
            logger.warning("/" + command + " from a menu did not run for " + player.getName());
            player.sendMessage(messages.get(Msg.ERROR_BUTTON_FAILED));
        }
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    private static void send(Player player, List<Component> lines) {
        for (Component line : lines) {
            player.sendMessage(line);
        }
    }
}
