package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Respawning into the fight with a team mode: team spawns, spawn protection and the time limit with teams. */
class RespawnTest {

    /** Two teams that respawn with 2 seconds of spawn protection; the higher team score at the time limit wins. */
    private static final TestMode SKIRMISH = new TestMode("skirmish", TestMode.twoTeams()
            .respawn(Respawn.AT_SPAWN)
            .spawnProtection(2)
            .killScore(1)
            .win(WinConditions.highestAt(Setting.TIME_LIMIT))
            .build());

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running() {
        engine = new TestEngine(dir, new ModeRegistry(List.of(SKIRMISH)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("skirmish"));
        Game game = engine.open("AS-TDM", Map.of("timeLimit", 1));
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        return game;
    }

    @Test
    void aTeamPlayerRespawnsOnTheTeamSpawn() {
        Game game = running();

        engine.kill(game, "Bob", "Alice");

        assertEquals(Optional.of(game.arena().teamSpawns().get("blue").get(0)), game.respawnPoint(id("Bob")));
        assertTrue(engine.session(game, "Bob").alive());
    }

    @Test
    void spawnProtectionRunsForItsSecondsAfterARespawn() {
        Game game = running();
        engine.kill(game, "Bob", "Alice");

        game.handleRespawn(id("Bob"));
        engine.context(game).advanceTicks(1);
        assertTrue(game.isProtected(id("Bob")));
        assertFalse(game.isProtected(id("Alice")));

        engine.seconds(game, 1);
        assertTrue(game.isProtected(id("Bob")));

        engine.seconds(game, 1);
        assertFalse(game.isProtected(id("Bob")));
    }

    @Test
    void aRespawnTaskScheduledJustBeforeThePlayerLeavesDoesNothing() {
        Game game = running();
        engine.kill(game, "Bob", "Alice");
        game.handleRespawn(id("Bob"));

        engine.games().leave(id("Bob"), LeaveReason.LEFT);
        FakeGameContext context = engine.context(game);
        int linesAfterLeaving = context.log().size();
        context.advanceTicks(1);

        assertEquals(linesAfterLeaving, context.log().size());
        assertTrue(context.inLobby().contains(id("Bob")));
    }

    @Test
    void theEndClearsSpawnProtection() {
        Game game = running();
        engine.kill(game, "Bob", "Alice");
        game.handleRespawn(id("Bob"));
        engine.context(game).advanceTicks(1);
        assertTrue(engine.session(game, "Bob").isProtected());

        engine.games().stop(game);

        // Game.isProtected is also gated on phase() == RUNNING, so once the game is ENDING it would say false
        // regardless of the session's own flag; check that flag directly, since this test is in package `game`.
        assertFalse(game.isProtected(id("Bob")));
        assertFalse(engine.session(game, "Bob").isProtected());
    }

    @Test
    void theTeamWithTheHighestScoreAtTheTimeLimitWins() {
        Game game = running();
        engine.kill(game, "Bob", "Alice");

        engine.seconds(game, 60);

        assertEquals(Optional.of(Outcome.team("red", EndReason.TIME_LIMIT)), game.outcome());
    }

    @Test
    void teamsWithEqualScoresDraw() {
        Game game = running();
        engine.kill(game, "Bob", "Alice");
        engine.kill(game, "Alice", "Bob");

        engine.seconds(game, 60);

        assertEquals(Optional.of(Outcome.draw(EndReason.TIME_LIMIT)), game.outcome());
    }
}
