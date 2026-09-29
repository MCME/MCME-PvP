package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.GameContext;
import com.mcmiddleearth.pvp.game.GameContextFactory;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creates the Paper context of each new game: its own scoreboard, task list, player and world actions. It keeps
 * each game's scoreboard until the game's context is disposed, so the HUD can draw on it.
 */
public final class PaperGameContextFactory implements GameContextFactory {

    private final Plugin plugin;
    private final PaperLobby lobby;
    private final GameStateMarker marker;
    private final Map<GameId, GameScoreboard> scoreboards = new HashMap<>();

    public PaperGameContextFactory(Plugin plugin, PaperLobby lobby) {
        this.plugin = plugin;
        this.lobby = lobby;
        this.marker = new GameStateMarker(plugin);
    }

    /**
     * @throws PvpException when the arena's world isn't loaded, before anything is built: every teleport would
     *                       otherwise silently do nothing and the game would be played wherever the players stand
     */
    @Override
    public GameContext create(GameId id, Arena arena) {
        if (plugin.getServer().getWorld(arena.world()) == null) {
            throw new PvpException(Msg.ERROR_ARENA_UNASSIGNED,
                    Map.of("arena", arena.id(), "reason", "world " + arena.world() + " is not loaded"));
        }
        Server server = plugin.getServer();
        GameScoreboard scoreboard = new GameScoreboard(server.getScoreboardManager());
        scoreboards.put(id, scoreboard);
        return new PaperGameContext(
                new PaperPlayerActions(server, lobby, scoreboard, marker),
                new PaperGameTasks(plugin),
                new PaperWorldActions(server),
                () -> {
                    scoreboards.remove(id);
                    scoreboard.dispose();
                });
    }

    /** The scoreboard of an open game; empty once the game's context is disposed. */
    public Optional<GameScoreboard> scoreboard(GameId id) {
        return Optional.ofNullable(scoreboards.get(id));
    }

    /** Marks and clears a player's saved game state; shared with {@link CrashRecovery}. */
    public GameStateMarker marker() {
        return marker;
    }
}
