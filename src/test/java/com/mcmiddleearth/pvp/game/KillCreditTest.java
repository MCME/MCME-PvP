package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the {@link Combat#death} kill-credit clauses that no mode's own tests exercise: a killer who has left the
 * game, a killer who is spectating, and a suicide (which needs a mode without teams, since the teammate check would
 * otherwise catch it first).
 */
class KillCreditTest {

    private static final TestMode DUEL = new TestMode("duel", TestMode.twoTeams().build());
    private static final Arena DUEL_ARENA = TestArenas.tdm("AS-DUEL", 0, 2).withMode("duel");

    /** No teams at all, so nothing but the self-kill check can disqualify the kill. */
    private static final TestMode SOLO = new TestMode("solo", ModeRules.builder()
            .noTeams()
            .spawnPoints(SpawnPoints.perTeam())
            .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
            .respawn(Respawn.NEVER)
            .midgameJoin(MidgameJoin.SPECTATE)
            .win(WinConditions.firstTo(Setting.SCORE_GOAL))
            .killScore(1)
            .build());
    private static final Arena SOLO_ARENA = TestArenas.tdm("AS-SOLO", 0, 2).withMode("solo");

    @TempDir
    Path dir;

    private TestEngine engine;

    /** Two teams of three, running. */
    private Game runningDuel(String... names) {
        engine = new TestEngine(dir, new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(), DUEL_ARENA);
        Game game = engine.open("AS-DUEL");
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    @Test
    void aKillerWhoHasLeftGetsNoCredit() {
        Game game = runningDuel("Alice", "Bob", "Carol", "Dave", "Eve", "Frank");

        engine.games().leave(id("Bob"), LeaveReason.LEFT);
        assertEquals(Phase.RUNNING, game.phase(), "one of three teammates leaving must not end the game");
        engine.kill(game, "Alice", "Bob");

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Bob").kills());
        assertEquals(List.of("died Alice"), engine.events().events("died"));
    }

    @Test
    void aSpectatingKillerGetsNoCredit() {
        Game game = runningDuel("Alice", "Bob", "Carol", "Dave", "Eve", "Frank");
        engine.spectate(game, "Gina");

        engine.kill(game, "Alice", "Gina");

        assertEquals(1, engine.session(game, "Alice").deaths());
        assertEquals(0, engine.session(game, "Gina").kills());
        assertEquals(List.of("died Alice"), engine.events().events("died"));
    }

    @Test
    void aSuicideGetsOnlyADeath() {
        engine = new TestEngine(dir, new ModeRegistry(List.of(SOLO)), PvpConfig.defaults(), SOLO_ARENA);
        Game game = engine.open("AS-SOLO");
        engine.join(game, "Alice", "Bob");
        engine.start(game);

        engine.kill(game, "Alice", "Alice");

        PlayerSession alice = engine.session(game, "Alice");
        assertEquals(1, alice.deaths());
        assertEquals(0, alice.kills());
        assertEquals(0, alice.score());
    }
}
