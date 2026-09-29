package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.UiNavigator;
import com.mcmiddleearth.pvp.ui.UiScreens;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * The clickable additions to the plain announcements of {@code TextAnnouncer}: the [Join] [Spectate]
 * [Rules] row after "game opened" for players outside games, the [Games] [My stats] row after the end-of-game
 * summary, and the arrival notice listing the open games when a player enters the server. It only adds buttons;
 * the texts themselves come from the text announcer, once. It never opens a dialog: those open only on a click
 * or a command. PvpPlugin adds it after TextAnnouncer, so each row follows its text, and its audiences are
 * TextAnnouncer's.
 */
public final class UiAnnouncer implements GameListener, Listener {

    private final PvpServices services;
    private final UiScreens screens;
    private final UiNavigator navigator;
    private final BooleanSupplier enabled;

    /** @param enabled false while the plugin shuts down */
    public UiAnnouncer(PvpServices services, UiScreens screens, UiNavigator navigator, BooleanSupplier enabled) {
        this.services = services;
        this.screens = screens;
        this.navigator = navigator;
        this.enabled = enabled;
    }

    @Override
    public void gameOpened(Game game, String openedBy) {
        ChatMenu buttons = screens.opened(game);
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean outside = services.games().gameOf(player.getUniqueId()).isEmpty();
            if (outside && !services.lock().keepsOut(player::hasPermission)) {
                navigator.show(Viewers.of(player), buttons);
            }
        }
    }

    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        if (!enabled.getAsBoolean()) {
            return;   // A game ended by the shutdown: buttons made now could never be clicked.
        }
        ChatMenu buttons = screens.summary();
        for (UUID id : game.audience()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                navigator.show(Viewers.of(player), buttons);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (services.lock().keepsOut(player::hasPermission)) {
            // LockListener tells them PvP is locked and sends them to the main server; the lock refuses their
            // joins anyway
            return;
        }
        screens.arrival().ifPresent(notice -> navigator.show(Viewers.of(player), notice));
    }
}
