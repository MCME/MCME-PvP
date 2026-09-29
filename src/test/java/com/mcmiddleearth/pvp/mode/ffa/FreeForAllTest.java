package com.mcmiddleearth.pvp.mode.ffa;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
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

class FreeForAllTest {

    private static final Arena ARENA = TestArenas.ffa("AS-FFA", 0, 3);

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(Map<String, ?> settings, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), ARENA);
        Game game = engine.open("AS-FFA", settings);
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    @Test
    void theRulesAreThoseOf112() {
        FreeForAll ffa = new FreeForAll();

        assertEquals("ffa", ffa.id());
        assertEquals(List.of(Setting.TIME_LIMIT, Setting.SCORE_GOAL, Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN),
                ffa.rules().settings());
        assertEquals(1, ffa.rules().killScore());
        assertEquals(Optional.of("ffa"), TestArenas.modes().byLegacyName("Free For All").map(GameMode::id));
        assertEquals(Optional.of("tdm"), TestArenas.modes().byLegacyName("Team Deathmatch").map(GameMode::id));
    }

    @Test
    void theTimeLimitIsInMinutes() {
        Game game = running(Map.of("timeLimit", 10), "Alice", "Bob");

        assertEquals(600, game.timeLimitSeconds());
        assertEquals(600, game.secondsLeft());
        engine.seconds(game, 1);
        assertEquals(599, game.secondsLeft());
    }

    @Test
    void theHighestScoreAtTheTimeLimitWins() {
        Game game = running(Map.of("timeLimit", 1), "Alice", "Bob", "Carol");
        engine.kill(game, "Bob", "Alice");
        engine.kill(game, "Carol", "Alice");
        engine.kill(game, "Alice", "Carol");

        engine.seconds(game, 59);
        assertEquals(Phase.RUNNING, game.phase());
        engine.seconds(game, 1);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.player(id("Alice"), EndReason.TIME_LIMIT)), game.outcome());
    }

    @Test
    void aTieAtTheTimeLimitIsADraw() {
        Game game = running(Map.of("timeLimit", 1), "Alice", "Bob");
        engine.kill(game, "Bob", "Alice");
        engine.kill(game, "Alice", "Bob");

        engine.seconds(game, 60);

        assertEquals(Optional.of(Outcome.draw(EndReason.TIME_LIMIT)), game.outcome());
    }

    @Test
    void aGameWithoutScoresIsADraw() {
        Game game = running(Map.of("timeLimit", 1), "Alice", "Bob");

        engine.seconds(game, 60);

        assertEquals(Optional.of(Outcome.draw(EndReason.TIME_LIMIT)), game.outcome());
    }

    @Test
    void theFirstToTheScoreGoalWinsAtOnce() {
        Game game = running(Map.of("scoreGoal", 2), "Alice", "Bob");

        engine.kill(game, "Bob", "Alice");
        assertEquals(Phase.RUNNING, game.phase());
        engine.kill(game, "Bob", "Alice");

        assertEquals(Optional.of(Outcome.player(id("Alice"), EndReason.WIN_CONDITION)), game.outcome());
        // The death is announced before the game over, even though the kill decided it.
        List<String> events = engine.events().events().stream()
                .filter(event -> event.startsWith("died") || event.startsWith("phase"))
                .toList();
        assertEquals(List.of("died Bob by Alice", "phase AS-FFA RUNNING->ENDING"),
                events.subList(events.size() - 2, events.size()));
    }

    @Test
    void aKillScoresAPointAndTheVictimRespawns() {
        Game game = running(Map.of(), "Alice", "Bob");

        engine.kill(game, "Bob", "Alice");

        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        assertEquals(1, alice.score());
        assertEquals(1, alice.kills());
        assertEquals(1, bob.deaths());
        assertTrue(bob.alive());
        assertEquals(1, engine.events().scoreChanges());
    }

    @Test
    void playersSpawnAndRespawnRoundRobinOverThePlayerSpawns() {
        Game game = running(Map.of(), "Alice", "Bob");
        FakeGameContext context = engine.context(game);
        assertEquals(ARENA.points().get("PlayerSpawn"), context.positions().get(id("Alice")));
        assertEquals(ARENA.points().get("PlayerSpawn1"), context.positions().get(id("Bob")));

        engine.kill(game, "Bob", "Alice");

        assertEquals(Optional.of(ARENA.points().get("PlayerSpawn2")), game.respawnPoint(id("Bob")));
        assertEquals(Optional.of(ARENA.points().get("PlayerSpawn")), game.respawnPoint(id("Bob")));
    }

    @Test
    void aRespawnedPlayerGetsAFreshKitInTheirColor() {
        Game game = running(Map.of(), "Alice", "Bob");
        FakeGameContext context = engine.context(game);
        engine.kill(game, "Bob", "Alice");
        int kitsBefore = context.log("kit " + id("Bob")).size();

        game.handleRespawn(id("Bob"));
        context.advanceTicks(1);

        assertEquals(kitsBefore + 1, context.log("kit " + id("Bob")).size());
        assertEquals("kit " + id("Bob") + " standard p-blue", context.log("kit " + id("Bob")).getLast());
        assertEquals(Kits.STANDARD, context.kits().get(id("Bob")));
        assertFalse(context.spectating().contains(id("Bob")));
    }

    @Test
    void everyPlayerGetsTheNextPersonalColor() {
        Game game = running(Map.of(), "Alice", "Bob", "Carol");

        assertEquals(Optional.of(Palette.PERSONAL.get(0)), engine.session(game, "Alice").color());
        assertEquals(Optional.of(Palette.PERSONAL.get(1)), engine.session(game, "Bob").color());
        assertEquals(Optional.of(Palette.PERSONAL.get(2)), engine.session(game, "Carol").color());
        assertEquals(Palette.PERSONAL.get(1), engine.context(game).colors().get(id("Bob")));
        assertEquals(List.of(), game.teams());
    }

    @Test
    void theWinnerIsTheOnlyPresentPlayerWithTheTopScore() {
        Game game = running(Map.of("timeLimit", 1), "Alice", "Bob", "Carol");
        engine.kill(game, "Bob", "Alice");
        engine.kill(game, "Bob", "Alice");
        engine.kill(game, "Carol", "Bob");

        // Alice has the top score (2), but quit: 1.1.2 still declared a departed player the winner.
        engine.games().leave(id("Alice"), LeaveReason.QUIT);
        assertEquals(Phase.RUNNING, game.phase(), "Bob and Carol staying must not end the game as abandoned");

        engine.seconds(game, 60);

        assertEquals(Optional.of(Outcome.player(id("Bob"), EndReason.TIME_LIMIT)), game.outcome());
    }

    @Test
    void twelvePlayersWrapThePersonalColorPalette() {
        String[] names = {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12"};
        Game game = running(Map.of(), names);

        assertEquals(names.length, game.playerCount());
        for (int i = 0; i < Palette.PERSONAL.size(); i++) {
            assertEquals(Optional.of(Palette.PERSONAL.get(i)), engine.session(game, names[i]).color());
        }
        // The palette has 11 colors; the 12th player wraps back to the first.
        assertEquals(Optional.of(Palette.PERSONAL.get(0)), engine.session(game, names[11]).color());
    }

    @Test
    void aSinglePlayerSpawnStartsAndRespawnsWithoutTrouble() {
        // 1.1.2's Free For All crashed at the end of a spawn list instead of wrapping.
        Arena arena = TestArenas.ffa("AS-FFA-SOLO", 0, 1);
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), arena);
        Game game = engine.open("AS-FFA-SOLO");
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        assertEquals(Phase.RUNNING, game.phase());
        FakeGameContext context = engine.context(game);
        NamedPoint spawn = arena.points().get("PlayerSpawn");
        assertEquals(spawn, context.positions().get(id("Alice")));
        assertEquals(spawn, context.positions().get(id("Bob")));

        engine.kill(game, "Bob", "Alice");
        assertEquals(1, engine.session(game, "Bob").deaths());
        assertEquals(Optional.of(spawn), game.respawnPoint(id("Bob")));

        game.handleRespawn(id("Bob"));
        context.advanceTicks(1);

        assertTrue(engine.session(game, "Bob").alive());
        assertFalse(context.spectating().contains(id("Bob")));
        assertEquals(Kits.STANDARD, context.kits().get(id("Bob")));
    }
}
