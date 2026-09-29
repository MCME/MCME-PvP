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

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTrackerTest {

    private static final TestMode DUEL = new TestMode("duel", TestMode.twoTeams().build());

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(String... names) {
        engine = new TestEngine(dir, new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("duel"));
        Game game = engine.open("AS-TDM");
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    @Test
    void aPlayersPointsAlsoCountForTheirTeam() {
        Game game = running("Alice", "Bob");
        PlayerSession alice = engine.session(game, "Alice");

        game.scores().addScore(alice, 3);

        assertEquals(3, alice.score());
        assertEquals(3, game.teams().get(0).score());
        assertEquals(0, game.teams().get(1).score());
        assertEquals(1, engine.events().scoreChanges());
    }

    @Test
    void teamPointsDoNotGoToThePlayers() {
        Game game = running("Alice", "Bob");

        game.scores().addTeamScore(game.teams().get(1), 2);

        assertEquals(2, game.teams().get(1).score());
        assertEquals(0, engine.session(game, "Bob").score());
        assertEquals(1, engine.events().scoreChanges());
    }

    @Test
    void theRankingSortsByScoreThenName() {
        Game game = running("Carol", "alice", "Bob", "Dave");
        game.scores().addScore(engine.session(game, "Dave"), 2);
        game.scores().addScore(engine.session(game, "Bob"), 1);

        assertEquals(List.of("Dave", "Bob", "alice", "Carol"),
                game.scores().ranking().stream().map(PlayerSession::name).toList());
    }

    @Test
    void theTeamRankingSortsByScoreThenPaletteOrder() {
        Game game = running("Alice", "Bob");
        assertEquals(List.of("red", "blue"), game.scores().teamRanking().stream().map(GameTeam::id).toList());

        game.scores().addTeamScore(game.teams().get(1), 1);

        assertEquals(List.of("blue", "red"), game.scores().teamRanking().stream().map(GameTeam::id).toList());
    }

    @Test
    void killsKillDeathAndDeathsRankPlayersDifferently() {
        // Today TeamDeathmatchTest's summary test gives everyone 0 or 1 kill, so mostKills and bestKillDeath
        // cannot be told apart there; these counts make all three summaries disagree. Bob and Dave, the tied
        // pair below, join in reverse name order so the name tie-break is what puts them back in order.
        Game game = running("Alice", "Dave", "Carol", "Bob");
        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        PlayerSession carol = engine.session(game, "Carol");
        PlayerSession dave = engine.session(game, "Dave");

        alice.addKill();
        alice.addKill();
        alice.addKill();
        alice.addDeath();
        alice.addDeath();
        alice.addDeath();
        bob.addKill();
        bob.addKill();
        carol.addKill();
        carol.addDeath();
        carol.addDeath();
        carol.addDeath();
        carol.addDeath();
        // Dave: no kills, no deaths.

        assertEquals(List.of(alice, bob, carol, dave), game.scores().mostKills(4));
        assertEquals(List.of(bob, alice, carol, dave), game.scores().bestKillDeath(4));
        // Bob and Dave are tied on 0 deaths; the tracker breaks ties by name.
        assertEquals(List.of(carol, alice, bob, dave), game.scores().mostDeaths(4));
        // A limit past the player count still returns everyone, not an error or a short list.
        assertEquals(4, game.scores().mostKills(100).size());

        // A game nobody has started yet has players but no participants.
        TestEngine idle = new TestEngine(dir, new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM-IDLE", 0, 2).withMode("duel"));
        Game waiting = idle.open("AS-TDM-IDLE");
        idle.join(waiting, "Erin", "Frank");
        assertEquals(List.of(), waiting.scores().mostDeaths(4));
    }

    @Test
    void theRankingBreaksTiesByKillsThenDeathsThenName() {
        // Dave joins before Carol, the reverse of their name order, so a correct result can only come from the
        // name tie-break, never from join order.
        Game game = running("Dave", "Bob", "Carol", "Alice");
        PlayerSession alice = engine.session(game, "Alice");
        PlayerSession bob = engine.session(game, "Bob");
        PlayerSession carol = engine.session(game, "Carol");
        PlayerSession dave = engine.session(game, "Dave");

        // Same score (0) throughout. Alice and Bob share the most kills; Bob's fewer deaths must put him first.
        alice.addKill();
        alice.addKill();
        for (int i = 0; i < 5; i++) {
            alice.addDeath();
        }
        bob.addKill();
        bob.addKill();
        bob.addDeath();
        // Carol and Dave share fewer kills and the same (zero) deaths, so only their names decide the order.
        carol.addKill();
        dave.addKill();
        engine.spectate(game, "Eve");

        assertEquals(List.of("Bob", "Alice", "Carol", "Dave"),
                game.scores().ranking().stream().map(PlayerSession::name).toList());
    }
}
