package com.mcmiddleearth.pvp.mode.tdm;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.text.Msg;
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

class TeamDeathmatchTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    /** A running TDM game. Everyone's skill is 0, so players alternate over the teams in join order. */
    private Game running(int teams, String... names) {
        Arena arena = TestArenas.tdm("AS-TDM", 0, teams);
        engine = new TestEngine(dir, new ModeRegistry(List.of(new TeamDeathmatch())), PvpConfig.defaults(), arena);
        Game game = engine.open("AS-TDM", Map.of("teamCount", teams));
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    private String team(Game game, String name) {
        return engine.session(game, name).team().orElseThrow().id();
    }

    @Test
    void theRulesAreThoseOf112() {
        TeamDeathmatch tdm = new TeamDeathmatch();

        assertEquals("tdm", tdm.id());
        assertEquals(List.of(Setting.TEAM_COUNT, Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN), tdm.rules().settings());
        assertTrue(tdm.rules().balanced());
        assertEquals(0, tdm.rules().killScore());
        assertEquals(Optional.of(tdm), new ModeRegistry(List.of(tdm)).byLegacyName("Team Deathmatch"));
    }

    @Test
    void theLastTeamStandingWinsWithTwoTeams() {
        Game game = running(2, "Alice", "Bob", "Carol", "Dave");
        List<String> red = game.teams().get(0).members().stream().map(PlayerSession::name).toList();
        List<String> blue = game.teams().get(1).members().stream().map(PlayerSession::name).toList();

        engine.kill(game, red.get(0), blue.get(0));
        assertEquals(Phase.RUNNING, game.phase());
        engine.kill(game, red.get(1), blue.get(1));

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team("blue", EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void theLastTeamStandingWinsWithThreeTeams() {
        Game game = running(3, "Alice", "Bob", "Carol");
        assertEquals(List.of("red", "blue", "green"), game.teams().stream().map(t -> t.id()).toList());

        engine.kill(game, "Carol", "Alice");
        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(0, game.team("green").orElseThrow().aliveCount());

        engine.kill(game, "Bob", "Alice");

        assertEquals(Optional.of(Outcome.team(team(game, "Alice"), EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void theDeadSpectateFromTheArenaSpawn() {
        Game game = running(2, "Alice", "Bob", "Carol", "Dave");
        String victim = game.teams().get(0).members().get(0).name();

        engine.kill(game, victim, null);
        PlayerSession dead = engine.session(game, victim);

        assertFalse(dead.alive());
        assertEquals(Role.PLAYER, dead.role());
        assertTrue(dead.present());
        assertEquals(1, dead.deaths());
        assertEquals(Optional.of(game.arena().spawn()), game.respawnPoint(dead.id()));

        game.handleRespawn(dead.id());
        engine.context(game).advanceTicks(1);
        assertTrue(engine.context(game).spectating().contains(dead.id()));
        assertEquals(List.of("died " + victim), engine.events().events("died"));
    }

    @Test
    void aDeadPlayerCannotDieAgain() {
        Game game = running(2, "Alice", "Bob", "Carol", "Dave");
        String victim = game.teams().get(0).members().get(0).name();

        engine.kill(game, victim, null);
        engine.kill(game, victim, null);

        assertEquals(1, engine.session(game, victim).deaths());
    }

    @Test
    void killingATeammateDoesNotCount() {
        Game game = running(2, "Alice", "Bob", "Carol", "Dave");
        List<PlayerSession> red = game.teams().get(0).members();
        List<PlayerSession> blue = game.teams().get(1).members();

        engine.kill(game, red.get(0).name(), red.get(1).name());
        engine.kill(game, blue.get(0).name(), red.get(1).name());

        assertEquals(1, red.get(1).kills());
        assertEquals(1, red.get(0).deaths());
        assertEquals(List.of("died " + red.get(0).name(), "died " + blue.get(0).name() + " by " + red.get(1).name()),
                engine.events().events("died"));
    }

    @Test
    void aDeathDuringTheCountdownDoesNotCountAndThePlayerRespawnsReady() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        engine = new TestEngine(dir, new ModeRegistry(List.of(new TeamDeathmatch())), PvpConfig.defaults(), arena);
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);

        engine.kill(game, "Alice", "Bob");

        assertEquals(0, engine.session(game, "Alice").deaths());
        assertTrue(engine.session(game, "Alice").alive());
        NamedPoint teamSpawn = game.arena().teamSpawns().get(team(game, "Alice")).get(0);
        assertEquals(Optional.of(teamSpawn), game.respawnPoint(id("Alice")));
        game.handleRespawn(id("Alice"));
        engine.context(game).advanceTicks(1);
        assertEquals(2, engine.context(game).log("kit " + id("Alice")).size());
    }

    /**
     * The respawn corner case: a death in the countdown's last second doesn't count (still COUNTDOWN, so
     * {@code death()} ignores it), but once the heartbeat ticks the game into RUNNING, the player is still "in the
     * fight" and must respawn on their team's spawn, not the arena spawn.
     */
    @Test
    void aDeathInTheCountdownsLastSecondRespawnsOnTheTeamSpawnOnceTheGameIsRunning() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        engine = new TestEngine(dir, new ModeRegistry(List.of(new TeamDeathmatch())), PvpConfig.defaults(), arena);
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);
        engine.seconds(game, 6);

        engine.kill(game, "Alice", "Bob");
        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(0, engine.session(game, "Alice").deaths());

        engine.seconds(game, 1);
        assertEquals(Phase.RUNNING, game.phase());

        assertTrue(engine.session(game, "Alice").alive());
        NamedPoint teamSpawn = game.arena().teamSpawns().get(team(game, "Alice")).get(0);
        assertEquals(Optional.of(teamSpawn), game.respawnPoint(id("Alice")));

        game.handleRespawn(id("Alice"));
        engine.context(game).advanceTicks(1);
        assertEquals(2, engine.context(game).log("kit " + id("Alice")).size());
    }

    @Test
    void outsideTheFightEveryoneRespawnsAtTheArenaSpawn() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        engine = new TestEngine(dir, new ModeRegistry(List.of(new TeamDeathmatch())), PvpConfig.defaults(), arena);
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice");
        engine.spectate(game, "Carol");

        assertEquals(Optional.of(arena.spawn()), game.respawnPoint(id("Alice")));
        assertEquals(Optional.of(arena.spawn()), game.respawnPoint(id("Carol")));
        assertEquals(Optional.empty(), game.respawnPoint(id("Nobody")));

        game.handleRespawn(id("Alice"));
        game.handleRespawn(id("Carol"));
        engine.context(game).advanceTicks(1);

        assertEquals(2, engine.context(game).log("reset " + id("Alice")).size());
        assertEquals(2, engine.context(game).log("spectator " + id("Carol") + " true").size());
    }

    @Test
    void aQuitCanEndTheGame() {
        Game game = running(2, "Alice", "Bob");

        engine.games().leave(id("Alice"), LeaveReason.QUIT);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team(team(game, "Bob"), EndReason.WIN_CONDITION)), game.outcome());
    }

    @Test
    void moreTeamsThanTheArenaHasSpawnSetsAreRefused() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        engine = new TestEngine(dir, new ModeRegistry(List.of(new TeamDeathmatch())), PvpConfig.defaults(), arena);

        PvpException three = assertThrows(PvpException.class, () -> engine.open("AS-TDM", Map.of("teamCount", 3)));
        PvpException five = assertThrows(PvpException.class, () -> engine.open("AS-TDM", Map.of("teamCount", 5)));

        // The arena supports up to 2 teams.
        assertEquals(Msg.ERROR_ARENA_TEAMS, three.messageKey());
        assertEquals(Map.of("arena", "AS-TDM", "max", "2"), three.args());
        // Team Deathmatch never has 5 teams, so that is a plain range error.
        assertEquals(Msg.ERROR_SETTING_RANGE, five.messageKey());
    }

    @Test
    void theSummaryListsKillsKillDeathAndDeaths() {
        Game game = running(2, "Alice", "Bob", "Carol", "Dave");
        List<PlayerSession> red = game.teams().get(0).members();
        List<PlayerSession> blue = game.teams().get(1).members();
        PlayerSession ace = blue.get(0);

        engine.kill(game, red.get(0).name(), ace.name());
        engine.kill(game, blue.get(1).name(), red.get(1).name());

        assertEquals(List.of(ace, red.get(1)), game.scores().mostKills(2));
        assertEquals(ace, game.scores().bestKillDeath(1).get(0));
        List<PlayerSession> deaths = game.scores().mostDeaths(2);
        assertEquals(1, deaths.get(0).deaths());
        assertEquals(1, deaths.get(1).deaths());
        assertTrue(deaths.get(0).name().compareToIgnoreCase(deaths.get(1).name()) < 0);
    }

    @Test
    void teamColorsAreThePaletteColors() {
        Game game = running(2, "Alice", "Bob");

        assertEquals(Optional.of(Palette.RED), game.teams().get(0).members().get(0).color());
        assertEquals(Optional.of(Palette.BLUE), game.teams().get(1).members().get(0).color());
    }
}
