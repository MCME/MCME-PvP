package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.integration.LegacyColors;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PaperFixture.ProxyMessage;
import com.mcmiddleearth.pvp.testing.TestArenas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The chat side of game events, sent only to the game's players and spectators. */
class TextAnnouncerTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        fixture.listeners().add(new TextAnnouncer(fixture.services()));
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void anOpenedGameIsAnnouncedInTheLobbyAndOnTheNetwork() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        fixture.join(fixture.open("AS-FFA"), alice);
        PaperFixture.chat(alice);
        PaperFixture.chat(dave);

        Game game = fixture.open("AS-TDM");

        assertEquals(List.of("Tester opened Team Deathmatch on AS-TDM (" + game.maxPlayers() + " players). Use /pvp to join!"),
                PaperFixture.chat(dave));
        assertEquals(List.of(), PaperFixture.chat(alice));
        ProxyMessage broadcast = fixture.proxyMessages().getLast();
        assertEquals(List.of("Message", "ALL", "§7Tester has opened a game of §aTeam Deathmatch§7 on "
                + "§aAS-TDM§7. Use §a/pvp§7 on the PvP server to join!"), broadcast.parts());
    }

    @Test
    void aLockedServerSendsNoOpenedNoticeToPlayersItKeepsOut() {
        PlayerMock carol = fixture.player("Carol");
        PlayerMock dave = fixture.player("Dave");
        dave.setOp(true);
        fixture.services().lock().lock();

        Game game = fixture.open("AS-TDM");
        String opened = "Tester opened Team Deathmatch on AS-TDM (" + game.maxPlayers()
                + " players). Use /pvp to join!";

        assertEquals(List.of(), PaperFixture.chat(carol));
        assertEquals(List.of(opened), PaperFixture.chat(dave));
    }

    @Test
    void joinsAndLeavesAreToldToTheGameAndThePlayer() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        PlayerMock dave = fixture.player("Dave");
        Game game = fixture.open("AS-TDM");
        drain(alice, bob, dave);

        fixture.join(game, alice, bob);
        fixture.services().games().leave(bob.getUniqueId(), LeaveReason.LEFT);

        assertEquals(List.of("You joined AS-TDM (Team Deathmatch).", "Bob joined (2/" + game.maxPlayers() + ").", "Bob left."),
                PaperFixture.chat(alice));
        assertEquals(List.of("You joined AS-TDM (Team Deathmatch).", "You left the game."), PaperFixture.chat(bob));
        assertEquals(List.of(), PaperFixture.chat(dave));
    }

    @Test
    void aLateJoinerLearnsTheirTeamAndTheGameHearsIt() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, fixture.player("Bob"));
        drain(alice, carol);

        fixture.join(game, carol);

        String team = color(game, carol).name();
        assertEquals(List.of("You joined AS-TDM (Team Deathmatch).", "You're on team " + team + "."), PaperFixture.chat(carol));
        assertEquals(List.of("Carol joined the fight on " + team + "."), PaperFixture.chat(alice));
    }

    @Test
    void spectatorsAreTold() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice);
        drain(alice, carol);

        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertEquals(List.of("You're spectating AS-TDM."), PaperFixture.chat(carol));
        assertEquals(List.of(), PaperFixture.chat(alice));
    }

    @Test
    void aSpectatorLeavingIsNotAnnouncedEither() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice);
        fixture.services().games().spectate(PaperFixture.ref(carol), game);
        drain(alice, carol);

        fixture.services().games().leave(carol.getUniqueId(), LeaveReason.LEFT);

        assertEquals(List.of(), PaperFixture.chat(alice));
    }

    @Test
    void theCountdownCountsFromFiveToOneThenStarts() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, fixture.player("Bob"));
        PaperFixture.chat(alice);

        fixture.startAndRun(game);

        assertEquals(List.of("The game starts in 7 seconds!", "You're on team " + color(game, alice).name() + ".",
                "Game begins in 5", "Game begins in 4", "Game begins in 3", "Game begins in 2", "Game begins in 1",
                "Game Start!"), PaperFixture.chat(alice));
    }

    @Test
    void aOneSecondCountdownUsesTheSingular() {
        PaperFixture oneSecond = withCountdownSeconds(1);
        PlayerMock alice = oneSecond.player("Alice");
        Game game = oneSecond.open("AS-TDM");
        oneSecond.join(game, alice, oneSecond.player("Bob"));
        PaperFixture.chat(alice);

        oneSecond.services().games().startCountdown(game);

        assertTrue(PaperFixture.chat(alice).contains("The game starts in 1 second!"));
    }

    @Test
    void aCountdownWithoutEnoughPlayersIsCancelled() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob);
        fixture.services().games().startCountdown(game);
        PaperFixture.chat(alice);

        fixture.services().games().leave(bob.getUniqueId(), LeaveReason.LEFT);

        assertEquals(List.of("Bob left.", "Countdown cancelled: not enough players."), PaperFixture.chat(alice));
    }

    @Test
    void freeForAllWarnsAt30SecondsAndEverySecondFrom10() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.services().games().open("AS-FFA", Map.of("timeLimit", 1), "Tester");
        fixture.join(game, alice, fixture.player("Bob"));
        fixture.startAndRun(game);
        PaperFixture.chat(alice);

        fixture.runUntil(game, Phase.ENDING, 62);

        List<String> seen = PaperFixture.chat(alice);
        List<String> warnings = seen.stream().filter(line -> line.endsWith("remaining!")).toList();
        List<String> expected = new ArrayList<>(List.of("30 seconds remaining!"));
        for (int seconds = 10; seconds >= 2; seconds--) {
            expected.add(seconds + " seconds remaining!");
        }
        expected.add("1 second remaining!");
        assertEquals(expected, warnings);
        assertTrue(seen.contains("It's a draw."), "nobody scored");
    }

    @Test
    void aKillIsAnnouncedInTeamColorsAndTheEliminatedAreTold() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock loner = players.stream().filter(p -> teamSize(game, p) == 1).findFirst().orElseThrow();
        PlayerMock victim = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();
        drain(players.toArray(PlayerMock[]::new));

        victim.simulateDamage(100, loner);

        Component line = loner.nextComponentMessage();
        assertEquals(victim.getName() + " was killed by " + loner.getName(), PaperFixture.plain(line));
        String legacy = LegacyComponentSerializer.legacySection().serialize(line);
        assertTrue(legacy.contains(LegacyColors.code(color(game, victim).color()) + victim.getName()), legacy);
        assertTrue(legacy.contains(LegacyColors.code(color(game, loner).color()) + loner.getName()), legacy);
        assertTrue(PaperFixture.chat(victim).contains("You're out! You're spectating until the game ends."));
    }

    @Test
    void aPlayerWhoLeavesUnderFireIsAnnouncedAsKilledByTheOneWhoHitThem() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"),
                fixture.player("Dave"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock alice = players.getFirst();
        PlayerMock enemy = players.stream()
                .filter(p -> !color(game, p).equals(color(game, alice))).findFirst().orElseThrow();
        PlayerMock mate = players.stream().skip(1)
                .filter(p -> color(game, p).equals(color(game, alice))).findFirst().orElseThrow();
        alice.simulateDamage(5, enemy);
        drain(players.toArray(PlayerMock[]::new));

        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        String fled = "Alice left the fight and counts as killed by " + enemy.getName() + ".";
        assertEquals(List.of(fled, "Alice left."), PaperFixture.chat(mate));
        // She is on her way out, so she is not told that she spectates.
        assertEquals(List.of(fled, "You left the game."), PaperFixture.chat(alice));
    }

    @Test
    void aPlayerWhoLeavesUnderFireFromSomeoneWhoHasGoneIsAnnouncedAsDead() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        PlayerMock carol = fixture.player("Carol");
        running("AS-FFA", alice, bob, carol);
        alice.simulateDamage(5, bob);
        fixture.services().games().leave(bob.getUniqueId(), LeaveReason.LEFT);
        drain(alice, bob, carol);

        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertEquals(List.of("Alice left the fight and counts as dead.", "Alice left."), PaperFixture.chat(carol));
    }

    @Test
    void aTeamWinEndsWithTheSummary() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);
        drain(alice, bob);

        bob.simulateDamage(100, alice);

        List<String> seen = PaperFixture.chat(alice);
        String team = color(game, alice).name();
        assertEquals(List.of(
                "Bob was killed by Alice",
                "Game over — Team Deathmatch on AS-TDM",
                team + " won!",
                "Highest K/D: Alice (1.0), Bob (0.0)",
                "Most deaths: Bob (1), Alice (0)"), seen);
    }

    @Test
    void leavingDuringTheResultScreenIsNotAnnouncedToTheOthers() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);
        bob.simulateDamage(100, alice);
        drain(alice, bob);
        assertEquals(Phase.ENDING, game.phase());

        fixture.services().games().leave(bob.getUniqueId(), LeaveReason.LEFT);

        assertEquals(List.of("You left the game."), PaperFixture.chat(bob));
        assertEquals(List.of(), PaperFixture.chat(alice));
    }

    @Test
    void aFreeForAllSummaryNamesTheWinnerAndTheMostKills() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.services().games().open("AS-FFA", Map.of("timeLimit", 1), "Tester");
        fixture.join(game, alice, bob);
        fixture.startAndRun(game);
        bob.simulateDamage(100, alice);
        bob.respawn();
        server.getScheduler().performOneTick();
        PaperFixture.chat(alice);

        fixture.runUntil(game, Phase.ENDING, 62);

        List<String> seen = PaperFixture.chat(alice).stream().filter(line -> !line.endsWith("remaining!")).toList();
        assertEquals(List.of(
                "Game over — Free For All on AS-FFA",
                "Alice won with 1 kill!",
                "Highest K/D: Alice (1.0), Bob (0.0)",
                "Most kills: Alice (1), Bob (0)",
                "Most deaths: Bob (1), Alice (0)"), seen);
    }

    @Test
    void aStoppedGameSaysSo() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game running = running("AS-TDM", alice, fixture.player("Bob"));
        Game waiting = fixture.open("AS-FFA");
        fixture.join(waiting, carol);
        drain(alice, carol);

        fixture.services().games().stop(running);
        fixture.services().games().stop(waiting);

        assertTrue(PaperFixture.chat(alice).contains("The game on AS-TDM was stopped by staff."));
        assertEquals(List.of("The game on AS-FFA was stopped by staff."), PaperFixture.chat(carol));
    }

    @Test
    void anAbandonedGameTellsItsSpectators() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-FFA", alice, bob);
        fixture.services().games().spectate(PaperFixture.ref(carol), game);
        PaperFixture.chat(carol);

        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);
        fixture.services().games().leave(bob.getUniqueId(), LeaveReason.LEFT);

        List<String> seen = PaperFixture.chat(carol);
        assertTrue(seen.contains("Everyone left, so the game on AS-FFA ended."), seen.toString());
        assertEquals(Phase.ENDING, game.phase());
    }

    @Test
    void movingOnFromAnEndedGameIsNotAnnounced() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-TDM", alice, bob);
        bob.simulateDamage(100, alice);
        Game ffa = fixture.open("AS-FFA");
        drain(alice, bob);

        fixture.join(ffa, alice);

        assertEquals(List.of("You joined AS-FFA (Free For All)."), PaperFixture.chat(alice));
        assertEquals(List.of(), PaperFixture.chat(bob));
    }

    @Test
    void killDeathIsWrittenLike112() {
        assertEquals("3.0", SummaryLines.killDeath(3, 0));
        assertEquals("0.0", SummaryLines.killDeath(0, 0));
        assertEquals("0.33", SummaryLines.killDeath(1, 3));
        assertEquals("0.67", SummaryLines.killDeath(2, 3));
        assertEquals("1.5", SummaryLines.killDeath(3, 2));
        assertEquals("3.33", SummaryLines.killDeath(10, 3));
    }

    /** A second fixture, on its own arena store, whose countdown lasts {@code seconds}. */
    private PaperFixture withCountdownSeconds(int seconds) {
        PvpConfig d = PvpConfig.defaults();
        PvpConfig config = new PvpConfig(d.lobby(), d.worlds(), d.mainServer(), d.startLocked(), d.lavaDamage(),
                d.lobbyProtection(), seconds, d.endingSeconds(), d.modeDefaults());
        PaperFixture other = PaperFixture.create(server, dir.resolve("countdown" + seconds), config,
                TestArenas.tdm("AS-TDM", 0, 2));
        other.listeners().add(new TextAnnouncer(other.services()));
        return other;
    }

    private Game running(String arena, PlayerMock... players) {
        Game game = fixture.open(arena);
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private static void drain(PlayerMock... players) {
        for (PlayerMock player : players) {
            PaperFixture.chat(player);
        }
    }

    private static TeamSpec color(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().color().orElseThrow();
    }

    private static int teamSize(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().team().orElseThrow().members().size();
    }
}
