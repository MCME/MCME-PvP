package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.RecordingListener;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opening, joining, spectating, leaving, the countdown, running, ending and closing, with a two-team test mode. */
class GameLifecycleTest {

    private static final TestMode DUEL = new TestMode("duel", TestMode.twoTeams().build());
    private static final Arena ARENA = TestArenas.tdm("AS-TDM", 0, 2).withMode("duel");

    @TempDir
    Path dir;

    private TestEngine engine(PvpConfig config) {
        return new TestEngine(dir, new ModeRegistry(List.of(DUEL)), config, ARENA);
    }

    private TestEngine engine() {
        return engine(PvpConfig.defaults());
    }

    @Test
    void openingCreatesTheTeamsAndStartsTheHeartbeat() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");

        assertEquals(Phase.WAITING, game.phase());
        assertEquals(List.of("red", "blue"), game.teams().stream().map(GameTeam::id).toList());
        assertEquals(Optional.of(game.teams().get(1)), game.team("BLUE"));
        assertEquals(1, engine.context(game).tasks().activeCount());
        assertEquals(List.of("opened AS-TDM"), engine.events().events());
        assertEquals(16, game.maxPlayers());
        assertSame(DUEL.rules(), game.rules());
        assertTrue(game.acceptsJoins());
    }

    @Test
    void aWaitingPlayerStandsOnTheArenaSpawnWithoutAColor() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");

        engine.join(game, "Alice");

        PlayerSession alice = engine.session(game, "Alice");
        FakeGameContext context = engine.context(game);
        assertEquals(Role.PLAYER, alice.role());
        assertTrue(alice.alive());
        assertFalse(alice.participated());
        assertEquals(Optional.empty(), alice.color());
        assertEquals(ARENA.spawn(), context.positions().get(alice.id()));
        assertTrue(context.onScoreboard().contains(alice.id()));
        assertFalse(context.colors().containsKey(alice.id()));
        assertTrue(context.log().contains("reset " + alice.id()));
        assertEquals(List.of(alice), game.players());
        assertEquals(List.of(alice.id()), game.audience());
        assertEquals("joined Alice PLAYING", engine.events().events("joined ").get(0));
    }

    @Test
    void aFullGameRefusesPlayers() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM", Map.of("maxPlayers", 2));
        engine.join(game, "Alice");

        PvpException party = assertThrows(PvpException.class, () -> engine.party(game, "Bob", "Carol"));
        engine.join(game, "Bob");
        PvpException solo = assertThrows(PvpException.class, () -> engine.join(game, "Carol"));

        assertEquals(Msg.ERROR_GAME_FULL, party.messageKey());
        assertEquals(Map.of("max", "2"), solo.args());
        assertEquals(2, game.playerCount());
        assertEquals(Optional.empty(), engine.games().gameOf(id("Carol")));
    }

    @Test
    void spectatorsWatchFromTheArenaSpawn() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");

        engine.spectate(game, "Carol");

        PlayerSession carol = engine.session(game, "Carol");
        FakeGameContext context = engine.context(game);
        assertEquals(Role.SPECTATOR, carol.role());
        assertFalse(carol.alive());
        assertEquals(Optional.empty(), carol.color());
        assertEquals(ARENA.spawn(), context.positions().get(carol.id()));
        assertTrue(context.spectating().contains(carol.id()));
        assertTrue(context.onScoreboard().contains(carol.id()));
        assertEquals(List.of(carol), game.spectators());
        assertEquals(0, game.playerCount());
        assertEquals(List.of(carol.id()), game.audience());
        assertEquals("joined Carol SPECTATING", engine.events().events("joined ").get(0));
    }

    @Test
    void leavingSendsThePlayerToTheLobby() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice");

        assertTrue(engine.games().leave(id("Alice"), LeaveReason.LEFT));

        PlayerSession alice = engine.session(game, "Alice");
        FakeGameContext context = engine.context(game);
        assertFalse(alice.present());
        assertEquals(Optional.of(LeaveReason.LEFT), alice.leftReason());
        assertTrue(context.inLobby().contains(alice.id()));
        assertFalse(context.onScoreboard().contains(alice.id()));
        assertTrue(context.log().contains("spectator " + alice.id() + " false"));
        assertEquals("left Alice LEFT", engine.events().events("left ").get(0));
        assertEquals(Optional.empty(), engine.games().gameOf(alice.id()));
        assertEquals(List.of(), game.players());
        assertFalse(engine.games().leave(id("Alice"), LeaveReason.LEFT));
    }

    @Test
    void aQuittingSpectatorIsCleanedUpToo() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.spectate(game, "Carol");

        engine.games().leave(id("Carol"), LeaveReason.QUIT);

        FakeGameContext context = engine.context(game);
        assertFalse(context.spectating().contains(id("Carol")));
        assertTrue(context.inLobby().contains(id("Carol")));
        assertEquals(Optional.of(LeaveReason.QUIT), engine.session(game, "Carol").leftReason());
    }

    @Test
    void aPlayerWhoComesBackGetsTheirSessionBack() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice");
        PlayerSession first = engine.session(game, "Alice");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        engine.join(game, "Alice");

        assertSame(first, engine.session(game, "Alice"));
        assertTrue(first.present());
        assertEquals(Optional.empty(), first.leftReason());
        assertEquals(1, game.sessions().size());
    }

    @Test
    void theCountdownNeedsAWaitingGameWithEnoughPlayers() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice");

        PvpException tooFew = assertThrows(PvpException.class, () -> engine.games().startCountdown(game));
        assertEquals(Msg.ERROR_NOT_ENOUGH_PLAYERS, tooFew.messageKey());
        assertEquals(Map.of("min", "2", "count", "1"), tooFew.args());

        engine.join(game, "Bob");
        engine.games().startCountdown(game);
        PvpException again = assertThrows(PvpException.class, () -> engine.games().startCountdown(game));
        assertEquals(Msg.ERROR_COUNTDOWN_PHASE, again.messageKey());
    }

    @Test
    void theCountdownPutsEveryPlayerOnATeamWithAKitAndFreezesThem() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob", "Carol");
        engine.spectate(game, "Dave");

        engine.games().startCountdown(game);

        FakeGameContext context = engine.context(game);
        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(7, game.secondsLeft());
        assertEquals(Optional.of(Palette.RED), alice.color());
        assertEquals(Optional.of(Palette.BLUE), bob.color());
        assertEquals(Optional.of(Palette.RED), engine.session(game, "Carol").color());
        assertEquals(2, game.teams().get(0).startingSize());
        assertEquals(1, game.teams().get(1).startingSize());
        assertTrue(alice.participated());
        assertFalse(engine.session(game, "Dave").participated());
        assertEquals(ARENA.teamSpawns().get("red").get(0), context.positions().get(alice.id()));
        assertEquals(ARENA.teamSpawns().get("blue").get(0), context.positions().get(bob.id()));
        assertEquals(Kits.STANDARD, context.kits().get(alice.id()));
        assertTrue(context.log().contains("kit " + bob.id() + " standard blue"));
        assertEquals(Palette.BLUE, context.colors().get(bob.id()));
        assertTrue(game.isFrozen(alice.id()));
        assertFalse(game.isFrozen(id("Dave")));
        assertEquals("phase AS-TDM WAITING->COUNTDOWN", engine.events().events().getLast());
    }

    @Test
    void theGameRunsWhenTheCountdownRunsOut() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);

        engine.seconds(game, 6);
        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(1, game.secondsLeft());

        engine.seconds(game, 1);
        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(Optional.of(FakeGameContext.START.plusSeconds(7)), game.startedAt());
        assertEquals(0, game.secondsLeft());
        assertFalse(game.isFrozen(id("Alice")));
        assertEquals("phase AS-TDM COUNTDOWN->RUNNING", engine.events().events().getLast());
    }

    @Test
    void everySecondIsAnnouncedWhileTheGameIsOpen() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");

        engine.seconds(game, 3);

        assertEquals(3, engine.events().secondsPassed());
    }

    @Test
    void theLastTeamStandingWinsAtTheLatestOnTheNextSecond() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.seconds(game, 1);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team("blue", EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void aLateJoinerSpectatesInAModeWithoutLateJoining() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        JoinResult result = engine.games().join(JoinUnit.solo(TestArenas.player("Carol")), game);

        assertEquals(JoinResult.SPECTATING, result);
        assertEquals(Role.SPECTATOR, engine.session(game, "Carol").role());
    }

    @Test
    void stoppingARunningGameShowsTheResultBeforeItCloses() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        engine.games().stop(game);
        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.draw(EndReason.STOPPED)), game.outcome());
        assertEquals(10, game.secondsLeft());
        assertFalse(game.acceptsJoins());

        engine.seconds(game, 9);
        assertEquals(Phase.ENDING, game.phase());
        engine.seconds(game, 1);
        assertEquals(Phase.CLOSED, game.phase());
        List<String> phases = engine.events().events("phase");
        assertEquals(List.of("phase AS-TDM RUNNING->ENDING", "phase AS-TDM ENDING->CLOSED"),
                phases.subList(phases.size() - 2, phases.size()));
        assertEquals("closed AS-TDM", engine.events().events().getLast());
    }

    @Test
    void stoppingAWaitingGameClosesItAtOnceAndReleasesEverything() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice");
        engine.spectate(game, "Carol");
        FakeGameContext context = engine.context(game);

        engine.games().stop(game);

        assertEquals(Phase.CLOSED, game.phase());
        assertEquals(Optional.of(LeaveReason.GAME_CLOSED), engine.session(game, "Alice").leftReason());
        assertFalse(engine.session(game, "Carol").present());
        assertTrue(context.inLobby().containsAll(List.of(id("Alice"), id("Carol"))));
        assertTrue(context.onScoreboard().isEmpty());
        assertTrue(context.spectating().isEmpty());
        assertEquals(0, context.tasks().activeCount());
        assertTrue(context.disposed());
        List<String> log = context.log();
        assertEquals(List.of("cancelAll", "dispose"), log.subList(log.size() - 2, log.size()));
        assertEquals(List.of("phase AS-TDM WAITING->CLOSED", "closed AS-TDM"),
                engine.events().events().subList(engine.events().events().size() - 2, engine.events().events().size()));
        assertEquals(List.of(), engine.games().games());
        assertEquals(Optional.empty(), engine.games().gameOf(id("Alice")));
    }

    @Test
    void stoppingDuringTheCountdownOrTheEndingClosesAtOnce() {
        TestEngine engine = engine();
        Game counting = engine.open("AS-TDM");
        engine.join(counting, "Alice", "Bob");
        engine.games().startCountdown(counting);

        engine.games().stop(counting);
        assertEquals(Phase.CLOSED, counting.phase());

        Game ending = engine.open("AS-TDM");
        engine.join(ending, "Alice", "Bob");
        engine.start(ending);
        engine.games().stop(ending);
        engine.games().stop(ending);
        assertEquals(Phase.CLOSED, ending.phase());
    }

    @Test
    void withoutAnEndingPhaseTheGameClosesRightAway() {
        TestEngine engine = engine(TestEngine.config(7, 0));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        engine.games().stop(game);

        assertEquals(Phase.CLOSED, game.phase());
    }

    @Test
    void aGameEndsOnlyOnce() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        game.end(Outcome.team("red", EndReason.WIN_CONDITION));
        game.end(Outcome.team("blue", EndReason.WIN_CONDITION));

        assertEquals(Optional.of(Outcome.team("red", EndReason.WIN_CONDITION)), game.outcome());
        assertEquals(1, engine.events().events("phase AS-TDM RUNNING->ENDING").size());
    }

    @Test
    void aRespawnDuringEndingDoesNothingForALivingPlayerButSpectatesAnEliminatedOne() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        engine.kill(game, "Alice", "Bob");
        assertEquals(Phase.ENDING, game.phase());
        FakeGameContext context = engine.context(game);

        // The schedule call itself logs "after 1"; only the tick it runs on would add a reset or a kit.
        game.handleRespawn(id("Bob"));
        int linesBeforeTheTick = context.log().size();
        context.advanceTicks(1);
        assertEquals(linesBeforeTheTick, context.log().size());
        assertFalse(context.spectating().contains(id("Bob")));

        game.handleRespawn(id("Alice"));
        context.advanceTicks(1);
        assertTrue(context.spectating().contains(id("Alice")));
    }

    @Test
    void aRespawnTaskScheduledJustBeforeTheGameClosesDoesNothing() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        game.handleRespawn(id("Alice"));
        game.close();
        FakeGameContext context = engine.context(game);
        int linesAfterClose = context.log().size();

        context.advanceTicks(5);

        assertEquals(Phase.CLOSED, game.phase());
        assertEquals(0, context.tasks().activeCount());
        assertEquals(linesAfterClose, context.log().size());
    }

    @Test
    void nobodyCanJoinOrSpectateAnEndingGame() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        engine.games().stop(game);

        PvpException join = assertThrows(PvpException.class, () -> engine.join(game, "Carol"));
        PvpException spectate = assertThrows(PvpException.class, () -> engine.spectate(game, "Carol"));
        PvpException add = assertThrows(PvpException.class, () -> game.add(JoinUnit.solo(TestArenas.player("Dave"))));
        PvpException watch = assertThrows(PvpException.class, () -> game.addSpectator(TestArenas.player("Dave")));

        assertEquals(Msg.ERROR_GAME_ENDED, join.messageKey());
        assertEquals(Msg.ERROR_GAME_ENDED, spectate.messageKey());
        assertEquals(Msg.ERROR_GAME_ENDED, add.messageKey());
        assertEquals(Msg.ERROR_GAME_ENDED, watch.messageKey());
    }

    @Test
    void aCountdownStartedMidSecondStillLastsExactlySevenSeconds() {
        TestEngine engine = engine();
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        FakeGameContext context = engine.context(game);
        context.advanceTicks(15);

        engine.games().startCountdown(game);

        context.advanceTicks(139);
        assertEquals(Phase.COUNTDOWN, game.phase());

        context.advanceTicks(1);
        assertEquals(Phase.RUNNING, game.phase());
    }

    @Test
    void endingAGameSchedulesNoNewTask() {
        // end() also runs during plugin disable, when Paper refuses new tasks, so it must reuse the running
        // heartbeat instead of restarting it.
        PvpConfig config = PvpConfig.defaults();
        TestEngine engine = engine(config);
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        FakeGameContext context = engine.context(game);
        context.advanceTicks(5);
        int scheduledBefore = context.log("every ").size();
        int afterBefore = context.log("after ").size();

        game.end(Outcome.draw(EndReason.STOPPED));

        assertEquals(scheduledBefore, context.log("every ").size());
        assertEquals(afterBefore, context.log("after ").size());
        context.advanceTicks(config.endingSeconds() * 20);
        assertEquals(Phase.CLOSED, game.phase());
    }

    @Test
    void aClosingStepThatThrowsStillReleasesTheGame() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsToLeave(fake.players(), alice, "Alice"), fake.world());
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, new GameListener() {
                }, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);

        RuntimeException failure = assertThrows(RuntimeException.class, game::close);

        assertEquals("lobby failed for Alice", failure.getMessage());
        assertEquals(List.of(), manager.games());
        assertEquals(0, fake.tasks().activeCount());
        assertEquals(Phase.WAITING, manager.open("AS-TDM", Map.of(), "Staff").phase());
    }

    @Test
    void aFailingCloseStepDoesNotStopTheOthers() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        UUID bob = TestArenas.player("Bob").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsToLeave(fake.players(), alice, "Alice"), fake.world());
        RecordingListener events = new RecordingListener();
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, events, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);

        RuntimeException failure = assertThrows(RuntimeException.class, game::close);

        assertEquals("lobby failed for Alice", failure.getMessage());
        assertTrue(fake.inLobby().contains(bob));
        assertFalse(fake.onScoreboard().contains(bob));
        assertTrue(fake.disposed());
        assertTrue(events.events().contains("phase AS-TDM WAITING->CLOSED"));
        assertTrue(events.events().contains("closed AS-TDM"));
    }

    @Test
    void everyCloseStepGetsItsTurnEvenWhenTwoPlayersFail() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        UUID bob = TestArenas.player("Bob").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsTwoSteps(fake.players(), alice, bob), fake.world());
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, new GameListener() {
                }, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);

        RuntimeException failure = assertThrows(RuntimeException.class, game::close);

        assertEquals("setSpectator failed for " + alice, failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("sendToLobby failed for " + bob, failure.getSuppressed()[0].getMessage());
        assertTrue(fake.inLobby().contains(alice));
        assertEquals(List.of(), manager.games());
    }

    @Test
    void aLeaveThatFailsToReachThePlatformStillCancelsTheCountdownAndFiresPlayerLeft() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsToLeave(fake.players(), alice, "Alice"), fake.world());
        RecordingListener events = new RecordingListener();
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, events, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        manager.startCountdown(game);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> manager.leave(alice, LeaveReason.LEFT));

        assertEquals("lobby failed for Alice", failure.getMessage());
        assertEquals(Phase.WAITING, game.phase());
        assertEquals(List.of("left Alice LEFT"), events.events("left "));
    }

    @Test
    void aLeaveWhoseFirstStepFailsStillRunsTheRemainingStepsAndLeaves() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsTwoSteps(fake.players(), alice, UUID.randomUUID()), fake.world());
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, new GameListener() {
                }, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> manager.leave(alice, LeaveReason.LEFT));

        assertEquals("setSpectator failed for " + alice, failure.getMessage());
        assertTrue(fake.log().contains("unscoreboard " + alice));
        assertTrue(fake.log().contains("lobby " + alice));
        assertEquals(Optional.empty(), manager.gameOf(alice));
        assertFalse(game.session(alice).orElseThrow().present());
    }

    @Test
    void aLeaveFailureSurvivesAPhaseFollowUpThatAlsoFails() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        GameContextFactory factory = (id, arena) ->
                new SteppedContext(fake, new FailsToLeave(fake.players(), alice, "Alice"), fake.world());
        GameListener listener = new GameListener() {
            @Override
            public void phaseChanged(Game game, Phase from, Phase to) {
                if (to == Phase.WAITING) {
                    throw new RuntimeException("phaseChanged failed");
                }
            }
        };
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, listener, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        manager.startCountdown(game);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> manager.leave(alice, LeaveReason.LEFT));

        assertEquals("lobby failed for Alice", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("phaseChanged failed", failure.getSuppressed()[0].getMessage());
    }

    @Test
    void aCleanLeaveWhoseFollowUpFailsStillThrows() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        GameContextFactory factory = (id, arena) -> fake;
        GameListener listener = new GameListener() {
            @Override
            public void phaseChanged(Game game, Phase from, Phase to) {
                if (to == Phase.WAITING) {
                    throw new RuntimeException("phaseChanged failed");
                }
            }
        };
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, listener, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        manager.startCountdown(game);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> manager.leave(alice, LeaveReason.LEFT));

        assertEquals("phaseChanged failed", failure.getMessage());
        assertEquals(0, failure.getSuppressed().length);
        assertEquals(Phase.WAITING, game.phase());
        assertEquals(Optional.empty(), manager.gameOf(alice));
    }

    @Test
    void anEntityThatCannotBeRemovedDoesNotKeepTheOthers() {
        FakeGameContext fake = new FakeGameContext();
        FailsToRemove world = new FailsToRemove(fake.world());
        GameContextFactory factory = (id, arena) -> new SteppedContext(fake, fake.players(), world);
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, new GameListener() {
                }, player -> 0.0);
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        UUID first = game.spawnEntity("ZOMBIE", ARENA.spawn());
        UUID second = game.spawnEntity("ZOMBIE", ARENA.spawn());
        world.failFor(first);

        RuntimeException failure = assertThrows(RuntimeException.class, game::close);

        assertEquals("remove failed for " + first, failure.getMessage());
        assertFalse(fake.spawnedEntities().contains(second));
        assertTrue(game.entities().all().isEmpty());
    }

    @Test
    void stoppingAGameFromItsCountdownNoticeClosesItOnce() {
        List<FakeGameContext> created = new ArrayList<>();
        GameManager[] holder = new GameManager[1];
        int[] closedCount = {0};
        GameListener listener = new GameListener() {
            @Override
            public void phaseChanged(Game game, Phase from, Phase to) {
                if (to == Phase.COUNTDOWN) {
                    holder[0].stop(game);
                }
            }

            @Override
            public void gameClosed(Game game) {
                closedCount[0]++;
            }
        };
        GameManager manager = new GameManager(TestArenas.registry(dir, ARENA), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), FakeGameContext.factory(created), listener, player -> 0.0);
        holder[0] = manager;
        Game game = manager.open("AS-TDM", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), game);

        manager.startCountdown(game);

        assertEquals(1, closedCount[0]);
        assertEquals(List.of(), manager.games());
        assertEquals(Phase.CLOSED, game.phase());
        created.get(0).advanceSeconds(10);
        assertEquals(1, closedCount[0]);
        assertEquals(Phase.CLOSED, game.phase());
    }

    /** Delegates to {@code delegate} except players() and world(), which are substituted directly. */
    private record SteppedContext(GameContext delegate, PlayerActions players, WorldActions world)
            implements GameContext {

        @Override
        public GameTasks tasks() {
            return delegate.tasks();
        }

        @Override
        public Clock clock() {
            return delegate.clock();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }

    /** Delegates every player action to {@code delegate} except sendToLobby, which fails for one chosen player. */
    private record FailsToLeave(PlayerActions delegate, UUID failing, String name) implements PlayerActions {

        @Override
        public void teleport(UUID player, NamedPoint point) {
            delegate.teleport(player, point);
        }

        @Override
        public void resetForPlay(UUID player) {
            delegate.resetForPlay(player);
        }

        @Override
        public void giveKit(UUID player, Kit kit, TeamSpec color) {
            delegate.giveKit(player, kit, color);
        }

        @Override
        public void setSpectator(UUID player, boolean spectator) {
            delegate.setSpectator(player, spectator);
        }

        @Override
        public void joinScoreboard(UUID player, TeamSpec color) {
            delegate.joinScoreboard(player, color);
        }

        @Override
        public void leaveScoreboard(UUID player) {
            delegate.leaveScoreboard(player);
        }

        @Override
        public void sendToLobby(UUID player) {
            if (player.equals(failing)) {
                throw new RuntimeException("lobby failed for " + name);
            }
            delegate.sendToLobby(player);
        }
    }

    /**
     * Delegates every player action to {@code delegate}, except that setSpectator fails for one chosen player and
     * sendToLobby fails for another.
     */
    private record FailsTwoSteps(PlayerActions delegate, UUID failsSpectator, UUID failsLobby)
            implements PlayerActions {

        @Override
        public void teleport(UUID player, NamedPoint point) {
            delegate.teleport(player, point);
        }

        @Override
        public void resetForPlay(UUID player) {
            delegate.resetForPlay(player);
        }

        @Override
        public void giveKit(UUID player, Kit kit, TeamSpec color) {
            delegate.giveKit(player, kit, color);
        }

        @Override
        public void setSpectator(UUID player, boolean spectator) {
            if (player.equals(failsSpectator)) {
                throw new RuntimeException("setSpectator failed for " + player);
            }
            delegate.setSpectator(player, spectator);
        }

        @Override
        public void joinScoreboard(UUID player, TeamSpec color) {
            delegate.joinScoreboard(player, color);
        }

        @Override
        public void leaveScoreboard(UUID player) {
            delegate.leaveScoreboard(player);
        }

        @Override
        public void sendToLobby(UUID player) {
            if (player.equals(failsLobby)) {
                throw new RuntimeException("sendToLobby failed for " + player);
            }
            delegate.sendToLobby(player);
        }
    }

    /**
     * Delegates to {@code delegate} except removeEntity, which always fails for whichever entity {@link #failFor}
     * named.
     */
    private static final class FailsToRemove implements WorldActions {

        private final WorldActions delegate;
        private UUID failing;

        FailsToRemove(WorldActions delegate) {
            this.delegate = delegate;
        }

        void failFor(UUID entity) {
            failing = entity;
        }

        @Override
        public UUID spawnEntity(String entityType, NamedPoint at) {
            return delegate.spawnEntity(entityType, at);
        }

        @Override
        public void removeEntity(UUID entity) {
            if (entity.equals(failing)) {
                throw new RuntimeException("remove failed for " + entity);
            }
            delegate.removeEntity(entity);
        }
    }
}
