package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.PvpPlugin;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.storage.Json;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The real {@link PvpPlugin} on MockBukkit, for end-to-end tests: the given arenas saved in its data folder and
 * loaded, PvP unlocked (config.yml starts locked), and the Brigadier commands built by a first dispatch.
 */
public final class PluginFixture {

    private PluginFixture() {
    }

    public static PvpPlugin load(ServerMock server, Arena... arenas) {
        if (server.getWorld("world") == null) {
            server.addSimpleWorld("world");
        }
        PvpPlugin plugin = MockBukkit.load(PvpPlugin.class);
        ArenaStore store = new ArenaStore(plugin.getDataFolder().toPath().resolve("arenas"), Json.pretty());
        try {
            for (Arena arena : arenas) {
                store.save(arena);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        plugin.services().arenas().load();
        plugin.services().lock().unlock();
        // Paper builds Brigadier commands on the first dispatch.
        server.dispatchCommand(server.getConsoleSender(), "pvp-warm-up");
        return plugin;
    }

    /** Runs the server tick by tick until the game reaches {@code phase}; fails after {@code maxSeconds}. */
    public static void runUntil(ServerMock server, Game game, Phase phase, int maxSeconds) {
        for (int tick = 0; tick < maxSeconds * 20 && game.phase() != phase; tick++) {
            server.getScheduler().performOneTick();
        }
        if (game.phase() != phase) {
            throw new AssertionError("game on " + game.arena().id() + " is " + game.phase() + ", not " + phase);
        }
    }
}
