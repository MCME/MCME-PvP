package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListeners;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.platform.ServerLock;
import com.mcmiddleearth.pvp.stats.JsonStatsStore;
import com.mcmiddleearth.pvp.stats.LegacyStatsImporter;
import com.mcmiddleearth.pvp.stats.StatsService;
import com.mcmiddleearth.pvp.storage.AsyncWriter;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.Viewer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The services of the plugin without a server, for the view tests: a real {@link GameManager} on
 * {@link FakeGameContext}s, the bundled messages, stats in a temporary folder, and a lobby that only records who was
 * sent there. Extra game listeners (a dialog tracker, say) are added to {@link #listeners}.
 */
public final class UiFixture {

    public final List<FakeGameContext> contexts = new ArrayList<>();
    public final GameListeners listeners = new GameListeners(Logger.getLogger("UiFixture"));
    public final List<UUID> sentToLobby = new ArrayList<>();
    public final PvpServices services;

    public UiFixture(Path dir, Arena... arenas) {
        this(dir, TestArenas.modes(), arenas);
    }

    /** With other modes than Team Deathmatch and Free For All, to show that screens need no UI code per mode. */
    public UiFixture(Path dir, ModeRegistry modes, Arena... arenas) {
        Logger logger = Logger.getLogger("UiFixture");
        ArenaRegistry registry = TestArenas.registry(dir, arenas);
        PvpConfig config = PvpConfig.defaults();
        GameManager games = new GameManager(registry, modes, config, FakeGameContext.factory(contexts), listeners,
                id -> 0.0);
        StatsService stats = new StatsService(
                new JsonStatsStore(dir.resolve("players"), Json.pretty(),
                        new AsyncWriter(Runnable::run, logger), logger),
                new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), logger));
        // Nothing here sends plugin messages, so the proxy messenger needs no plugin.
        services = new PvpServices(config, Messages.bundled(), registry, modes, games, stats, sentToLobby::add,
                new ServerLock(false), new ProxyMessenger(null, config.mainServer()));
    }

    public GameManager games() {
        return services.games();
    }

    /** A player with the default permission only; the uuid matches {@link TestArenas#player(String)}. */
    public Viewer player(String name) {
        PlayerRef ref = TestArenas.player(name);
        return new Viewer(ref.id(), ref.name(), Permissions.PLAY::equals);
    }

    /** A staff member with every permission. */
    public Viewer staff(String name) {
        PlayerRef ref = TestArenas.player(name);
        return new Viewer(ref.id(), ref.name(), node -> true);
    }

    public Game open(String arenaId) {
        return open(arenaId, Map.of());
    }

    public Game open(String arenaId, Map<String, ?> settings) {
        return services.games().open(arenaId, settings, "Console");
    }

    public void join(Game game, Viewer... viewers) {
        for (Viewer viewer : viewers) {
            services.games().join(JoinUnit.solo(viewer.ref()), game);
        }
    }

    /**
     * Runs the tasks of every game for {@code seconds}, one game after another, not tick by tick together; a test
     * that depends on how two games interleave must step them itself.
     */
    public void advanceSeconds(int seconds) {
        for (FakeGameContext context : List.copyOf(contexts)) {
            context.advanceSeconds(seconds);
        }
    }
}
