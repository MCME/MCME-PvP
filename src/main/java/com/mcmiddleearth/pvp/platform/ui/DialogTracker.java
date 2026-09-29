package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Knows who has a PvP dialog open, so the countdown can close them: a dialog does not pause a
 * multiplayer game, so none may stay open into a fight. Besides the button callbacks, quitting and changing world
 * are the two other clean signals that a dialog closed; a few other cases still leave this bookkeeping stale,
 * because nothing tells this plugin about them: another plugin's {@code showDialog} replacing ours, a death screen
 * replacing whatever was open, and a click arriving after Paper already dropped the expired callback behind it. In
 * each of these, a later {@code closeDialog()} either does nothing or closes a dialog that isn't the PvP one,
 * possibly just as a fight starts.
 */
public final class DialogTracker implements GameListener, Listener {

    /** Closes the open dialog of a player; a seam, because dialogs cannot exist in unit tests. */
    @FunctionalInterface
    interface Closer {
        void close(UUID player);
    }

    private final Set<UUID> open = new HashSet<>();
    private final Closer closer;

    public DialogTracker() {
        this(player -> {
            Player online = Bukkit.getPlayer(player);
            if (online != null) {
                online.closeDialog();
            }
        });
    }

    DialogTracker(Closer closer) {
        this.closer = closer;
    }

    /** A PvP dialog was shown to the player. */
    public void opened(UUID player) {
        open.add(player);
    }

    /**
     * The player's PvP dialog closed: every dialog carries DialogRenderer's CLOSE after-action, so any of its
     * buttons closes it. {@link #onWorldChange} calls this too.
     */
    public void closed(UUID player) {
        open.remove(player);
    }

    public boolean isOpen(UUID player) {
        return open.contains(player);
    }

    /** When a countdown starts, the players of that game lose the PvP dialog they had open. */
    @Override
    public void phaseChanged(Game game, Phase from, Phase to) {
        if (to != Phase.COUNTDOWN) {
            return;
        }
        for (PlayerSession player : game.players()) {
            if (open.remove(player.id())) {
                closer.close(player.id());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        open.remove(event.getPlayer().getUniqueId());
    }

    /** A dimension or world change closes the client's dialog with no callback to tell the server. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        closed(event.getPlayer().getUniqueId());
    }
}
