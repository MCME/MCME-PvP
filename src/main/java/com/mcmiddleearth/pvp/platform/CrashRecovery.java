package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * A player file saved during a game keeps its game state if the server stops without closing the game: an
 * eliminated player or a spectator would rejoin in SPECTATOR mode and fly through walls, and any other marked
 * player would rejoin still standing in the arena with the kit. Nothing else resets them, since a game only
 * resets players on its own exits and this plugin never otherwise touches a player outside a game.
 */
public final class CrashRecovery implements Listener {

    private final PvpServices services;
    private final PaperLobby lobby;
    private final GameStateMarker marker;

    public CrashRecovery(PvpServices services, PaperLobby lobby, GameStateMarker marker) {
        this.services = services;
        this.lobby = lobby;
        this.marker = marker;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!marker.marked(player) || services.games().gameOf(player.getUniqueId()).isPresent()) {
            return;
        }
        lobby.reset(player);
        lobby.send(player.getUniqueId());
        marker.clear(player);
    }
}
