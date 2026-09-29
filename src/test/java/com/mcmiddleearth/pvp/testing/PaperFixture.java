package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.arena.LegacyMapImporter;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListeners;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.ffa.FreeForAll;
import com.mcmiddleearth.pvp.mode.tdm.TeamDeathmatch;
import com.mcmiddleearth.pvp.platform.PaperGameContextFactory;
import com.mcmiddleearth.pvp.platform.PaperLobby;
import com.mcmiddleearth.pvp.platform.ServerLock;
import com.mcmiddleearth.pvp.stats.JsonStatsStore;
import com.mcmiddleearth.pvp.stats.LegacyStatsImporter;
import com.mcmiddleearth.pvp.stats.StatsService;
import com.mcmiddleearth.pvp.storage.AsyncWriter;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.text.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * The services of {@code PvpPlugin}, built by hand on MockBukkit for the platform and command tests: TDM and FFA,
 * the given arenas in {@code dir/arenas}, stats written synchronously under {@code dir}, the lock open, and a
 * proxy that records its messages (MockBukkit drops plugin messages). Listeners are registered by each test.
 */
public final class PaperFixture {

    /** One plugin message: the player it went through and its UTF strings, e.g. [ConnectOther, Alice, world]. */
    public record ProxyMessage(String through, List<String> parts) {
    }

    private final ServerMock server;
    private final PluginMock plugin;
    private final PvpServices services;
    private final PaperGameContextFactory contexts;
    private final GameListeners listeners;
    private final List<ProxyMessage> proxyMessages;
    private final PaperLobby lobby;

    private PaperFixture(ServerMock server, PluginMock plugin, PvpServices services, PaperGameContextFactory contexts,
                         GameListeners listeners, List<ProxyMessage> proxyMessages, PaperLobby lobby) {
        this.server = server;
        this.plugin = plugin;
        this.services = services;
        this.contexts = contexts;
        this.listeners = listeners;
        this.proxyMessages = proxyMessages;
        this.lobby = lobby;
    }

    public static PaperFixture create(ServerMock server, Path dir, Arena... arenas) {
        return create(server, dir, PvpConfig.defaults(), arenas);
    }

    public static PaperFixture create(ServerMock server, Path dir, PvpConfig config, Arena... arenas) {
        return create(server, dir, config, List.of(), arenas);
    }

    /** With {@code extraModes} (modes made for a test) registered next to TDM and FFA. */
    public static PaperFixture create(ServerMock server, Path dir, PvpConfig config, List<GameMode> extraModes,
                                      Arena... arenas) {
        for (String world : worlds(config, arenas)) {
            if (server.getWorld(world) == null) {
                server.addSimpleWorld(world);
            }
        }
        PluginMock plugin = MockBukkit.createMockPlugin("PvpFixture");
        Logger logger = plugin.getLogger();
        List<GameMode> allModes = new ArrayList<>(List.of(new TeamDeathmatch(), new FreeForAll()));
        allModes.addAll(extraModes);
        ModeRegistry modes = new ModeRegistry(allModes);
        ArenaRegistry arenaRegistry = registry(dir, modes, logger, arenas);
        AsyncWriter writer = new AsyncWriter(Runnable::run, logger);
        StatsService stats = new StatsService(
                new JsonStatsStore(dir.resolve("players"), Json.pretty(), writer, logger),
                new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), logger));
        PaperLobby lobby = new PaperLobby(config.lobby());
        PaperGameContextFactory contexts = new PaperGameContextFactory(plugin, lobby);
        GameListeners listeners = new GameListeners(logger);
        GameManager games = new GameManager(arenaRegistry, modes, config, contexts, listeners, id -> 0.0);
        List<ProxyMessage> sent = new ArrayList<>();
        ProxyMessenger proxy = new ProxyMessenger(plugin, config.mainServer(),
                (player, payload) -> sent.add(new ProxyMessage(player.getName(), decode(payload))));
        PvpServices services = new PvpServices(config, Messages.bundled(), arenaRegistry, modes, games, stats, lobby,
                new ServerLock(false), proxy);
        return new PaperFixture(server, plugin, services, contexts, listeners, sent, lobby);
    }

    /** The shipped defaults with two switches changed. */
    public static PvpConfig config(boolean lavaDamage, boolean lobbyProtection) {
        PvpConfig d = PvpConfig.defaults();
        return new PvpConfig(d.lobby(), d.worlds(), d.mainServer(), d.startLocked(), lavaDamage, lobbyProtection,
                d.countdownSeconds(), d.endingSeconds(), d.modeDefaults());
    }

    public ServerMock server() {
        return server;
    }

    /** The owner of the game tasks and of the listeners the tests register. */
    public PluginMock plugin() {
        return plugin;
    }

    public PvpServices services() {
        return services;
    }

    public PaperGameContextFactory contexts() {
        return contexts;
    }

    public PaperLobby lobby() {
        return lobby;
    }

    /** The listeners every game reports to; tests add announcers here. */
    public GameListeners listeners() {
        return listeners;
    }

    /** Every plugin message sent so far, oldest first. */
    public List<ProxyMessage> proxyMessages() {
        return proxyMessages;
    }

    public void register(Listener listener) {
        server.getPluginManager().registerEvents(listener, plugin);
    }

    public PlayerMock player(String name) {
        return server.addPlayer(name);
    }

    public static PlayerRef ref(Player player) {
        return new PlayerRef(player.getUniqueId(), player.getName());
    }

    /** Opens a game with the arena's defaults, as staff member "Tester". */
    public Game open(String arena) {
        return services.games().open(arena, Map.of(), "Tester");
    }

    public void join(Game game, Player... players) {
        for (Player player : players) {
            services.games().join(JoinUnit.solo(ref(player)), game);
        }
    }

    /** Starts the countdown and runs the server until the game is RUNNING (at most countdown + 2 seconds). */
    public void startAndRun(Game game) {
        services.games().startCountdown(game);
        runUntil(game, Phase.RUNNING, services.config().countdownSeconds() + 2);
    }

    /** Runs the server tick by tick until the game reaches {@code phase}; fails after {@code maxSeconds}. */
    public void runUntil(Game game, Phase phase, int maxSeconds) {
        for (int tick = 0; tick < maxSeconds * 20 && game.phase() != phase; tick++) {
            server.getScheduler().performOneTick();
        }
        if (game.phase() != phase) {
            throw new AssertionError("game on " + game.arena().id() + " is " + game.phase() + ", not " + phase);
        }
    }

    public void runSeconds(int seconds) {
        server.getScheduler().performTicks(seconds * 20L);
    }

    /** Drains the chat queue of a player or the console, as plain text. */
    public static List<String> chat(MessageTarget target) {
        List<String> lines = new ArrayList<>();
        for (Component line = target.nextComponentMessage(); line != null; line = target.nextComponentMessage()) {
            lines.add(plain(line));
        }
        return lines;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Saves the arenas under dir/arenas and loads a registry that imports legacy maps from dir/maps. */
    private static ArenaRegistry registry(Path dir, ModeRegistry modes, Logger logger, Arena... arenas) {
        ArenaStore store = new ArenaStore(dir.resolve("arenas"), Json.pretty());
        try {
            for (Arena arena : arenas) {
                store.save(arena);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Function<String, Optional<String>> modeFor = gmType -> modes.byLegacyName(gmType).map(GameMode::id);
        ArenaRegistry registry = new ArenaRegistry(store, new LegacyMapImporter(modeFor, Clock.systemUTC()),
                dir.resolve("maps"), modeFor, logger);
        registry.load();
        return registry;
    }

    /** The lobby's world and every arena's world, each once. */
    private static List<String> worlds(PvpConfig config, Arena... arenas) {
        List<String> worlds = new ArrayList<>(List.of(config.lobby().world()));
        for (Arena arena : arenas) {
            if (!worlds.contains(arena.world())) {
                worlds.add(arena.world());
            }
        }
        return worlds;
    }

    private static List<String> decode(byte[] payload) {
        List<String> parts = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            while (in.available() > 0) {
                parts.add(in.readUTF());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return parts;
    }
}
