package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.mcmiddleearth.pvp.testing.UiTesting.labels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoViewsTest {

    @TempDir
    Path dir;

    private UiFixture ui;
    private InfoViews info;

    @BeforeEach
    void setUp() {
        ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        info = new InfoViews(ui.services);
    }

    @Test
    void theRulesListHasEveryModeWithItsRules() {
        NoticeScreen rules = info.rulesList();
        assertEquals("Rules", plain(rules.title()));
        assertEquals(List.of(
                "Team Deathmatch rules",
                "Two to four teams, and no respawns. The last team with players left wins.",
                "Free For All rules",
                "Everyone for themselves, madly killing everyone! Highest number of kills wins."), plain(rules.body()));
        assertEquals(List.of("Close"), labels(rules));
        assertTrue(rules.ok().action().isNone());
    }

    @Test
    void oneModesRulesAreANotice() {
        NoticeScreen rules = info.rules(ui.services.modes().get("ffa").orElseThrow());
        assertEquals("Free For All rules", plain(rules.title()));
        assertEquals(List.of("Everyone for themselves, madly killing everyone! Highest number of kills wins."),
                plain(rules.body()));
    }

    @Test
    void statsShowTheLifetimeCountersUnderTheCurrentName() {
        Viewer alice = ui.player("Alice");
        // Stats imported from 1.1.2 before a first 3.0 game carry the uuid, not a name.
        PlayerStats imported = new PlayerStats(1, alice.id(), alice.id().toString(), 10, 4, 7, 3, 4, 2, List.of());
        NoticeScreen stats = info.stats(imported, "Alice");
        assertEquals("Stats", plain(stats.title()));
        assertEquals(List.of(
                "Stats for Alice",
                "Kills: 10 · Deaths: 4 · K/D: 2.50",
                "Games played: 7 · Won: 3 · Lost: 4 · Spectated: 2"), plain(stats.body()));
    }

    @Test
    void aPlayerWithoutStatsIsToldSo() {
        NoticeScreen stats = info.statsOf(ui.player("Alice"));
        assertEquals(List.of("No PvP stats for Alice."), plain(stats.body()));
    }

    @Test
    void aPlayerWithStoredStatsShowsThemUnderTheirCurrentName() throws IOException {
        Viewer alice = ui.player("Alice");
        // A 1.1.2 file (name: the dashed uuid, no extension) carries no name, unlike the player's current one.
        Path legacyFile = dir.resolve("stats").resolve(alice.id().toString());
        Files.createDirectories(legacyFile.getParent());
        Files.writeString(legacyFile,
                "{\"kills\":5,\"deaths\":2,\"gamesPlayed\":3,\"gamesWon\":2,\"gamesLost\":1,\"gamesSpectated\":0}");
        NoticeScreen stats = info.statsOf(alice);
        assertEquals(List.of(
                "Stats for Alice",
                "Kills: 5 · Deaths: 2 · K/D: 2.50",
                "Games played: 3 · Won: 2 · Lost: 1 · Spectated: 0"), plain(stats.body()));
    }

    @Test
    void teamScoresListEachTeamWithThePlayersItHasLeft() {
        Game game = ui.open("AS-TDM");
        ui.join(game, ui.player("Alice"), ui.player("Bob"));
        ui.games().startCountdown(game);
        NoticeScreen scores = info.scores(game);
        assertEquals("Teams and scores", plain(scores.title()));
        assertEquals(List.of(
                "AS-TDM · Team Deathmatch · 2/" + game.maxPlayers() + " · starting",
                "Red 1 left",
                "Blue 1 left"), plain(scores.body()));
    }

    @Test
    void aWaitingGamesScoresListTheGameLineAndTheWaitingPlayers() {
        Game game = ui.open("AS-TDM");
        ui.join(game, ui.player("Alice"), ui.player("Bob"));
        assertEquals(List.of(
                "AS-TDM · Team Deathmatch · 2/" + game.maxPlayers() + " · waiting",
                "Waiting: Alice, Bob"),
                plain(info.scores(game).body()));
    }

    @Test
    void endedGameScoresShowTheResultAndStayFixedWhileAPlayerMovesOn() {
        Game game = ui.open("AS-TDM");
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        String aliceTeam = game.session(alice.id()).orElseThrow().team().orElseThrow().spec().name();
        int redAlive = aliceTeam.equals("Red") ? 1 : 0;
        int blueAlive = aliceTeam.equals("Blue") ? 1 : 0;

        game.handleDeath(bob.id(), alice.id());

        assertEquals(Phase.ENDING, game.phase());
        List<String> expected = List.of(
                "AS-TDM · Team Deathmatch · 2/" + game.maxPlayers() + " · ending",
                aliceTeam + " won!",
                "Red " + redAlive + " left",
                "Blue " + blueAlive + " left");
        assertEquals(expected, plain(info.scores(game).body()));

        // Alice, the alive winner, moves on to another game; the standings of the ended one must not change.
        Game ffa = ui.open("AS-FFA");
        ui.join(ffa, alice);

        assertEquals(expected, plain(info.scores(game).body()));
    }

    @Test
    void endedFfaGameScoresShowEachPresentPlayerByScoreAndThatStaffStoppedIt() {
        Game game = ui.open("AS-FFA");
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        game.handleDeath(alice.id(), bob.id());

        ui.games().stop(game);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(List.of(
                "AS-FFA · Free For All · 2/" + game.maxPlayers() + " · ending",
                "The game on AS-FFA was stopped by staff.",
                "Bob 1",
                "Alice 0"), plain(info.scores(game).body()));
    }

    @Test
    void aParticipantWhoLeftAndCameBackToWatchIsNotInTheEndedStandings() {
        Game game = ui.open("AS-FFA", Map.of("midgameJoin", false));
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.games().leave(alice.id(), LeaveReason.LEFT);
        ui.join(game, alice);
        assertEquals(Role.SPECTATOR, game.session(alice.id()).orElseThrow().role());

        ui.games().stop(game);

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(List.of(
                "AS-FFA · Free For All · 1/" + game.maxPlayers() + " · ending",
                "The game on AS-FFA was stopped by staff.",
                "Bob 0"), plain(info.scores(game).body()));
    }

    @Test
    void aWaitingGameWithNoPlayersShowsNoWaitingLine() {
        Game game = ui.open("AS-TDM");
        assertEquals(List.of("AS-TDM · Team Deathmatch · 0/" + game.maxPlayers() + " · waiting"),
                plain(info.scores(game).body()));
    }

    @Test
    void withoutTeamsThePlayersAreListedByScore() {
        Game game = ui.open("AS-FFA");
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        game.handleDeath(alice.id(), bob.id());
        List<String> body = plain(info.scores(game).body());
        assertEquals(List.of("Bob 1", "Alice 0"), body.subList(1, body.size()));
    }

    @Test
    void aPlayerWhoLeftARunningGameIsNotListed() {
        Game game = ui.open("AS-FFA");
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        ui.join(game, alice, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        ui.games().leave(alice.id(), LeaveReason.LEFT);
        List<String> body = plain(info.scores(game).body());
        assertEquals(List.of("Bob 0"), body.subList(1, body.size()));
    }
}
