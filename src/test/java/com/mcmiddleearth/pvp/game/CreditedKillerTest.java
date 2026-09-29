package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Game#creditedKiller} names whom {@link Game#handleDeath} would credit, and changes nothing. */
class CreditedKillerTest {

    private static final TestMode DUEL = new TestMode("duel", TestMode.twoTeams().build());
    private static final Arena DUEL_ARENA = TestArenas.tdm("AS-DUEL", 0, 2).withMode("duel");
    private static final String[] PLAYERS = {"Alice", "Bob", "Carol", "Dave"};

    @TempDir
    Path dir;

    private TestEngine engine;

    /** Two teams of two, not started yet. */
    private Game duel() {
        engine = new TestEngine(dir, new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(), DUEL_ARENA);
        Game game = engine.open("AS-DUEL");
        engine.join(game, PLAYERS);
        return game;
    }

    @Test
    void anOpponentIsCreditedAndAskingChangesNothing() {
        Game game = duel();
        engine.start(game);
        String opponent = other(game, "Alice", false);

        Optional<PlayerSession> credited = game.creditedKiller(id("Alice"), id(opponent));

        assertEquals(Optional.of(id(opponent)), credited.map(PlayerSession::id));
        assertEquals(0, engine.session(game, opponent).kills());
        assertEquals(0, engine.session(game, "Alice").deaths());
        assertEquals(List.of(), engine.events().events("died"));
    }

    @Test
    void aTeammateTheVictimThemselvesAndNoKillerAreNotCredited() {
        Game game = duel();
        engine.start(game);
        String teammate = other(game, "Alice", true);

        assertTrue(game.creditedKiller(id("Alice"), id(teammate)).isEmpty());
        assertTrue(game.creditedKiller(id("Alice"), id("Alice")).isEmpty());
        assertTrue(game.creditedKiller(id("Alice"), null).isEmpty());
    }

    @Test
    void aDeathThatWouldNotCountCreditsNobody() {
        Game game = duel();
        assertTrue(game.creditedKiller(id("Alice"), id("Bob")).isEmpty(), "before the fight");

        engine.start(game);
        String opponent = other(game, "Alice", false);
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(Phase.RUNNING, game.phase());
        assertTrue(game.creditedKiller(id("Alice"), id(opponent)).isEmpty(), "a victim who left");
    }

    /** Another of the four players, on the same team as {@code name} or on the other one. */
    private String other(Game game, String name, boolean sameTeam) {
        Optional<GameTeam> team = engine.session(game, name).team();
        return Stream.of(PLAYERS)
                .filter(other -> !other.equals(name))
                .filter(other -> engine.session(game, other).team().equals(team) == sameTeam)
                .findFirst()
                .orElseThrow();
    }
}
