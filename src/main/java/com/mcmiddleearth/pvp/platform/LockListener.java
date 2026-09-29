package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Enforces the lock: while PvP is locked, a player without {@code pvp.join.locked} who joins is told so and sent
 * to the main server; players who may stay see "Server Locked" on their action bar.
 */
public final class LockListener implements Listener {

    /** How long to keep checking for the proxy's channel before giving up (5 seconds). */
    private static final int PROXY_WAIT_TICKS = 100;

    private final PvpServices services;
    private final Plugin plugin;

    public LockListener(PvpServices services, Plugin plugin) {
        this.services = services;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!services.lock().keepsOut(player::hasPermission)) {
            return;
        }
        player.sendMessage(services.messages().get(Msg.ADMIN_LOCKED_NOTICE));
        // Paper only sends a plugin message on a channel the proxy has registered for the player, and Velocity
        // registers it a moment after the join, so a fixed one-tick wait could lose that race. This checks every
        // tick instead, until the channel is ready or the wait times out.
        new BukkitRunnable() {
            private int waited;

            @Override
            public void run() {
                if (!player.isOnline() || !services.lock().locked()) {
                    cancel();
                    return;
                }
                if (services.proxy().ready(player)) {
                    services.proxy().sendToMainServer(player);
                    cancel();
                    return;
                }
                waited++;
                if (waited >= PROXY_WAIT_TICKS) {
                    plugin.getLogger().warning(player.getName() + " could not be sent to the main server: the "
                            + "proxy never registered the BungeeCord channel for them. " + ProxyMessenger.CHANNEL_HINT);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    /** Starts the once-a-second "Server Locked" action bar for players with {@code pvp.join.locked}. */
    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::showLocked, 20, 20);
    }

    private void showLocked() {
        if (!services.lock().locked()) {
            return;
        }
        Component bar = services.messages().get(Msg.HUD_ACTIONBAR_LOCKED);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission(Permissions.JOIN_LOCKED)) {
                continue;
            }
            // While a game runs, the HUD owns its players' action bar; the lock stays visible in /pvpadmin.
            // The reminder still reaches the lobby and games that are WAITING, in COUNTDOWN or ENDING.
            boolean inRunningGame = services.games().gameOf(player.getUniqueId())
                    .map(Game::phase)
                    .filter(Phase.RUNNING::equals)
                    .isPresent();
            if (!inRunningGame) {
                player.sendActionBar(bar);
            }
        }
    }
}
