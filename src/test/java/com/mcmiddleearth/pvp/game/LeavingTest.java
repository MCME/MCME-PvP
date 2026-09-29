package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What leaving does to the game itself: a countdown can fall back to WAITING, and an empty game ends. */
class LeavingTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game open(String arenaId, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3));
        Game game = engine.open(arenaId);
        engine.join(game, names);
        return game;
    }

    @Test
    void leavingTheCountdownBelowTheMinimumGoesBackToWaiting() {
        Game game = open("AS-TDM", "Alice", "Bob");
        engine.games().startCountdown(game);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        FakeGameContext context = engine.context(game);
        assertEquals(Phase.WAITING, game.phase());
        assertEquals(Optional.empty(), bob.team());
        assertEquals(Optional.empty(), bob.color());
        assertFalse(bob.participated());
        assertFalse(context.colors().containsKey(bob.id()));
        assertEquals(0, game.teams().get(1).startingSize());
        assertEquals("phase AS-TDM COUNTDOWN->WAITING", engine.events().events().getLast());
        assertFalse(game.isFrozen(bob.id()));
        assertEquals(Optional.empty(), alice.team());
        assertFalse(alice.participated());
        assertFalse(context.kits().containsKey(bob.id()));
        assertEquals(game.arena().spawn(), context.positions().get(bob.id()));
    }

    @Test
    void aCancelledCountdownCanStartAgain() {
        Game game = open("AS-TDM", "Alice", "Bob");
        engine.games().startCountdown(game);
        engine.games().leave(id("Alice"), LeaveReason.QUIT);

        engine.join(game, "Carol");
        engine.start(game);

        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(2, game.players().stream().filter(p -> p.team().isPresent()).count());
    }

    @Test
    void aTeamEmptiedDuringTheCountdownGoesBackToWaiting() {
        Game game = open("AS-TDM", "Alice", "Bob", "Carol");
        engine.games().startCountdown(game);
        PlayerSession alone = game.players().stream()
                .filter(player -> player.team().orElseThrow().members().size() == 1)
                .findFirst().orElseThrow();

        engine.games().leave(alone.id(), LeaveReason.LEFT);

        assertEquals(Phase.WAITING, game.phase());
        assertEquals("phase AS-TDM COUNTDOWN->WAITING", engine.events().events().getLast());
    }

    @Test
    void leavingTheCountdownWithEnoughPlayersLeftKeepsItGoing() {
        Game game = open("AS-TDM", "Alice", "Bob", "Carol");
        engine.games().startCountdown(game);
        PlayerSession paired = game.players().stream()
                .filter(player -> player.team().orElseThrow().members().size() == 2)
                .findFirst().orElseThrow();

        engine.games().leave(paired.id(), LeaveReason.LEFT);

        assertEquals(Phase.COUNTDOWN, game.phase());
        // The players who stay, not the leaver: leaving keeps its own (now unused) team on the session.
        assertTrue(game.players().stream().allMatch(p -> p.team().isPresent()));
    }

    @Test
    void leavingAnFfaCountdownBelowTheMinimumClearsEveryoneAndRestartsTheColors() {
        // Without teams, "too few players" is the only way a countdown can cancel (no team can ever be empty).
        Game game = open("AS-FFA", "Alice", "Bob", "Carol");
        engine.games().startCountdown(game);

        engine.games().leave(id("Carol"), LeaveReason.LEFT);
        assertEquals(Phase.COUNTDOWN, game.phase());

        engine.games().leave(id("Bob"), LeaveReason.LEFT);
        assertEquals(Phase.WAITING, game.phase());

        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession carol = engine.session(game, "Carol");
        FakeGameContext context = engine.context(game);
        assertEquals(Optional.empty(), alice.color());
        assertFalse(context.colors().containsKey(alice.id()));
        assertEquals(Optional.empty(), carol.color());
        assertFalse(carol.participated());

        engine.join(game, "Dave");
        engine.games().startCountdown(game);

        assertEquals(Optional.of(Palette.PERSONAL.get(0)), alice.color());
    }

    @Test
    void aRunningGameThatEveryoneLeftEndsAsAbandoned() {
        Game game = open("AS-FFA", "Alice", "Bob");
        engine.start(game);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        assertEquals(Phase.RUNNING, game.phase());
        engine.games().leave(id("Bob"), LeaveReason.QUIT);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.draw(EndReason.ABANDONED)), game.outcome());
    }

    @Test
    void spectatorsDoNotKeepAGameAlive() {
        Game game = open("AS-FFA", "Alice", "Bob");
        engine.spectate(game, "Carol");
        engine.start(game);

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.games().leave(id("Bob"), LeaveReason.LEFT);

        assertEquals(Optional.of(Outcome.draw(EndReason.ABANDONED)), game.outcome());
        assertEquals(List.of(engine.session(game, "Carol")), game.spectators());
    }

    @Test
    void aPartyThatJoinedDuringTheCountdownStaysTogetherAfterACancelledCountdown() {
        Game game = open("AS-TDM", "Alice", "Bob");
        engine.games().startCountdown(game);

        engine.party(game, "Carol", "Dave");

        // Bob is alone on his team; leaving empties it and cancels the countdown.
        engine.games().leave(id("Bob"), LeaveReason.LEFT);
        assertEquals(Phase.WAITING, game.phase());

        engine.games().startCountdown(game);

        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(engine.session(game, "Carol").team().orElseThrow().id(),
                engine.session(game, "Dave").team().orElseThrow().id());
    }
}
