package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leaving or quitting a running game within 10 seconds (200 ticks) of being hurt by another player of the game
 * counts as a death, and as a kill for that player when they earn one. The recording listener does not tell a flee
 * from a death, so it lists a flee as "died".
 */
class LeavingUnderFireTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(String arenaId, String... names) {
        return running(PvpConfig.defaults(), arenaId, Map.of(), names);
    }

    private Game running(PvpConfig config, String arenaId, Map<String, ?> settings, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), config,
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3));
        Game game = engine.open(arenaId, settings);
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    private List<String> lastEvents(int count) {
        List<String> events = engine.events().events();
        return events.subList(events.size() - count, events.size());
    }

    private static String result(MatchRecord record, String name) {
        return record.participants().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow().result();
    }

    @Test
    void leavingWithinTenSecondsOfAHitCountsAsKilledByTheAttacker() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        engine.ticks(game, 200);
        int scoreChanges = engine.events().scoreChanges();

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        assertEquals(1, alice.deaths());
        assertEquals(1, bob.kills());
        assertEquals(1, bob.score());
        assertEquals(scoreChanges + 1, engine.events().scoreChanges());
        // The death is counted first, then the leave goes through as always.
        assertEquals(List.of("died Alice by Bob", "left Alice LEFT"), lastEvents(2));
        assertFalse(alice.present());
        assertTrue(engine.context(game).inLobby().contains(id("Alice")));
    }

    @Test
    void aHitMoreThanTenSecondsAgoCountsForNothing() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        engine.ticks(game, 201);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void quittingCountsLikeLeaving() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        engine.ticks(game, 20);

        engine.games().leave(id("Alice"), LeaveReason.QUIT);

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(1, engine.session(game, "Bob").kills());
        assertEquals(List.of("died Alice by Bob", "left Alice QUIT"), lastEvents(2));
    }

    @Test
    void goingForAnyOtherReasonCountsForNothing() {
        Game game = running("AS-FFA", "Alice", "Bob", "Carol");
        engine.hit(game, "Alice", "Bob");
        engine.hit(game, "Carol", "Bob");

        // Staff kicks and the kickall and lock sweeps take players out as KICKED, a closing game as GAME_CLOSED.
        engine.games().leave(id("Alice"), LeaveReason.KICKED);
        engine.games().leave(id("Carol"), LeaveReason.GAME_CLOSED);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Carol").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void leavingAGameThatEndedOrClosedCountsForNothing() {
        Game game = running("AS-FFA", "Alice", "Bob", "Carol");
        engine.hit(game, "Alice", "Bob");
        engine.hit(game, "Carol", "Bob");
        engine.games().stop(game);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.seconds(game, PvpConfig.defaults().endingSeconds());

        assertEquals(Phase.CLOSED, game.phase());
        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Carol").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void inTeamDeathmatchLeavingUnderFireIsOutAndCanEndTheGame() {
        Game game = running("AS-TDM", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        PlayerSession alice = engine.session(game, "Alice");
        String bobsTeam = engine.session(game, "Bob").team().orElseThrow().id();
        assertTrue(alice.eliminated());
        assertEquals(1, alice.deaths());
        assertEquals(1, engine.session(game, "Bob").kills());
        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team(bobsTeam, EndReason.WIN_CONDITION)), game.outcome());
        // The leave then goes through as leaving an ended game does.
        assertFalse(alice.present());
        assertEquals(Optional.of(LeaveReason.LEFT), alice.leftReason());
        assertTrue(engine.context(game).inLobby().contains(id("Alice")));
        assertTrue(engine.games().gameOf(id("Alice")).isEmpty());
        assertEquals(List.of("died Alice by Bob", "phase AS-TDM RUNNING->ENDING",
                "ended AS-TDM " + bobsTeam + " WIN_CONDITION", "left Alice LEFT"), lastEvents(4));
        MatchRecord record = engine.events().records().getFirst();
        assertEquals(MatchRecord.LOST, result(record, "Alice"));
        assertEquals(MatchRecord.WON, result(record, "Bob"));
    }

    @Test
    void aFleeThatReachesTheScoreGoalEndsTheGameAndTheLeaveStillGoesThrough() {
        Game game = running(PvpConfig.defaults(), "AS-FFA", Map.of("scoreGoal", 1), "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.player(id("Bob"), EndReason.WIN_CONDITION)), game.outcome());
        assertFalse(engine.session(game, "Alice").present());
        assertTrue(engine.context(game).inLobby().contains(id("Alice")));
        assertEquals("left Alice LEFT", engine.events().events().getLast());
    }

    @Test
    void aFleeThatClosesTheGameAtOnceLeavesThePlayerInTheLobbyAndNothingAfterTheClose() {
        Game game = running(TestEngine.config(7, 0), "AS-TDM", Map.of(), "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");

        assertDoesNotThrow(() -> engine.games().leave(id("Alice"), LeaveReason.QUIT));

        assertEquals(Phase.CLOSED, game.phase());
        assertFalse(engine.session(game, "Alice").present());
        assertTrue(engine.context(game).inLobby().contains(id("Alice")));
        assertTrue(engine.games().games().isEmpty());
        assertEquals("closed AS-TDM", engine.events().events().getLast());
        assertEquals(MatchRecord.LOST, result(engine.events().records().getFirst(), "Alice"));
    }

    @Test
    void aDeathForgetsTheHitSoQuittingFromTheDeathScreenCountsOnce() {
        Game game = running("AS-FFA", "Alice", "Bob", "Carol");
        engine.hit(game, "Alice", "Bob");
        engine.kill(game, "Alice", "Carol");

        engine.games().leave(id("Alice"), LeaveReason.QUIT);

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(List.of("died Alice by Carol"), engine.events().events("died"));
    }

    @Test
    void aHitBeforeADeathAndRespawnDoesNotCountALaterLeave() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        engine.kill(game, "Alice", "Bob");
        game.handleRespawn(id("Alice"));
        engine.ticks(game, 20);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(1, engine.session(game, "Bob").kills());
        assertEquals(List.of("died Alice by Bob"), engine.events().events("died"));
    }

    @Test
    void aRespawnForgetsTheHitToo() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        game.handleRespawn(id("Alice"));

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void aKickForgetsTheHitSoAPlayerWhoComesBackStartsWithoutOne() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.hit(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.KICKED);
        engine.join(game, "Alice");

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void anAttackerWhoHasLeftGetsNoKillButTheLeaverStillDies() {
        Game game = running("AS-FFA", "Alice", "Bob", "Carol");
        engine.hit(game, "Alice", "Bob");
        engine.games().leave(id("Bob"), LeaveReason.LEFT);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(0, engine.session(game, "Bob").score());
        assertEquals(List.of("died Alice"), engine.events().events("died"));
    }

    @Test
    void aLaterHitReplacesTheEarlierOne() {
        Game game = running("AS-FFA", "Alice", "Bob", "Carol");
        engine.hit(game, "Alice", "Bob");
        engine.ticks(game, 150);
        engine.hit(game, "Alice", "Carol");
        engine.ticks(game, 150);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(1, engine.session(game, "Carol").kills());
        assertEquals(List.of("died Alice by Carol"), engine.events().events("died"));
    }

    @Test
    void onlyAHitByAnotherPlayerOfTheGameCounts() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.spectate(game, "Carol");

        engine.hit(game, "Alice", "Carol");
        engine.hit(game, "Alice", "Alice");
        engine.hit(game, "Alice", "Dave");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void aSpectatorWhoIsHitAndLeavesCountsForNothing() {
        Game game = running("AS-FFA", "Alice", "Bob");
        engine.spectate(game, "Carol");
        engine.hit(game, "Carol", "Bob");

        engine.games().leave(id("Carol"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Carol").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void aHitBeforeTheGameRunsCountsForNothing() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.ffa("AS-FFA", 100, 3));
        Game game = engine.open("AS-FFA");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);
        engine.hit(game, "Alice", "Bob");
        engine.seconds(game, PvpConfig.defaults().countdownSeconds());
        assertEquals(Phase.RUNNING, game.phase());

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }
}
