package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PluginFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiTesting;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.DisplaySlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wired plugin: it enables, its Brigadier commands work, staff commands stay hidden from players, and
 * the menus, buttons and HUD are wired in.
 */
class PluginEnableTest {

    private ServerMock server;
    private PvpPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = PluginFixture.load(server, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } finally {
            CapturingClickCallbackProvider.reset();
        }
    }

    @Test
    void enablesWithItsServices() {
        assertTrue(plugin.isEnabled());
        assertEquals(2, plugin.services().arenas().playable().size());
        assertTrue(server.getMessenger().isOutgoingChannelRegistered(plugin, "BungeeCord"));
    }

    @Test
    void registersTheThreeCommands() {
        for (String command : List.of("pvp", "pvpadmin", "t")) {
            assertNotNull(server.getCommandMap().getCommand(command), command);
        }
    }

    @Test
    void playersRunPvpCommands() {
        PlayerMock alice = server.addPlayer("Alice");

        alice.performCommand("pvp rules");

        // A player gets the rules notice of the menu kit; MockBukkit cannot build dialogs, so it comes as chat.
        assertEquals(List.of("Rules",
                "Team Deathmatch rules", "Two to four teams, and no respawns. The last team with players left wins.",
                "Free For All rules", "Everyone for themselves, madly killing everyone! Highest number of kills wins."),
                PaperFixture.chat(alice));
    }

    @Test
    void theConsoleKeepsTheTextMenus() {
        PaperFixture.chat(server.getConsoleSender());

        server.dispatchCommand(server.getConsoleSender(), "pvp rules");

        assertEquals(List.of("Game modes:", "Team Deathmatch — /pvp rules tdm", "Free For All — /pvp rules ffa"),
                PaperFixture.chat(server.getConsoleSender()));
    }

    @Test
    void playersWithoutStaffRightsCannotUsePvpadmin() {
        PlayerMock alice = server.addPlayer("Alice");

        alice.performCommand("pvpadmin lock");

        assertFalse(plugin.services().lock().locked());
        assertEquals(List.of("Unknown command"), PaperFixture.chat(alice));
    }

    @Test
    void aNonStaffPlayerTypingPvpadminAloneIsAlsoRefused() {
        PlayerMock alice = server.addPlayer("Alice");

        alice.performCommand("pvpadmin");

        assertEquals(List.of("Unknown command"), PaperFixture.chat(alice));
    }

    @Test
    void staffTypingPvpadminAloneGetTheCommandOverview() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);

        staff.performCommand("pvpadmin");

        assertEquals(List.of("/pvpadmin start [arena] [setting=value …] · countdown <arena> · stop <arena> · "
                + "kick <player> · kickall · lock · unlock · arena list · arena info <id> · arena import <id>"),
                PaperFixture.chat(staff));
    }

    @Test
    void aPlayerTypingPvpSpectateAloneGetsItsUsage() {
        PlayerMock alice = server.addPlayer("Alice");

        alice.performCommand("pvp spectate");

        assertEquals(List.of("Usage: /pvp spectate <arena>"), PaperFixture.chat(alice));
    }

    @Test
    void aPlayerTypingTAloneGetsItsUsage() {
        PlayerMock alice = server.addPlayer("Alice");

        alice.performCommand("t");

        assertEquals(List.of("Usage: /t <message>"), PaperFixture.chat(alice));
    }

    @Test
    void aBareAdminSubCommandGetsItsUsage() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);

        staff.performCommand("pvpadmin countdown");

        assertEquals(List.of("Usage: /pvpadmin countdown <arena>"), PaperFixture.chat(staff));
    }

    @Test
    void staffOpenAGameAndPlayersJoinItByCommand() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        PlayerMock alice = server.addPlayer("Alice");

        staff.performCommand("pvpadmin start AS-TDM teamCount=2");
        alice.performCommand("pvp join");

        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        assertTrue(game.session(alice.getUniqueId()).isPresent());
    }

    @Test
    void theConsoleStartsAGameWithTheDefaults() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-FFA");

        assertTrue(plugin.services().games().gameOn("AS-FFA").isPresent());
    }

    @Test
    void tabCompletionListsTheArenasAndSettings() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);

        assertEquals(List.of("AS-FFA", "AS-TDM"),
                server.getCommandMap().tabComplete(staff, "pvpadmin start ").stream().sorted().toList());
        assertEquals(List.of("AS-TDM"), server.getCommandMap().tabComplete(staff, "pvpadmin start AS-T"));
        assertEquals(List.of("timeLimit=10"), server.getCommandMap().tabComplete(staff, "pvpadmin start AS-FFA ti"));
    }

    @Test
    void nonStaffGetNoPvpadminCompletions() {
        PlayerMock alice = server.addPlayer("Alice");

        List<String> completions = server.getCommandMap().tabComplete(alice, "pvpadmin ");

        assertTrue(completions == null || completions.isEmpty());
    }

    @Test
    void aKickOnlyModeratorCannotReachStartLockOrKickAll() {
        // MockBukkit's tabComplete, like Paper's CommandMap.tabComplete, asks Brigadier's getCompletionSuggestions,
        // which lists every subcommand without checking its requires; in game the client only gets the nodes the
        // player may use. So this pins the guard by running the commands.
        PlayerMock moderator = server.addPlayer("Moderator");
        moderator.addAttachment(plugin, Permissions.STAFF_KICK, true);

        moderator.performCommand("pvpadmin kickall");
        assertEquals(List.of("Incorrect argument for command"), PaperFixture.chat(moderator));

        moderator.performCommand("pvpadmin start AS-TDM teamCount=2");
        assertEquals(List.of("Incorrect argument for command"), PaperFixture.chat(moderator));

        moderator.performCommand("pvpadmin lock");
        assertEquals(List.of("Incorrect argument for command"), PaperFixture.chat(moderator));

        moderator.performCommand("pvp");
        assertEquals(List.of("PvP", "No games are open right now.", "[Spectate]", "[Rules]", "[My stats]", "[Lobby]"),
                lines(moderator).stream().map(PaperFixture::plain).toList());

        assertTrue(plugin.services().games().gameOn("AS-TDM").isEmpty());
        assertFalse(plugin.services().lock().locked());
    }

    @Test
    void joinSuggestsTheOpenGames() {
        PlayerMock alice = server.addPlayer("Alice");
        plugin.services().games().open("AS-FFA", Map.of(), "Tester");

        assertEquals(List.of("AS-FFA"), server.getCommandMap().tabComplete(alice, "pvp join "));
    }

    @Test
    void countdownSuggestsWaitingGamesAndStopEveryOpenGame() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-FFA");
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join AS-TDM");
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin countdown AS-TDM");
        PluginFixture.runUntil(server, game, Phase.RUNNING, 10);

        assertEquals(List.of("AS-FFA"), server.getCommandMap().tabComplete(staff, "pvpadmin countdown "));

        server.dispatchCommand(server.getConsoleSender(), "pvpadmin stop AS-TDM");

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(List.of("AS-FFA", "AS-TDM"),
                server.getCommandMap().tabComplete(staff, "pvpadmin stop ").stream().sorted().toList());
    }

    @Test
    void statsSuggestsOnlyThePlayersTheSenderCanSee() {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        alice.hidePlayer(plugin, bob);

        assertEquals(List.of("Alice"), server.getCommandMap().tabComplete(alice, "pvp stats "));
    }

    @Test
    void kickSuggestsAPlayerTheStaffSenderCanSee() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        PlayerMock bob = server.addPlayer("Bob");
        staff.hidePlayer(plugin, bob);

        assertEquals(List.of("Staff"),
                server.getCommandMap().tabComplete(staff, "pvpadmin kick ").stream().sorted().toList());
    }

    @Test
    void consoleStatsSuggestionsIncludeAPlayerVanishedFromAnotherPlayer() {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        alice.hidePlayer(plugin, bob);

        assertEquals(List.of("Alice", "Bob"), server.getCommandMap()
                .tabComplete(server.getConsoleSender(), "pvp stats ").stream().sorted().toList());
    }

    @Test
    void disablingThePluginClosesEveryGame() {
        PlayerMock alice = server.addPlayer("Alice");
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        alice.performCommand("pvp join AS-TDM");
        PvpServices services = plugin.services();

        server.getPluginManager().disablePlugin(plugin);

        assertTrue(services.games().games().isEmpty());
        assertTrue(services.games().gameOf(alice.getUniqueId()).isEmpty());
    }

    @Test
    void theMainMenuJoinsAGameWithAClick() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        PlayerMock alice = server.addPlayer("Alice");
        PaperFixture.chat(alice);

        alice.performCommand("pvp");
        List<Component> menu = lines(alice);

        assertEquals(List.of("PvP", "Pick a game to join, or spectate one.",
                "[Join AS-TDM · Team Deathmatch · 0/16 · waiting]", "[Spectate]", "[Rules]", "[My stats]", "[Lobby]"),
                menu.stream().map(PaperFixture::plain).toList());
        click(line(menu, "[Join AS-TDM · Team Deathmatch · 0/16 · waiting]"), alice);
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        assertTrue(game.session(alice.getUniqueId()).isPresent());
    }

    @Test
    void staffStartAGameFromThePickerAndTheForm() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);

        staff.performCommand("pvpadmin start");
        List<Component> modePicker = lines(staff);
        assertEquals(List.of("Start a game", "Pick a mode.", "[Team Deathmatch (1 free)]", "[Free For All (1 free)]"),
                modePicker.stream().map(PaperFixture::plain).toList());
        click(line(modePicker, "[Free For All (1 free)]"), staff);
        List<Component> picker = lines(staff);
        assertEquals(List.of("Start a game", "Pick a Free For All arena.", "[AS-FFA]", "[Back]"),
                picker.stream().map(PaperFixture::plain).toList());
        click(line(picker, "[AS-FFA]"), staff);
        List<Component> form = lines(staff);
        assertEquals(List.of("Free For All on AS-FFA", "Time limit (minutes): 10", "Score goal (0 = none): 0",
                "Max players: 16", "Allow joining after the start: true", "[Start] [Back]"),
                form.stream().map(PaperFixture::plain).toList());
        click(line(form, "[Start] [Back]"), staff);

        assertTrue(plugin.services().games().gameOn("AS-FFA").isPresent());
        assertTrue(PaperFixture.chat(staff).contains("Opened Free For All on AS-FFA."));
    }

    @Test
    void aNewGameIsAnnouncedWithButtons() {
        PlayerMock alice = server.addPlayer("Alice");
        PaperFixture.chat(alice);

        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-FFA");

        assertEquals(List.of("Console opened Free For All on AS-FFA (16 players). Use /pvp to join!",
                "[Join] [Spectate] [Rules]"), PaperFixture.chat(alice));
    }

    @Test
    void arrivingPlayersSeeTheOpenGames() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");

        PlayerMock carol = server.addPlayer("Carol");

        assertEquals(List.of("PvP games open:", "AS-TDM · Team Deathmatch · 0/16 · waiting [Join] [Spectate]"),
                PaperFixture.chat(carol));
    }

    @Test
    void theHudShowsTheCountdown() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join AS-TDM");

        server.dispatchCommand(server.getConsoleSender(), "pvpadmin countdown AS-TDM");

        List<BossBar> bars = new ArrayList<>();
        alice.activeBossBars().forEach(bars::add);
        assertEquals(List.of("Starting in 7s"), bars.stream().map(bar -> PaperFixture.plain(bar.name())).toList());
        assertEquals("Team Deathmatch Starting in 7s",
                PaperFixture.plain(alice.getScoreboard().getObjective(DisplaySlot.SIDEBAR).displayName()));
    }

    @Test
    void disablingDuringACountdownLeavesNoBossBarAndNoListenerFailure() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join AS-TDM");
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin countdown AS-TDM");

        List<BossBar> aliceBarsDuring = new ArrayList<>();
        alice.activeBossBars().forEach(aliceBarsDuring::add);
        List<BossBar> bobBarsDuring = new ArrayList<>();
        bob.activeBossBars().forEach(bobBarsDuring::add);
        assertEquals(List.of("Starting in 7s"),
                aliceBarsDuring.stream().map(bar -> PaperFixture.plain(bar.name())).toList());
        assertEquals(List.of("Starting in 7s"),
                bobBarsDuring.stream().map(bar -> PaperFixture.plain(bar.name())).toList());

        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        plugin.getLogger().addHandler(handler);
        try {
            server.getPluginManager().disablePlugin(plugin);
        } finally {
            plugin.getLogger().removeHandler(handler);
        }

        List<BossBar> aliceBars = new ArrayList<>();
        alice.activeBossBars().forEach(aliceBars::add);
        List<BossBar> bobBars = new ArrayList<>();
        bob.activeBossBars().forEach(bobBars::add);
        assertTrue(aliceBars.isEmpty());
        assertTrue(bobBars.isEmpty());
        assertTrue(messages.stream().noneMatch(message -> message.startsWith("Game listener")));
    }

    @Test
    void disablingWithARunningGameShowsNoSummaryButtons() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join AS-TDM");
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin countdown AS-TDM");
        PluginFixture.runUntil(server, game, Phase.RUNNING, 10);
        PaperFixture.chat(alice);
        PaperFixture.chat(bob);

        server.getPluginManager().disablePlugin(plugin);

        List<String> aliceChat = PaperFixture.chat(alice);
        List<String> bobChat = PaperFixture.chat(bob);
        assertFalse(aliceChat.contains("[Games] [My stats]"));
        assertFalse(bobChat.contains("[Games] [My stats]"));
        assertTrue(aliceChat.contains("The game on AS-TDM ended because the server is stopping."));
        assertTrue(bobChat.contains("The game on AS-TDM ended because the server is stopping."));
    }

    @Test
    void disablingWithAWaitingGameTellsItsPlayersTheServerIsStopping() {
        server.dispatchCommand(server.getConsoleSender(), "pvpadmin start AS-TDM");
        PlayerMock alice = server.addPlayer("Alice");
        alice.performCommand("pvp join AS-TDM");
        PaperFixture.chat(alice);

        server.getPluginManager().disablePlugin(plugin);

        assertTrue(PaperFixture.chat(alice).contains("The game on AS-TDM ended because the server is stopping."));
    }

    @Test
    void kickAllFromTheMainMenuAsksFirst() {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        PlayerMock alice = server.addPlayer("Alice");
        PaperFixture.chat(alice);

        staff.performCommand("pvp");
        click(line(lines(staff), "[Kick all]"), staff);
        List<Component> question = lines(staff);
        assertEquals(List.of("Send everyone to the main server?", "Only players with pvp.join.locked stay.", "[Yes]"),
                question.stream().map(PaperFixture::plain).toList());
        click(line(question, "[Yes]"), staff);

        assertEquals(List.of("Sent 1 player to the main server."), PaperFixture.chat(staff));
        assertEquals(List.of("A PvP manager kicked all players"), PaperFixture.chat(alice));
    }

    /** Drains a player's chat as components, so that its buttons can be clicked. */
    private static List<Component> lines(PlayerMock player) {
        List<Component> lines = new ArrayList<>();
        for (Component line = player.nextComponentMessage(); line != null; line = player.nextComponentMessage()) {
            lines.add(line);
        }
        return lines;
    }

    private static Component line(List<Component> lines, String text) {
        return lines.stream().filter(line -> PaperFixture.plain(line).equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("no line \"" + text + "\" in "
                        + lines.stream().map(PaperFixture::plain).toList()));
    }

    /** Clicks the first button of a chat line, through the test click-callback provider. */
    private static void click(Component line, PlayerMock player) {
        assertTrue(CapturingClickCallbackProvider.click(UiTesting.firstClick(line).orElseThrow(), player));
    }
}
