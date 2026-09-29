package com.mcmiddleearth.pvp.mode.rules;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinConditionsTest {

    /** Two teams; the last team standing wins, or the first team to the score goal. */
    private static final TestMode SCORED = new TestMode("scored",
            TestMode.twoTeams().win(WinConditions.lastTeamStanding().orFirstTo(Setting.SCORE_GOAL)).build());

    /** A condition that always names a winner, to see which one {@code either} asks first. */
    private record Fixed(Outcome outcome, boolean timed) implements WinCondition {
        @Override
        public Optional<Outcome> evaluate(Game game) {
            return Optional.of(outcome);
        }

        @Override
        public boolean usesTimeLimit() {
            return timed;
        }

        @Override
        public Outcome timeUp(Game game) {
            return outcome;
        }

        @Override
        public List<Setting> settings() {
            return timed ? List.of(Setting.TIME_LIMIT) : List.of();
        }
    }

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(Map<String, ?> settings) {
        engine = new TestEngine(dir, new ModeRegistry(List.of(SCORED)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("scored"));
        Game game = engine.open("AS-TDM", settings);
        engine.join(game, "Alice", "Bob");
        engine.start(game);
        return game;
    }

    @Test
    void lastTeamStandingWaitsWhileTwoTeamsHavePlayers() {
        Game game = running(Map.of());

        assertEquals(Optional.empty(), WinConditions.lastTeamStanding().evaluate(game));
        assertFalse(WinConditions.lastTeamStanding().usesTimeLimit());
        assertEquals(List.of(), WinConditions.lastTeamStanding().settings());
    }

    @Test
    void lastTeamStandingIsADrawWhenNoTeamHasPlayersLeft() {
        Game game = running(Map.of());

        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        engine.games().leave(id("Bob"), LeaveReason.LEFT);

        assertEquals(Optional.of(Outcome.draw(EndReason.WIN_CONDITION)),
                WinConditions.lastTeamStanding().evaluate(game));
    }

    @Test
    void highestAtOnlyTakesTheTimeLimit() {
        assertThrows(IllegalArgumentException.class, () -> WinConditions.highestAt(Setting.SCORE_GOAL));
    }

    @Test
    void theFirstTeamToTheScoreGoalWins() {
        Game game = running(Map.of("scoreGoal", 3));

        game.scores().addScore(engine.session(game, "Alice"), 2);
        assertEquals(Phase.RUNNING, game.phase());

        game.scores().addScore(engine.session(game, "Alice"), 1);
        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team("red", EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void teamPointsCountTowardsTheGoalToo() {
        Game game = running(Map.of("scoreGoal", 5));

        game.scores().addTeamScore(game.teams().get(1), 5);

        assertEquals(Optional.of(Outcome.team("blue", EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void aScoreGoalOfZeroIsOff() {
        Game game = running(Map.of("scoreGoal", 0));

        game.scores().addScore(engine.session(game, "Alice"), 50);

        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(Optional.empty(), WinConditions.firstTo(Setting.SCORE_GOAL).evaluate(game));
    }

    @Test
    void eitherAsksTheFirstConditionFirst() {
        Game game = running(Map.of());
        Outcome red = Outcome.team("red", EndReason.WIN_CONDITION);
        Outcome blue = Outcome.team("blue", EndReason.TIME_LIMIT);

        WinCondition both = WinConditions.either(new Fixed(red, false), new Fixed(blue, true));

        assertEquals(Optional.of(red), both.evaluate(game));
        assertTrue(both.usesTimeLimit());
        assertEquals(blue, both.timeUp(game));
    }

    @Test
    void eitherOffersTheSettingsOfBothInMenuOrder() {
        WinCondition condition = WinConditions.firstTo(Setting.SCORE_GOAL)
                .orFirstTo(Setting.SCORE_GOAL);
        WinCondition timed = WinConditions.either(WinConditions.firstTo(Setting.SCORE_GOAL),
                new Fixed(Outcome.draw(EndReason.TIME_LIMIT), true));

        assertEquals(List.of(Setting.SCORE_GOAL), condition.settings());
        assertEquals(List.of(Setting.TIME_LIMIT, Setting.SCORE_GOAL), timed.settings());
        assertFalse(condition.usesTimeLimit());
    }
}
