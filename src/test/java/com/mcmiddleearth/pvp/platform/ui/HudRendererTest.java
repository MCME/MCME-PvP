package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.platform.ActionBarHolds;
import com.mcmiddleearth.pvp.platform.PaperGameContextFactory;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudRendererTest {

    @TempDir
    Path dir;

    private final List<String> titles = new ArrayList<>();
    private ServerMock server;
    private PaperGameContextFactory contexts;
    private GameManager games;
    private ActionBarHolds holds;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        PaperFixture fixture = PaperFixture.create(server, dir,
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 6));
        contexts = fixture.contexts();
        games = fixture.services().games();
        holds = new ActionBarHolds(server::getCurrentTick);
        fixture.listeners().add(new HudRenderer(fixture.services(), contexts, holds, new TitleSender() {
            @Override
            public void show(Player player, Title title) {
                titles.add(player.getName() + ": " + plain(title.title()));
            }

            @Override
            public void clear(Player player) {
                titles.add(player.getName() + ": (cleared)");
            }
        }));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock join(Game game, String name) {
        PlayerMock player = server.addPlayer(name);
        games.join(JoinUnit.solo(new PlayerRef(player.getUniqueId(), name)), game);
        return player;
    }

    private void seconds(int count) {
        server.getScheduler().performTicks(20L * count);
    }

    private Objective sidebar(Game game) {
        return contexts.scoreboard(game.id()).orElseThrow().board().getObjective(DisplaySlot.SIDEBAR);
    }

    private static List<BossBar> bars(PlayerMock player) {
        List<BossBar> bars = new ArrayList<>();
        player.activeBossBars().forEach(bars::add);
        return bars;
    }

    @Test
    void theSidebarTitleFollowsThePhase() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        join(game, "Alice");
        assertEquals("Team Deathmatch Waiting 1/" + game.maxPlayers(), plain(sidebar(game).displayName()));
        join(game, "Bob");
        games.startCountdown(game);
        assertEquals("Team Deathmatch Starting in 7s", plain(sidebar(game).displayName()));
        seconds(7);
        assertEquals(Phase.RUNNING, game.phase());
        assertEquals("Team Deathmatch", plain(sidebar(game).displayName()));
    }

    @Test
    void eachGameShowsItsOwnSidebarToItsOwnPlayersOnly() {
        Game tdm = games.open("AS-TDM", Map.of(), "Console");
        Game ffa = games.open("AS-FFA", Map.of(), "Console");
        PlayerMock alice = join(tdm, "Alice");
        PlayerMock carol = join(ffa, "Carol");
        PlayerMock dave = server.addPlayer("Dave");
        assertEquals("Team Deathmatch Waiting 1/" + tdm.maxPlayers(),
                plain(alice.getScoreboard().getObjective(DisplaySlot.SIDEBAR).displayName()));
        assertEquals("Free For All Waiting 1/" + ffa.maxPlayers(),
                plain(carol.getScoreboard().getObjective(DisplaySlot.SIDEBAR).displayName()));
        assertNull(dave.getScoreboard().getObjective(DisplaySlot.SIDEBAR));
    }

    @Test
    void teamLinesCountThePlayersEachTeamHasLeft() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        Objective sidebar = sidebar(game);
        assertEquals(1, sidebar.getScore("§cRed").getScore());
        assertEquals(1, sidebar.getScore("§9Blue").getScore());

        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());

        String bobsTeam = game.session(bob.getUniqueId()).orElseThrow().team().orElseThrow().id().equals("red")
                ? "§cRed" : "§9Blue";
        assertEquals(0, sidebar.getScore(bobsTeam).getScore());
    }

    @Test
    void withoutTeamsTheSidebarShowsTheTopFiveByScore() {
        Game game = games.open("AS-FFA", Map.of(), "Console");
        List<PlayerMock> players = new ArrayList<>();
        for (String name : List.of("Alice", "Bob", "Carol", "Dave", "Erin", "Frank")) {
            players.add(join(game, name));
        }
        games.startCountdown(game);
        seconds(7);
        game.handleDeath(players.get(1).getUniqueId(), players.get(0).getUniqueId());
        game.handleDeath(players.get(2).getUniqueId(), players.get(0).getUniqueId());
        game.handleDeath(players.get(0).getUniqueId(), players.get(3).getUniqueId());

        Objective sidebar = sidebar(game);
        assertEquals("Free For All Time 10:00", plain(sidebar.displayName()));
        assertEquals(2, sidebar.getScore("Alice").getScore());
        assertEquals(1, sidebar.getScore("Dave").getScore());
        // Ties at 0 go by kills, then fewer deaths, then name: Erin, Frank, Bob, and Carol is sixth.
        for (String listed : List.of("Erin", "Frank", "Bob")) {
            assertTrue(sidebar.getScore(listed).isScoreSet(), listed);
        }
        assertFalse(sidebar.getScore("Carol").isScoreSet(), "the sixth player is not listed");
    }

    @Test
    void theBossBarCountsDownForTheGamesAudienceOnly() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        join(game, "Erin");
        // A fourth player keeps Bob's team occupied when he leaves: a team emptied during the countdown stops it.
        join(game, "Frank");
        PlayerMock carol = server.addPlayer("Carol");
        games.spectate(new PlayerRef(carol.getUniqueId(), "Carol"), game);
        PlayerMock dave = server.addPlayer("Dave");
        assertTrue(bars(alice).isEmpty());

        games.startCountdown(game);
        seconds(1);

        BossBar bar = bars(alice).get(0);
        assertEquals("Starting in 6s", plain(bar.name()));
        assertEquals(6f / 7f, bar.progress(), 0.001);
        assertEquals(List.of(bar), bars(bob));
        assertEquals(List.of(bar), bars(carol));
        assertTrue(bars(dave).isEmpty());

        games.leave(bob.getUniqueId(), LeaveReason.LEFT);
        assertEquals(Phase.COUNTDOWN, game.phase());
        assertTrue(bars(bob).isEmpty());
        assertEquals(List.of(bar), bars(alice));
    }

    @Test
    void aTimeLimitKeepsTheBarAndTeamDeathmatchDropsIt() {
        Game ffa = games.open("AS-FFA", Map.of("timeLimit", 1), "Console");
        PlayerMock alice = join(ffa, "Alice");
        join(ffa, "Bob");
        games.startCountdown(ffa);
        seconds(8);
        assertEquals("Free For All · 0:59 left", plain(bars(alice).get(0).name()));

        Game tdm = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock carol = join(tdm, "Carol");
        join(tdm, "Dave");
        games.startCountdown(tdm);
        seconds(7);
        assertEquals(Phase.RUNNING, tdm.phase());
        assertTrue(bars(carol).isEmpty(), "Team Deathmatch has no time limit");
    }

    @Test
    void closingAGameHidesItsBar() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        join(game, "Bob");
        games.startCountdown(game);
        assertEquals(1, bars(alice).size());
        games.stop(game);
        assertTrue(bars(alice).isEmpty());
    }

    @Test
    void theActionBarShowsKillsDeathsOrWatching() {
        Game game = games.open("AS-FFA", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        PlayerMock carol = server.addPlayer("Carol");
        games.spectate(new PlayerRef(carol.getUniqueId(), "Carol"), game);
        games.startCountdown(game);
        seconds(7);
        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());
        drainActionBars(alice);
        drainActionBars(carol);

        seconds(1);

        assertEquals("Kills 1 · Deaths 0", plain(alice.nextActionBar()));
        assertEquals("Spectating AS-FFA · /pvp leave", plain(carol.nextActionBar()));
    }

    @Test
    void anEliminatedPlayerIsToldOnTheActionBar() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        join(game, "Carol");
        join(game, "Dave");
        games.startCountdown(game);
        seconds(7);
        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());
        assertEquals(Phase.RUNNING, game.phase(), "two against two: Bob's team still has a player");
        drainActionBars(bob);
        seconds(1);
        assertEquals("You're out · spectating", plain(bob.nextActionBar()));
    }

    @Test
    void noActionBarIsSentBeforeTheGameIsRunning() {
        Game game = games.open("AS-FFA", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        join(game, "Bob");
        seconds(2);
        assertNull(alice.nextActionBar(), "waiting sends no action bar");

        games.startCountdown(game);
        seconds(2);
        assertNull(alice.nextActionBar(), "the countdown sends no action bar");
    }

    @Test
    void aHeldActionBarIsNotRedrawnUntilTheHoldExpires() {
        Game game = games.open("AS-FFA", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        drainActionBars(alice);
        holds.hold(alice.getUniqueId(), 40);

        seconds(1);
        assertNull(alice.nextActionBar(), "the hold still covers this second");

        seconds(1);
        assertEquals("Kills 0 · Deaths 0", plain(alice.nextActionBar()));
    }

    @Test
    void titlesMarkTheCountdownTheStartEliminationAndTheResult() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        assertEquals(List.of("Alice: 5", "Bob: 5", "Alice: 4", "Bob: 4", "Alice: 3", "Bob: 3", "Alice: 2", "Bob: 2",
                "Alice: 1", "Bob: 1", "Alice: Fight!", "Bob: Fight!"), titles);
        titles.clear();

        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());

        assertEquals(List.of("Bob: You're out", "Alice: Victory", "Bob: Defeat"), titles);
    }

    @Test
    void aSpectatorOfAGameThatEndsGetsNoResultTitle() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        PlayerMock carol = server.addPlayer("Carol");
        games.spectate(new PlayerRef(carol.getUniqueId(), "Carol"), game);
        games.startCountdown(game);
        seconds(7);
        titles.clear();

        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());

        assertTrue(titles.stream().noneMatch(t -> t.startsWith("Carol:")), titles.toString());
        assertTrue(titles.contains("Alice: Victory"), titles.toString());
    }

    @Test
    void aPlayerWhoFleesGetsNoTitleButTheOthersSidebarStillUpdates() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(game, "Alice");
        PlayerMock bob = join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        titles.clear();
        game.handleHit(alice.getUniqueId(), bob.getUniqueId());

        games.leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertTrue(titles.stream().noneMatch(t -> t.equals("Alice: You're out")), titles.toString());
        assertEquals(Phase.ENDING, game.phase());
        String bobsTeam = game.session(bob.getUniqueId()).orElseThrow().team().orElseThrow().id().equals("red")
                ? "§cRed" : "§9Blue";
        assertEquals(1, sidebar(game).getScore(bobsTeam).getScore());
    }

    @Test
    void aShutdownSendsNoResultTitle() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        join(game, "Alice");
        join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        titles.clear();

        games.closeAll();

        assertTrue(titles.isEmpty(), titles.toString());
        assertEquals(Phase.CLOSED, game.phase());
    }

    @Test
    void stoppingARunningGameSendsNoResultTitle() {
        Game game = games.open("AS-TDM", Map.of(), "Console");
        join(game, "Alice");
        join(game, "Bob");
        games.startCountdown(game);
        seconds(7);
        titles.clear();

        games.stop(game);

        assertTrue(titles.isEmpty(), titles.toString());
        assertEquals(Phase.ENDING, game.phase());
    }

    @Test
    void movingOnFromAnEndedGameClearsItsResultTitleAndSidebar() {
        Game tdm = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(tdm, "Alice");
        PlayerMock bob = join(tdm, "Bob");
        games.startCountdown(tdm);
        seconds(7);
        tdm.handleDeath(bob.getUniqueId(), alice.getUniqueId());
        Game ffa = games.open("AS-FFA", Map.of(), "Console");
        titles.clear();

        games.join(JoinUnit.solo(new PlayerRef(alice.getUniqueId(), "Alice")), ffa);

        assertEquals(List.of("Alice: (cleared)"), titles);
        assertEquals(contexts.scoreboard(ffa.id()).orElseThrow().board(), alice.getScoreboard());
        assertNull(contexts.scoreboard(tdm.id()).orElseThrow().board().getEntryTeam(alice.getName()));
        assertTrue(bars(alice).isEmpty());
        assertEquals(Phase.ENDING, tdm.phase());
    }

    @Test
    void anEndedGamesSidebarStaysAsItWasWhileAPlayerMovesOn() {
        Game tdm = games.open("AS-TDM", Map.of(), "Console");
        PlayerMock alice = join(tdm, "Alice");
        PlayerMock bob = join(tdm, "Bob");
        games.startCountdown(tdm);
        seconds(7);
        tdm.handleDeath(bob.getUniqueId(), alice.getUniqueId());
        Objective sidebar = sidebar(tdm);
        int redAtEnd = sidebar.getScore("§cRed").getScore();
        int blueAtEnd = sidebar.getScore("§9Blue").getScore();
        Game ffa = games.open("AS-FFA", Map.of(), "Console");

        games.join(JoinUnit.solo(new PlayerRef(alice.getUniqueId(), "Alice")), ffa);
        seconds(2);

        assertEquals(redAtEnd, sidebar.getScore("§cRed").getScore());
        assertEquals(blueAtEnd, sidebar.getScore("§9Blue").getScore());
    }

    private static void drainActionBars(PlayerMock player) {
        Component bar = player.nextActionBar();
        while (bar != null) {
            bar = player.nextActionBar();
        }
        assertNull(player.nextActionBar());
    }
}
