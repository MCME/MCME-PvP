package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Team placement at the countdown: parties stay together, and balanced modes spread skill. */
class BalancingTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game waiting(int teams, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, teams));
        Game game = engine.open("AS-TDM", Map.of("teamCount", teams));
        engine.join(game, names);
        return game;
    }

    private List<String> team(Game game, int index) {
        return game.teams().get(index).members().stream().map(PlayerSession::name).toList();
    }

    @Test
    void aBalancedModeSpreadsLifetimeKillDeathOverTheTeams() {
        Game game = waiting(2, "Alice", "Bob", "Carol", "Dave");
        engine.skill("Alice", 4.0);
        engine.skill("Bob", 3.0);
        engine.skill("Carol", 2.0);
        engine.skill("Dave", 1.0);

        engine.games().startCountdown(game);

        assertEquals(List.of("Alice", "Dave"), team(game, 0));
        assertEquals(List.of("Bob", "Carol"), team(game, 1));
    }

    @Test
    void aPartyStaysTogetherAndTheTeamsStayEven() {
        Game game = waiting(2, "Solo1");
        engine.party(game, "Alice", "Bob", "Carol");
        engine.join(game, "Solo2", "Solo3");

        engine.games().startCountdown(game);

        assertEquals(List.of("Alice", "Bob", "Carol"), team(game, 0));
        assertEquals(List.of("Solo1", "Solo2", "Solo3"), team(game, 1));
        assertEquals(3, game.teams().get(0).startingSize());
        assertEquals(3, game.teams().get(1).startingSize());
    }

    @Test
    void playersSpreadOverThreeTeams() {
        Game game = waiting(3);
        engine.party(game, "A", "B");
        engine.join(game, "C", "D", "E", "F");

        engine.games().startCountdown(game);

        List<List<String>> teams = List.of(team(game, 0), team(game, 1), team(game, 2));
        assertEquals(List.of(2, 2, 2), teams.stream().map(List::size).toList());
        // The balancer must keep the party on one team, not just get the sizes even.
        assertTrue(teams.stream().anyMatch(t -> t.containsAll(List.of("A", "B"))));
    }

    @Test
    void aModeWithoutBalancingPlacesUnitsRoundRobin() {
        TestMode plain = new TestMode("plain", TestMode.twoTeams().build());
        engine = new TestEngine(dir, new ModeRegistry(List.of(plain)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("plain"));
        Game game = engine.open("AS-TDM");
        engine.party(game, "Alice", "Bob", "Carol");
        engine.join(game, "Dave", "Erin");
        engine.skill("Dave", 9.0);

        engine.games().startCountdown(game);

        assertEquals(List.of("Alice", "Bob", "Carol", "Erin"), team(game, 0));
        assertEquals(List.of("Dave"), team(game, 1));
    }

    @Test
    void aPlayerWhoLeftAndCameBackIsPlacedOnce() {
        Game game = waiting(2, "Alice", "Bob", "Carol", "Dave");
        engine.games().leave(TestEngine.id("Bob"), LeaveReason.LEFT);
        engine.join(game, "Bob");

        engine.games().startCountdown(game);

        assertEquals(List.of("Alice", "Dave"), team(game, 0));
        assertEquals(List.of("Bob", "Carol"), team(game, 1));
    }

    @Test
    void aPartyMinusAMemberWhoLeftAndCameBackStaysTogether() {
        Game game = waiting(2, "Dave", "Erin", "Frank");
        engine.party(game, "Alice", "Bob", "Carol");
        engine.games().leave(TestEngine.id("Alice"), LeaveReason.LEFT);
        engine.join(game, "Alice");

        engine.games().startCountdown(game);

        assertEquals(List.of("Frank", "Bob", "Carol"), team(game, 0));
        assertEquals(List.of("Dave", "Erin", "Alice"), team(game, 1));
    }

    @Test
    void aPartysSkillIsItsAverageNotItsSum() {
        Game game = waiting(2);
        engine.party(game, "Alice", "Bob");
        engine.join(game, "Carol", "Dave", "Erin");
        // Same average (1.0) as two equal 1.0s, but different enough that a first-member or maximum shortcut,
        // instead of the true average, would place Erin on the wrong team.
        engine.skill("Alice", 1.8);
        engine.skill("Bob", 0.2);
        engine.skill("Carol", 1.5);
        engine.skill("Dave", 1.5);
        engine.skill("Erin", 0.0);

        engine.games().startCountdown(game);

        assertEquals(List.of("Alice", "Bob", "Erin"), team(game, 0));
        assertEquals(List.of("Carol", "Dave"), team(game, 1));
    }
}
