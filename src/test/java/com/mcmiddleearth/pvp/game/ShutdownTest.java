package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plugin disable: closeAll records running games, then closes every game at once. */
class ShutdownTest {

    private static final String ARENA_A = "AS-TDM-A";
    private static final String ARENA_B = "AS-TDM-B";

    @TempDir
    Path dir;

    @Test
    void closeAllRecordsRunningGamesAndClosesEverything() {
        TestEngine engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3));
        Game running = engine.open("AS-TDM");
        engine.join(running, "Alice", "Bob");
        engine.start(running);
        Game waiting = engine.open("AS-FFA");
        engine.join(waiting, "Carol");

        engine.games().closeAll();

        assertEquals(Phase.CLOSED, running.phase());
        assertEquals(Phase.CLOSED, waiting.phase());
        assertEquals(1, engine.events().records().size());
        assertEquals("shutdown", engine.events().records().get(0).endReason());
        assertEquals(List.of(), engine.games().games());
        assertEquals(Optional.of(LeaveReason.SHUTDOWN), engine.session(waiting, "Carol").leftReason());
        for (Game game : List.of(running, waiting)) {
            FakeGameContext context = engine.context(game);
            assertEquals(0, context.tasks().activeCount());
            assertTrue(context.disposed());
        }
        assertTrue(engine.context(running).inLobby().containsAll(List.of(id("Alice"), id("Bob"))));
        assertTrue(engine.context(waiting).inLobby().contains(id("Carol")));
    }

    @Test
    void closeAllClosesEveryGameEvenWhenOneFailsToClose() {
        FakeGameContext failingFake = new FakeGameContext();
        FakeGameContext healthyFake = new FakeGameContext();
        GameContextFactory factory = (id, arena) -> arena.id().equals("AS-TDM")
                ? new FailingDisposeContext(failingFake)
                : healthyFake;
        GameManager manager = new GameManager(
                TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3)),
                TestArenas.modes(), PvpConfig.defaults(), factory, new GameListener() {
        }, player -> 0.0);
        Game failing = manager.open("AS-TDM", Map.of(), "Staff");
        Game healthy = manager.open("AS-FFA", Map.of(), "Staff");

        RuntimeException thrown = assertThrows(RuntimeException.class, manager::closeAll);

        assertEquals("dispose failed", thrown.getMessage());
        assertEquals(Phase.CLOSED, failing.phase());
        assertEquals(Phase.CLOSED, healthy.phase());
        assertEquals(List.of(), manager.games());
        assertEquals(0, failingFake.tasks().activeCount());
        assertEquals(0, healthyFake.tasks().activeCount());
        assertTrue(healthyFake.disposed());
    }

    @Test
    void closeAllEndsEveryRunningGameEvenWhenOneListenerThrows() {
        ThrowingListener listener = new ThrowingListener(ARENA_A);
        TwoGames games = twoRunningGames(listener);

        RuntimeException caught = assertThrows(RuntimeException.class, games.manager()::closeAll);

        assertSame(listener.thrownFor(ARENA_A), caught);
        assertEquals("shutdown", listener.recordFor(ARENA_B).endReason());
        assertEquals(Phase.CLOSED, games.a().phase());
        assertEquals(Phase.CLOSED, games.b().phase());
        assertEquals(List.of(), games.manager().games());
    }

    @Test
    void closeAllKeepsTheFirstListenerFailureAndSuppressesTheRest() {
        ThrowingListener listener = new ThrowingListener(ARENA_A, ARENA_B);
        TwoGames games = twoRunningGames(listener);

        RuntimeException caught = assertThrows(RuntimeException.class, games.manager()::closeAll);

        assertSame(listener.thrownFor(ARENA_A), caught);
        assertEquals(1, caught.getSuppressed().length);
        assertSame(listener.thrownFor(ARENA_B), caught.getSuppressed()[0]);
        assertEquals(Phase.CLOSED, games.a().phase());
        assertEquals(Phase.CLOSED, games.b().phase());
        assertEquals(List.of(), games.manager().games());
    }

    /** Two RUNNING TDM games (Alice+Bob on {@link #ARENA_A}, Carol+Dave on {@link #ARENA_B}) on one manager. */
    private TwoGames twoRunningGames(GameListener listener) {
        FakeGameContext contextA = new FakeGameContext();
        FakeGameContext contextB = new FakeGameContext();
        GameContextFactory factory = (id, arena) -> arena.id().equals(ARENA_A) ? contextA : contextB;
        PvpConfig config = PvpConfig.defaults();
        GameManager manager = new GameManager(
                TestArenas.registry(dir, TestArenas.tdm(ARENA_A, 0, 2), TestArenas.tdm(ARENA_B, 100, 2)),
                TestArenas.modes(), config, factory, listener, player -> 0.0);
        Game a = manager.open(ARENA_A, Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), a);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), a);
        manager.startCountdown(a);
        contextA.advanceSeconds(config.countdownSeconds());
        Game b = manager.open(ARENA_B, Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Carol")), b);
        manager.join(JoinUnit.solo(TestArenas.player("Dave")), b);
        manager.startCountdown(b);
        contextB.advanceSeconds(config.countdownSeconds());
        assertEquals(Phase.RUNNING, a.phase());
        assertEquals(Phase.RUNNING, b.phase());
        return new TwoGames(manager, a, b);
    }

    private record TwoGames(GameManager manager, Game a, Game b) {
    }

    /** A listener whose {@code gameEnded} records the match record, then throws for the named arenas. */
    private static final class ThrowingListener implements GameListener {

        private final Set<String> failingArenas;
        private final Map<String, MatchRecord> records = new HashMap<>();
        private final Map<String, RuntimeException> thrown = new HashMap<>();

        ThrowingListener(String... failingArenas) {
            this.failingArenas = Set.of(failingArenas);
        }

        @Override
        public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
            String arenaId = game.arena().id();
            records.put(arenaId, record);
            if (failingArenas.contains(arenaId)) {
                RuntimeException failure = new RuntimeException("gameEnded failed for " + arenaId);
                thrown.put(arenaId, failure);
                throw failure;
            }
        }

        MatchRecord recordFor(String arenaId) {
            return records.get(arenaId);
        }

        RuntimeException thrownFor(String arenaId) {
            return thrown.get(arenaId);
        }
    }

    /** Delegates everything to a shared fake context except {@code dispose()}, which always throws. */
    private record FailingDisposeContext(GameContext delegate) implements GameContext {

        @Override
        public PlayerActions players() {
            return delegate.players();
        }

        @Override
        public GameTasks tasks() {
            return delegate.tasks();
        }

        @Override
        public WorldActions world() {
            return delegate.world();
        }

        @Override
        public Clock clock() {
            return delegate.clock();
        }

        @Override
        public void dispose() {
            throw new RuntimeException("dispose failed");
        }
    }
}
