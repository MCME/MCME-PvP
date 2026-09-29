package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;

import java.util.UUID;

/**
 * A player who is in no game respawns at the PvP lobby (1.1.2) after dying, instead of the vanilla bed or world
 * spawn. Players in a game are left to {@link GameEventRouter}. Left alone when the lobby's world isn't loaded;
 * {@code PvpPlugin} already warns about that at startup. LOW priority, so this is just a default that another
 * plugin on pvpserver can still override; the only excluded reason is END_PORTAL, the one respawn that doesn't
 * follow a death.
 */
public final class LobbyRespawn implements Listener {

    private final GameManager games;
    private final PaperLobby lobby;

    public LobbyRespawn(PvpServices services, PaperLobby lobby) {
        this.games = services.games();
        this.lobby = lobby;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() == RespawnReason.END_PORTAL) {
            return;
        }
        UUID player = event.getPlayer().getUniqueId();
        if (games.gameOf(player).isEmpty()) {
            lobby.location().ifPresent(event::setRespawnLocation);
        }
    }
}
