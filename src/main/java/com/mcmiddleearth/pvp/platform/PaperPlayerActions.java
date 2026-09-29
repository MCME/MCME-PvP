package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.PlayerActions;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.team.TeamSpec;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * What one game does with its players on Paper. A player counts as online while the server still has the player
 * object, which includes the quit event: a game can still reset a player who is leaving.
 */
public final class PaperPlayerActions implements PlayerActions {

    private final Server server;
    private final PaperLobby lobby;
    private final GameScoreboard scoreboard;
    private final GameStateMarker marker;

    public PaperPlayerActions(Server server, PaperLobby lobby, GameScoreboard scoreboard, GameStateMarker marker) {
        this.server = server;
        this.lobby = lobby;
        this.scoreboard = scoreboard;
        this.marker = marker;
    }

    /**
     * A dead player isn't moved: Paper refuses the teleport, so the respawn handler places them through
     * {@code Game.respawnPoint} instead.
     */
    @Override
    public void teleport(UUID player, NamedPoint point) {
        with(player, online -> Locations.location(point).ifPresent(online::teleport));
    }

    @Override
    public void resetForPlay(UUID player) {
        with(player, online -> {
            lobby.reset(online);
            online.setFireTicks(0);
            online.setFallDistance(0);
            marker.mark(online);
        });
    }

    @Override
    public void giveKit(UUID player, Kit kit, TeamSpec color) {
        with(player, online -> KitItems.give(online, kit, color));
    }

    @Override
    public void setSpectator(UUID player, boolean spectator) {
        with(player, online -> {
            online.setGameMode(spectator ? GameMode.SPECTATOR : GameMode.ADVENTURE);
            if (spectator) {
                marker.mark(online);
            }
        });
    }

    @Override
    public void joinScoreboard(UUID player, TeamSpec color) {
        with(player, online -> scoreboard.add(online, color));
    }

    @Override
    public void leaveScoreboard(UUID player) {
        with(player, scoreboard::remove);
    }

    @Override
    public void sendToLobby(UUID player) {
        with(player, online -> {
            lobby.reset(online);
            marker.clear(online);
        });
        lobby.send(player);
    }

    private void with(UUID player, Consumer<Player> action) {
        Player online = server.getPlayer(player);
        if (online != null) {
            action.accept(online);
        }
    }
}
