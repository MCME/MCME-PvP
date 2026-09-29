package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.stats.StatsRecorder;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PaperFixture.ProxyMessage;
import com.mcmiddleearth.pvp.testing.RecordingListener;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /pvpadmin, called directly. */
class AdminCommandsTest {

    private static final String LEGACY_MAP = """
            {"gmType":"Team Deathmatch","name":"OLD","title":"Old Keep","max":10,"curr":0,\
            "spawn":{"world":"world","x":300,"y":64,"z":300},\
            "importantPoints":{"RedSpawn":{"world":"world","x":295,"y":63,"z":295},\
            "BlueSpawn":{"world":"world","x":305,"y":63,"z":305}},\
            "regionPoints":[{"world":"world","x":290,"y":64,"z":290},{"world":"world","x":310,"y":64,"z":290},\
            {"world":"world","x":310,"y":64,"z":310},{"world":"world","x":290,"y":64,"z":310}],\
            "resourcePackURL":null}""";

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();
    private ServerMock server;
    private PaperFixture fixture;
    private AdminCommands commands;
    private PlayerMock staff;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4),
                TestArenas.tdm("OLD", 300, 2).unassigned("the map has no game mode"),
                TestArenas.tdm("UP", 400, 2).withMode("TDM"));
        commands = new AdminCommands(fixture.services(), new TextMenus(fixture.services()), log.logger());
        staff = fixture.player("Staff");
        staff.setOp(true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void startWithoutSettingsShowsAPlayerTheStartScreen() {
        commands.start(staff, Optional.empty(), Optional.empty());

        assertEquals("Arenas (3)", PaperFixture.chat(staff).getFirst());
        assertTrue(fixture.services().games().games().isEmpty());
    }

    @Test
    void startWithAnArenaButNoSettingsShowsThatArenasUsageLineAndOpensNoGame() {
        commands.start(staff, Optional.of("AS-TDM"), Optional.empty());

        String answer = PaperFixture.chat(staff).getFirst();
        assertTrue(answer.startsWith("Usage: /pvpadmin start AS-TDM"), answer);
        assertTrue(fixture.services().games().games().isEmpty());
    }

    @Test
    void startWithSettingsOpensTheGame() {
        commands.start(staff, Optional.of("AS-TDM"), Optional.of("teamCount=2 maxPlayers=8"));

        Game game = fixture.services().games().gameOn("AS-TDM").orElseThrow();
        assertEquals(8, game.maxPlayers());
        assertEquals(List.of("Opened Team Deathmatch on AS-TDM."), PaperFixture.chat(staff));
    }

    @Test
    void theConsoleOpensAtOnceWithTheDefaults() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        commands.start(console, Optional.of("AS-FFA"), Optional.empty());

        assertTrue(fixture.services().games().gameOn("AS-FFA").isPresent());
        assertEquals(List.of("Opened Free For All on AS-FFA."), PaperFixture.chat(console));
    }

    @Test
    void theConsolesStartWithoutAnArenaListsTheArenas() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        commands.start(console, Optional.empty(), Optional.empty());

        assertEquals("Arenas (4)", PaperFixture.chat(console).getFirst());
        assertTrue(fixture.services().games().games().isEmpty());
    }

    @Test
    void theOpenerIsTheStaffMemberOrTheConsole() {
        List<String> openedBy = new ArrayList<>();
        fixture.listeners().add(new GameListener() {
            @Override
            public void gameOpened(Game game, String opener) {
                openedBy.add(opener);
            }
        });
        ConsoleCommandSenderMock console = server.getConsoleSender();

        commands.start(staff, Optional.of("AS-TDM"), Optional.of("teamCount=2"));
        commands.start(console, Optional.of("AS-FFA"), Optional.empty());

        assertEquals(List.of("Staff", "Console"), openedBy);
    }

    @Test
    void badSettingsAreRefused() {
        commands.start(staff, Optional.of("AS-TDM"), Optional.of("teamCount"));
        commands.start(staff, Optional.of("AS-TDM"), Optional.of("colour=red"));

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("teamCount has no value: write settings as setting=value.", seen.getFirst());
        assertTrue(seen.get(1).startsWith("colour is not a setting of tdm."), seen.get(1));
        assertTrue(fixture.services().games().games().isEmpty());
    }

    @Test
    void moreTeamsThanTheArenaHasSpawnsForAreRefusedWithItsLimit() {
        commands.start(staff, Optional.of("AS-TDM"), Optional.of("teamCount=3"));

        assertEquals(List.of("AS-TDM supports up to 2 teams."), PaperFixture.chat(staff));
        assertTrue(fixture.services().games().games().isEmpty());
    }

    @Test
    void startingAnUnknownArenaIsRefused() {
        commands.start(staff, Optional.of("NOPE"), Optional.empty());

        assertEquals(List.of("There is no arena called NOPE."), PaperFixture.chat(staff));
    }

    @Test
    void everyStaffCommandNeedsItsPermission() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock arenaStaff = fixture.player("Builder");
        arenaStaff.addAttachment(fixture.plugin(), Permissions.STAFF_ARENA, true);
        PlayerMock kickOnly = fixture.player("Kicker");
        kickOnly.addAttachment(fixture.plugin(), Permissions.STAFF_KICK, true);
        fixture.open("AS-TDM");

        commands.start(alice, Optional.of("AS-FFA"), Optional.of("timeLimit=5"));
        commands.countdown(alice, "AS-TDM");
        commands.stop(alice, "AS-TDM");
        commands.kick(alice, "Staff");
        commands.kickAll(alice);
        commands.lock(alice);
        commands.unlock(alice);
        commands.arenaList(alice);
        commands.arenaInfo(alice, "AS-TDM");
        commands.arenaImport(alice, "AS-TDM");
        commands.lock(arenaStaff);
        // kickall needs pvp.staff.run: pvp.staff.kick alone (a plain moderator) is not enough.
        commands.kickAll(kickOnly);

        assertEquals(10, PaperFixture.chat(alice).stream().filter("You don't have permission to do that."::equals).count());
        assertEquals(List.of("You don't have permission to do that."), PaperFixture.chat(arenaStaff));
        assertEquals(List.of("You don't have permission to do that."), PaperFixture.chat(kickOnly));
        assertFalse(fixture.services().lock().locked());
        assertTrue(fixture.services().games().gameOn("AS-FFA").isEmpty());
        assertTrue(fixture.proxyMessages().isEmpty());
    }

    @Test
    void countdownAndStopActOnTheArenasGame() {
        Game game = fixture.open("AS-TDM");
        fixture.join(game, fixture.player("Alice"), fixture.player("Bob"));

        commands.countdown(staff, "AS-TDM");
        assertEquals(Phase.COUNTDOWN, game.phase());
        commands.stop(staff, "as-tdm");

        assertEquals(Phase.CLOSED, game.phase());
        assertEquals(List.of("Countdown started on AS-TDM.", "Stopped the game on AS-TDM."), PaperFixture.chat(staff));
    }

    @Test
    void countdownWithoutGameOrPlayersIsRefused() {
        commands.countdown(staff, "AS-TDM");
        fixture.open("AS-TDM");
        commands.countdown(staff, "AS-TDM");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("There is no game on AS-TDM.", seen.getFirst());
        assertTrue(seen.get(1).startsWith("At least 2 players are needed to start"), seen.get(1));
    }

    @Test
    void kickTakesThePlayerOutOfTheirGameAndOffTheServer() {
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        commands.kick(staff, "Alice");
        commands.kick(staff, "Zed");

        assertTrue(fixture.services().games().gameOf(alice.getUniqueId()).isEmpty());
        assertEquals(List.of(new ProxyMessage("Alice", List.of("ConnectOther", "Alice", "world"))), fixture.proxyMessages());
        assertEquals(List.of("Kicked Alice from the PvP server!", "No online player is called Zed."), PaperFixture.chat(staff));
    }

    @Test
    void kickRecordsTheLeaveReasonAsKicked() {
        RecordingListener listener = new RecordingListener();
        fixture.listeners().add(listener);
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        commands.kick(staff, "Alice");

        assertTrue(listener.events().contains("left Alice KICKED"), listener.events().toString());
    }

    @Test
    void kickingAPlayerInNoGameStillSendsThemAway() {
        PlayerMock carol = fixture.player("Carol");

        commands.kick(staff, "Carol");

        assertEquals(List.of("You were removed from PvP by staff."), PaperFixture.chat(carol));
        assertEquals(List.of(new ProxyMessage("Carol", List.of("ConnectOther", "Carol", "world"))),
                fixture.proxyMessages());
        assertEquals(List.of("Kicked Carol from the PvP server!"), PaperFixture.chat(staff));
    }

    @Test
    void aPlayerWhoseLeaveFailsIsStillKickedAndSentAway() {
        AtomicBoolean failing = new AtomicBoolean();
        PlayerMock alice = playerWithFailingScoreboard(failing, "Alice");
        fixture.join(fixture.open("AS-TDM"), alice);
        failing.set(true);

        commands.kick(staff, "Alice");

        assertTrue(log.messages(Level.WARNING).stream().anyMatch(m -> m.contains("Could not finish removing Alice")));
        assertEquals(List.of(new ProxyMessage("Alice", List.of("ConnectOther", "Alice", "world"))),
                fixture.proxyMessages());
        assertEquals(List.of("Kicked Alice from the PvP server!"), PaperFixture.chat(staff));
    }

    @Test
    void kickallRecordsTheLeaveReasonAsKicked() {
        RecordingListener listener = new RecordingListener();
        fixture.listeners().add(listener);
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        commands.kickAll(staff);

        assertTrue(listener.events().contains("left Alice KICKED"), listener.events().toString());
    }

    @Test
    void kickallSendsEveryoneWithoutTheLockPermissionAway() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.join(fixture.open("AS-TDM"), alice);

        commands.kickAll(staff);

        assertTrue(fixture.services().games().gameOf(alice.getUniqueId()).isEmpty());
        assertTrue(PaperFixture.chat(bob).contains("A PvP manager kicked all players"));
        assertEquals(List.of("Alice", "Bob"), fixture.proxyMessages().stream().map(ProxyMessage::through).sorted().toList());
        assertEquals(List.of("Sent 2 players to the main server."), PaperFixture.chat(staff));
    }

    @Test
    void kickallLeavesTheSenderInPlaceAndOutOfTheCount() {
        PlayerMock moderator = fixture.player("Moderator");
        moderator.addAttachment(fixture.plugin(), Permissions.STAFF_RUN, true);
        fixture.player("Alice");

        commands.kickAll(moderator);

        assertEquals(List.of("Sent 1 player to the main server."), PaperFixture.chat(moderator));
        assertEquals(List.of("Alice"), fixture.proxyMessages().stream().map(ProxyMessage::through).toList());
    }

    @Test
    void kickallStopsARunningGameAsADrawInsteadOfLettingSweepOrderPickAWinner() {
        List<MatchRecord> history = new ArrayList<>();
        fixture.listeners().add(new StatsRecorder(fixture.services().stats(), history::add));
        RecordingListener listener = new RecordingListener();
        fixture.listeners().add(listener);
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob);
        fixture.startAndRun(game);

        commands.kickAll(staff);

        assertTrue(listener.events().contains("ended AS-TDM draw STOPPED"), listener.events().toString());
        assertNull(history.getFirst().winner());
        assertTrue(history.getFirst().participants().stream().noneMatch(p -> MatchRecord.WON.equals(p.result())));
        assertEquals(0, fixture.services().stats().get(alice.getUniqueId(), "Alice").gamesWon());
        assertEquals(0, fixture.services().stats().get(bob.getUniqueId(), "Bob").gamesWon());
    }

    @Test
    void kickallLogsOneWarningNamingPlayersWhoseProxyChannelIsntReady() {
        PvpServices base = fixture.services();
        ProxyMessenger notReady = new ProxyMessenger(fixture.plugin(), base.config().mainServer(),
                (through, payload) -> { }, player -> false);
        PvpServices withProxy = new PvpServices(base.config(), base.messages(), base.arenas(), base.modes(),
                base.games(), base.stats(), base.lobby(), base.lock(), notReady);
        AdminCommands notReadyCommands = new AdminCommands(withProxy, new TextMenus(withProxy), log.logger());
        fixture.player("Alice");
        fixture.player("Bob");

        notReadyCommands.kickAll(staff);

        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("Alice"), warnings.getFirst());
        assertTrue(warnings.getFirst().contains("Bob"), warnings.getFirst());
        assertTrue(warnings.getFirst().contains("bungee-plugin-message-channel = true in velocity.toml"), warnings.getFirst());
    }

    @Test
    void aPlayerWhoseLeaveFailsIsStillSentAwayAndTheSweepGoesOn() {
        AtomicBoolean failing = new AtomicBoolean();
        PlayerMock alice = playerWithFailingScoreboard(failing, "Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.join(fixture.open("AS-TDM"), alice, bob);
        failing.set(true);

        commands.kickAll(staff);

        assertTrue(log.messages(Level.WARNING).stream().anyMatch(m -> m.contains("Could not finish removing Alice")));
        assertEquals(List.of("Alice", "Bob"),
                fixture.proxyMessages().stream().map(ProxyMessage::through).sorted().toList());
        assertEquals(List.of("Sent 2 players to the main server."), PaperFixture.chat(staff));
    }

    @Test
    void lockSendsPlayersAwayAndUnlockOpensAgain() {
        PlayerMock alice = fixture.player("Alice");

        commands.lock(staff);

        assertTrue(fixture.services().lock().locked());
        assertEquals(List.of("PvP-server Locked"), PaperFixture.chat(alice));
        assertEquals(List.of("Alice"), fixture.proxyMessages().stream().map(ProxyMessage::through).toList());

        commands.unlock(staff);

        assertFalse(fixture.services().lock().locked());
        assertEquals(List.of("PvP is locked. Only players with pvp.join.locked can stay.", "PvP is unlocked."),
                PaperFixture.chat(staff));
    }

    @Test
    void lockDuringARunningGameStopsItAsADraw() {
        List<MatchRecord> history = new ArrayList<>();
        fixture.listeners().add(new StatsRecorder(fixture.services().stats(), history::add));
        RecordingListener listener = new RecordingListener();
        fixture.listeners().add(listener);
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob);
        fixture.startAndRun(game);

        commands.lock(staff);

        assertTrue(listener.events().contains("ended AS-TDM draw STOPPED"), listener.events().toString());
        assertNull(history.getFirst().winner());
        assertTrue(history.getFirst().participants().stream().noneMatch(p -> MatchRecord.WON.equals(p.result())));
        assertEquals(0, fixture.services().stats().get(alice.getUniqueId(), "Alice").gamesWon());
        assertEquals(0, fixture.services().stats().get(bob.getUniqueId(), "Bob").gamesWon());
    }

    @Test
    void arenaListShowsPlayableAndUnassignedArenas() {
        commands.arenaList(staff);

        assertEquals(List.of(
                "Arenas (4)",
                "AS-FFA · Free For All",
                "AS-TDM · Team Deathmatch",
                "OLD · unassigned: the map has no game mode",
                "UP · Team Deathmatch"), PaperFixture.chat(staff));
    }

    @Test
    void arenaInfoDescribesOneArena() {
        commands.arenaInfo(staff, "as-tdm");
        commands.arenaInfo(staff, "NOPE");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("AS-TDM — AS-TDM", seen.get(0));
        assertEquals("Mode: Team Deathmatch · Max players: 16 · World: world", seen.get(1));
        assertEquals("Spawns: blue 1, red 1", seen.get(2));
        assertTrue(seen.get(3).startsWith("Region: "), seen.get(3));
        assertEquals("There is no arena called NOPE.", seen.get(4));
    }

    @Test
    void arenaInfoDescribesTheRegion() {
        commands.arenaInfo(staff, "AS-TDM");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("Region: 4 corners, x 0..19, z 0..19, y 0..1000", seen.get(3));
    }

    @Test
    void arenaInfoDescribesNoRegionAsTheWholeWorld() throws IOException {
        new ArenaStore(dir.resolve("arenas"), Json.pretty())
                .save(TestArenas.withoutRegion(TestArenas.ffa("NOREGION", 500, 2)));
        fixture.services().arenas().load();

        commands.arenaInfo(staff, "NOREGION");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("Region: none (the whole world)", seen.get(3));
    }

    @Test
    void aModeIdInCapitalsStillShowsTheModesName() {
        commands.arenaInfo(staff, "UP");

        List<String> seen = PaperFixture.chat(staff);
        assertTrue(seen.get(1).contains("Mode: Team Deathmatch"), seen.get(1));
    }

    @Test
    void arenaInfoOfAnUnassignedArenaAlsoShowsWhy() {
        commands.arenaInfo(staff, "OLD");

        List<String> seen = PaperFixture.chat(staff);
        assertTrue(seen.get(1).startsWith("Mode: unassigned "), seen.get(1));
        assertEquals("Unassigned: the map has no game mode", seen.get(4));
    }

    @Test
    void arenaInfoOfAnFfaArenaCountsItsNumberedSpawns() {
        commands.arenaInfo(staff, "AS-FFA");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("Spawns: 4 player spawns", seen.get(2));
    }

    @Test
    void arenaInfoOfAnFfaArenaWithOneSpawnUsesTheSingular() throws IOException {
        new ArenaStore(dir.resolve("arenas"), Json.pretty()).save(TestArenas.ffa("ONE-FFA", 600, 1));
        fixture.services().arenas().load();

        commands.arenaInfo(staff, "ONE-FFA");

        List<String> seen = PaperFixture.chat(staff);
        assertEquals("Spawns: 1 player spawn", seen.get(2));
    }

    @Test
    void arenaImportReadsTheLegacyMapAgain() throws IOException {
        Files.createDirectories(dir.resolve("maps"));
        Files.writeString(dir.resolve("maps").resolve("OLD"), LEGACY_MAP);

        commands.arenaImport(staff, "OLD");
        commands.arenaImport(staff, "NOPE");

        assertEquals(Optional.of("tdm"), fixture.services().arenas().get("OLD").map(arena -> arena.modeId()));
        assertEquals(List.of("Re-imported OLD from maps/OLD.", "There is no arena called NOPE."), PaperFixture.chat(staff));
    }

    @Test
    void aMapThatCannotBeReadIsReportedAndLogged() throws IOException {
        Files.createDirectories(dir.resolve("maps"));
        Files.writeString(dir.resolve("maps").resolve("OLD"), "{ not json");
        Arena before = fixture.services().arenas().get("OLD").orElseThrow();

        commands.arenaImport(staff, "OLD");

        String answer = PaperFixture.chat(staff).getFirst();
        assertTrue(answer.startsWith("OLD can't be played: maps/OLD could not be imported"), answer);
        assertEquals(List.of("Re-importing arena OLD failed"), log.messages(Level.WARNING));
        assertEquals(before, fixture.services().arenas().get("OLD").orElseThrow());
    }

    /**
     * A player whose scoreboard update throws once {@code failing} is set, simulating a leave whose platform step
     * cannot reach the client. Joining calls it too, so arm the flag only afterwards.
     */
    private PlayerMock playerWithFailingScoreboard(AtomicBoolean failing, String name) {
        PlayerMock player = new PlayerMock(server, name) {
            @Override
            public void setScoreboard(Scoreboard scoreboard) {
                if (failing.get()) {
                    throw new IllegalStateException("client gone");
                }
                super.setScoreboard(scoreboard);
            }
        };
        server.addPlayer(player);
        return player;
    }
}
