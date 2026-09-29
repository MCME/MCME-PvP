package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameContextFactory;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.mode.ModeRegistry;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link GameManager} on fake contexts with a {@link RecordingListener}, for scenario tests. Players are named;
 * their uuids come from {@link TestArenas#player}. Lifetime K/D ("skill") is 0 unless set with {@link #skill}.
 */
public final class TestEngine {

    private final Map<GameId, FakeGameContext> contexts = new LinkedHashMap<>();
    private final Map<UUID, Double> skills = new HashMap<>();
    private final RecordingListener events = new RecordingListener();
    private final PvpConfig config;
    private final GameManager games;

    public TestEngine(Path dir, ModeRegistry modes, PvpConfig config, Arena... arenas) {
        this.config = config;
        GameContextFactory factory = (id, arena) -> {
            FakeGameContext context = new FakeGameContext();
            contexts.put(id, context);
            return context;
        };
        this.games = new GameManager(TestArenas.registry(dir, arenas), modes, config, factory, events,
                player -> skills.getOrDefault(player, 0.0));
    }

    /** config.yml's defaults, with other countdown and ending lengths. */
    public static PvpConfig config(int countdownSeconds, int endingSeconds) {
        PvpConfig d = PvpConfig.defaults();
        return new PvpConfig(d.lobby(), d.worlds(), d.mainServer(), d.startLocked(), d.lavaDamage(),
                d.lobbyProtection(), countdownSeconds, endingSeconds, d.modeDefaults());
    }

    public static UUID id(String name) {
        return TestArenas.player(name).id();
    }

    public GameManager games() {
        return games;
    }

    public RecordingListener events() {
        return events;
    }

    public FakeGameContext context(Game game) {
        return contexts.get(game.id());
    }

    public Game open(String arenaId) {
        return open(arenaId, Map.of());
    }

    public Game open(String arenaId, Map<String, ?> settings) {
        return games.open(arenaId, settings, "Staff");
    }

    /** Each player joins alone, in the given order. */
    public void join(Game game, String... names) {
        for (String name : names) {
            games.join(JoinUnit.solo(TestArenas.player(name)), game);
        }
    }

    /** The players join together as one party. */
    public void party(Game game, String... names) {
        List<PlayerRef> members = Arrays.stream(names).map(TestArenas::player).toList();
        games.join(JoinUnit.party(members), game);
    }

    public void spectate(Game game, String name) {
        games.spectate(TestArenas.player(name), game);
    }

    public void skill(String name, double killDeathRatio) {
        skills.put(id(name), killDeathRatio);
    }

    /** Starts the countdown and lets it run out, so the game is RUNNING. */
    public void start(Game game) {
        games.startCountdown(game);
        seconds(game, config.countdownSeconds());
    }

    public void seconds(Game game, int seconds) {
        context(game).advanceSeconds(seconds);
    }

    public PlayerSession session(Game game, String name) {
        return game.session(id(name)).orElseThrow();
    }

    /** {@code killer} (nullable) kills {@code victim}. */
    public void kill(Game game, String victim, String killer) {
        game.handleDeath(id(victim), killer == null ? null : id(killer));
    }

    /** {@code attacker} hurts {@code victim}, in damage the game allows. */
    public void hit(Game game, String victim, String attacker) {
        game.handleHit(id(victim), id(attacker));
    }

    public void ticks(Game game, long ticks) {
        context(game).advanceTicks(ticks);
    }
}
