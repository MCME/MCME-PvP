package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Every ending produces a match record with the right participants and results. */
class MatchRecordsTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(String arenaId, Map<String, ?> settings, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3));
        Game game = engine.open(arenaId, settings);
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    private MatchRecord record() {
        assertEquals(1, engine.events().records().size());
        return engine.events().records().get(0);
    }

    private static List<String> names(MatchRecord record) {
        return record.participants().stream().map(MatchRecord.Participant::name).toList();
    }

    private static MatchRecord.Participant participant(MatchRecord record, String name) {
        return record.participants().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void aWinRecordsTheTeamsTheResultsAndTheTimes() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.seconds(game, 30);
        engine.kill(game, "Alice", "Bob");
        engine.kill(game, "Carol", "Bob");

        MatchRecord record = record();

        assertEquals(1, record.schemaVersion());
        assertEquals(game.id().value().toString(), record.gameId());
        assertEquals("tdm", record.mode());
        assertEquals("AS-TDM", record.arena());
        assertEquals(FakeGameContext.START.plusSeconds(7).toString(), record.startedAt());
        assertEquals(FakeGameContext.START.plusSeconds(37).toString(), record.endedAt());
        assertEquals(Map.of("maxPlayers", 16, "midgameJoin", true, "teamCount", 2), record.settings());
        assertEquals(List.of(
                new MatchRecord.TeamEntry("red", List.of(id("Alice").toString(), id("Carol").toString())),
                new MatchRecord.TeamEntry("blue", List.of(id("Bob").toString(), id("Dave").toString()))),
                record.teams());
        assertEquals(List.of("Alice", "Bob", "Carol", "Dave"), names(record));
        assertEquals(new MatchRecord.Participant(id("Bob").toString(), "Bob", "blue", 2, 0, 0, MatchRecord.WON),
                participant(record, "Bob"));
        assertEquals(new MatchRecord.Participant(id("Alice").toString(), "Alice", "red", 0, 1, 0, MatchRecord.LOST),
                participant(record, "Alice"));
        assertEquals("blue", record.winner());
        assertEquals("win_condition", record.endReason());
    }

    @Test
    void aTimeLimitRecordNamesTheWinningPlayer() {
        Game game = running("AS-FFA", Map.of("timeLimit", 1), "Alice", "Bob");
        engine.kill(game, "Bob", "Alice");

        engine.seconds(game, 60);
        MatchRecord record = record();

        assertEquals(id("Alice").toString(), record.winner());
        assertEquals("time_limit", record.endReason());
        assertEquals(List.of("ended AS-FFA Alice TIME_LIMIT"), engine.events().events("ended"));
        assertEquals(List.of(), record.teams());
        assertEquals(new MatchRecord.Participant(id("Alice").toString(), "Alice", null, 1, 0, 1, MatchRecord.WON),
                participant(record, "Alice"));
        assertEquals(MatchRecord.LOST, participant(record, "Bob").result());
        assertEquals(Map.of("maxPlayers", 16, "midgameJoin", true, "scoreGoal", 0, "timeLimit", 1), record.settings());
    }

    @Test
    void aStoppedGameIsADrawForEveryone() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob");

        engine.games().stop(game);
        MatchRecord record = record();

        assertNull(record.winner());
        assertEquals("stopped", record.endReason());
        assertEquals(List.of(MatchRecord.DRAW, MatchRecord.DRAW),
                record.participants().stream().map(MatchRecord.Participant::result).toList());
    }

    @Test
    void anAbandonedGameListsEveryoneAsLeft() {
        Game game = running("AS-FFA", Map.of(), "Alice", "Bob");

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.games().leave(id("Bob"), LeaveReason.QUIT);
        MatchRecord record = record();

        assertEquals("abandoned", record.endReason());
        assertEquals(List.of(MatchRecord.LEFT, MatchRecord.LEFT),
                record.participants().stream().map(MatchRecord.Participant::result).toList());
    }

    @Test
    void aParticipantWhoLeftIsListedAsLeftAndSpectatorsAreNotListed() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.spectate(game, "Erin");
        engine.games().leave(id("Carol"), LeaveReason.KICKED);

        engine.kill(game, "Alice", "Bob");
        MatchRecord record = record();

        assertEquals(List.of("Alice", "Bob", "Carol", "Dave"), names(record));
        assertEquals(MatchRecord.LEFT, participant(record, "Carol").result());
        assertEquals("red", participant(record, "Carol").team());
        assertEquals(MatchRecord.WON, participant(record, "Dave").result());
    }

    @Test
    void anEliminatedPlayerWhoComesBackToWatchGetsHerTeamsResult() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.kill(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        engine.join(game, "Alice");
        assertEquals(Role.SPECTATOR, engine.session(game, "Alice").role());

        engine.kill(game, "Carol", "Bob");
        MatchRecord record = record();

        assertEquals(new MatchRecord.Participant(id("Alice").toString(), "Alice", "red", 0, 1, 0, MatchRecord.LOST),
                participant(record, "Alice"));
        assertEquals(List.of(id("Alice").toString(), id("Carol").toString()),
                record.teams().stream().filter(team -> team.id().equals("red")).findFirst().orElseThrow().members());
    }

    @Test
    void anEliminatedPlayerWhoLeavesGetsHerTeamsLoss() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.kill(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        engine.kill(game, "Carol", "Bob");
        MatchRecord record = record();

        assertEquals("blue", record.winner());
        assertEquals(new MatchRecord.Participant(id("Alice").toString(), "Alice", "red", 0, 1, 0, MatchRecord.LOST),
                participant(record, "Alice"));
    }

    @Test
    void anEliminatedPlayerWhoQuitGetsHerTeamsWinWhenItWinsAfterwards() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.kill(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.QUIT);

        engine.kill(game, "Bob", "Carol");
        engine.kill(game, "Dave", "Carol");
        MatchRecord record = record();

        assertEquals("red", record.winner());
        assertEquals(MatchRecord.WON, participant(record, "Alice").result());
        assertEquals(MatchRecord.WON, participant(record, "Carol").result());
        assertEquals(MatchRecord.LOST, participant(record, "Bob").result());
    }

    @Test
    void aParticipantWhoComesBackOnlyToWatchWithoutBeingEliminatedIsListedAsLeft() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave", "Erin", "Frank");
        // Red (Alice, Carol, Erin) is below its start, so it can't take Alice back once she leaves.
        engine.kill(game, "Carol", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.join(game, "Alice");
        assertEquals(Role.SPECTATOR, engine.session(game, "Alice").role());

        engine.kill(game, "Erin", "Bob");
        MatchRecord record = record();

        assertEquals("blue", record.winner());
        assertEquals(MatchRecord.LEFT, participant(record, "Alice").result());
        assertEquals("red", participant(record, "Alice").team());
    }

    @Test
    void aLeaverOnTheWinningTeamIsLeftNotWon() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.games().leave(id("Dave"), LeaveReason.KICKED);

        engine.kill(game, "Alice", "Bob");
        engine.kill(game, "Carol", "Bob");
        MatchRecord record = record();

        assertEquals("blue", record.winner());
        assertEquals(MatchRecord.LEFT, participant(record, "Dave").result());
        assertEquals("blue", participant(record, "Dave").team());
        assertEquals(List.of(id("Bob").toString(), id("Dave").toString()),
                record.teams().stream().filter(team -> team.id().equals("blue")).findFirst().orElseThrow().members());
    }

    @Test
    void aSessionThatLeftForShutdownGetsHerTeamsResultNotLeft() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.games().leave(id("Dave"), LeaveReason.SHUTDOWN);

        engine.kill(game, "Alice", "Bob");
        engine.kill(game, "Carol", "Bob");
        MatchRecord record = record();

        assertEquals("blue", record.winner());
        assertEquals(MatchRecord.WON, participant(record, "Dave").result());
    }

    @Test
    void gameEndedFollowsThePhaseChange() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob");

        engine.games().stop(game);

        List<String> events = engine.events().events();
        assertEquals(List.of("phase AS-TDM RUNNING->ENDING", "ended AS-TDM draw STOPPED"),
                events.subList(events.size() - 2, events.size()));
    }

    @Test
    void aGameThatNeverRanStartsWhenItEnds() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, 2));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);
        engine.seconds(game, 2);

        game.end(Outcome.draw(EndReason.STOPPED));
        MatchRecord record = record();

        assertEquals(FakeGameContext.START.plusSeconds(2).toString(), record.startedAt());
        assertEquals(record.startedAt(), record.endedAt());
    }

    @Test
    void aTimeLimitDrawRecordsEveryoneAsDrawWithNoWinner() {
        Game game = running("AS-FFA", Map.of("timeLimit", 1), "Alice", "Bob");

        engine.seconds(game, 60);
        MatchRecord record = record();

        assertNull(record.winner());
        assertEquals("time_limit", record.endReason());
        assertEquals(List.of(MatchRecord.DRAW, MatchRecord.DRAW),
                record.participants().stream().map(MatchRecord.Participant::result).toList());
    }

    @Test
    void aLeaverDuringAContinuingCountdownIsLeftInTheEventualRecord() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, 2));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob", "Carol", "Dave");
        engine.games().startCountdown(game);

        engine.games().leave(id("Carol"), LeaveReason.LEFT);
        engine.seconds(game, PvpConfig.defaults().countdownSeconds());
        assertEquals(Phase.RUNNING, game.phase());

        engine.games().stop(game);
        MatchRecord record = record();

        assertEquals(MatchRecord.LEFT, participant(record, "Carol").result());
    }
}
