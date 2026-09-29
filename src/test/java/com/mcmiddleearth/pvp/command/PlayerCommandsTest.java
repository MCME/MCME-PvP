package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /pvp and its sub-commands, called directly. */
class PlayerCommandsTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private PlayerCommands commands;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        commands = new PlayerCommands(fixture.services(), new TextMenus(fixture.services()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pvpShowsPlayersTheMainMenuAndTheConsoleTheCommands() {
        PlayerMock alice = fixture.player("Alice");
        ConsoleCommandSenderMock console = server.getConsoleSender();

        commands.root(alice);
        commands.root(console);

        assertEquals("PvP games", PaperFixture.chat(alice).getFirst());
        assertTrue(PaperFixture.chat(console).getFirst().startsWith("/pvp join [arena]"));
    }

    @Test
    void joinWithoutArenaTakesTheOnlyOpenGame() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-TDM");

        commands.join(alice, Optional.empty());

        assertTrue(game.session(alice.getUniqueId()).isPresent());
    }

    @Test
    void joinWithoutArenaNeedsAnOpenGame() {
        PlayerMock alice = fixture.player("Alice");

        commands.join(alice, Optional.empty());

        assertEquals(List.of("There is no open game to join."), PaperFixture.chat(alice));
    }

    @Test
    void joinWithoutArenaAsksToChooseBetweenSeveralGames() {
        PlayerMock alice = fixture.player("Alice");
        fixture.open("AS-TDM");
        fixture.open("AS-FFA");

        commands.join(alice, Optional.empty());

        assertEquals(List.of("Several games are open. Use /pvp join <arena>, or pick one in /pvp."), PaperFixture.chat(alice));
    }

    @Test
    void joinNamesTheArenaInAnyCase() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-FFA");
        fixture.open("AS-TDM");

        commands.join(alice, Optional.of("as-ffa"));

        assertTrue(game.session(alice.getUniqueId()).isPresent());
    }

    @Test
    void joinWithoutAnArenaMovesAPlayerOnFromTheirEndedGame() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game tdm = fixture.open("AS-TDM");
        fixture.join(tdm, alice, bob);
        fixture.startAndRun(tdm);
        fixture.services().games().stop(tdm);
        Game ffa = fixture.open("AS-FFA");

        commands.join(alice, Optional.empty());

        assertEquals(ffa, fixture.services().games().gameOf(alice.getUniqueId()).orElseThrow());
    }

    @Test
    void joinRefusesAnArenaWithoutGameAndASecondGame() {
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);
        PaperFixture.chat(alice);

        commands.join(alice, Optional.of("AS-FFA"));
        fixture.open("AS-FFA");
        commands.join(alice, Optional.of("AS-FFA"));

        assertEquals(List.of("There is no game on AS-FFA.", "Alice is already in a game. Use /pvp leave first."),
                PaperFixture.chat(alice));
    }

    @Test
    void aLateJoinerWhoCannotPlaySpectatesAndIsToldWhy() {
        PlayerMock carol = fixture.player("Carol");
        PlayerMock dave = fixture.player("Dave");
        Game full = fixture.services().games().open("AS-TDM", Map.of("maxPlayers", 2), "Tester");
        fixture.join(full, fixture.player("Alice"), fixture.player("Bob"));
        fixture.startAndRun(full);
        Game closed = fixture.services().games().open("AS-FFA", Map.of("midgameJoin", false), "Tester");
        fixture.join(closed, fixture.player("Erin"), fixture.player("Frank"));
        fixture.startAndRun(closed);
        PaperFixture.chat(carol);
        PaperFixture.chat(dave);

        commands.join(carol, Optional.of("AS-TDM"));
        commands.join(dave, Optional.of("AS-FFA"));

        assertEquals(Role.SPECTATOR, full.session(carol.getUniqueId()).orElseThrow().role());
        assertEquals(List.of("The game is full."), PaperFixture.chat(carol));
        assertEquals(List.of("The game has already started."), PaperFixture.chat(dave));
    }

    @Test
    void anEliminatedPlayerWhoJoinsAgainIsToldSheIsOut() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob, fixture.player("Carol"), fixture.player("Dave"));
        fixture.startAndRun(game);
        game.handleDeath(alice.getUniqueId(), bob.getUniqueId());
        commands.leave(alice);
        PaperFixture.chat(alice);

        commands.join(alice, Optional.of("AS-TDM"));

        assertEquals(Role.SPECTATOR, game.session(alice.getUniqueId()).orElseThrow().role());
        assertEquals(List.of("You're out of this game, so you watch it."), PaperFixture.chat(alice));
    }

    @Test
    void theConsoleCannotJoin() {
        fixture.open("AS-TDM");
        ConsoleCommandSenderMock console = server.getConsoleSender();

        commands.join(console, Optional.empty());

        assertEquals(List.of("Only players can do that."), PaperFixture.chat(console));
    }

    @Test
    void whileLockedOnlyPlayersWithThePermissionJoin() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock staff = fixture.player("Staff");
        staff.setOp(true);
        Game game = fixture.open("AS-TDM");
        fixture.services().lock().lock();

        commands.join(alice, Optional.empty());
        commands.join(staff, Optional.empty());

        assertEquals(List.of("PvP is locked right now."), PaperFixture.chat(alice));
        assertTrue(game.session(staff.getUniqueId()).isPresent());
    }

    @Test
    void whileLockedOnlyPlayersWithThePermissionSpectate() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock staff = fixture.player("Staff");
        staff.setOp(true);
        Game game = fixture.open("AS-TDM");
        fixture.services().lock().lock();

        commands.spectate(alice, "AS-TDM");
        commands.spectate(staff, "AS-TDM");

        assertEquals(List.of("PvP is locked right now."), PaperFixture.chat(alice));
        assertEquals(Role.SPECTATOR, game.session(staff.getUniqueId()).orElseThrow().role());
    }

    @Test
    void leaveReturnsToTheLobby() {
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        commands.leave(alice);

        assertTrue(fixture.services().games().gameOf(alice.getUniqueId()).isEmpty());
        assertEquals(344.47, alice.getLocation().getX(), 1e-9);
    }

    @Test
    void leaveOutsideAGameIsRefused() {
        PlayerMock alice = fixture.player("Alice");

        commands.leave(alice);

        assertEquals(List.of("You're not in a game."), PaperFixture.chat(alice));
    }

    @Test
    void spectateWatchesTheGameOnTheArena() {
        PlayerMock carol = fixture.player("Carol");
        Game game = fixture.open("AS-TDM");

        commands.spectate(carol, "AS-TDM");

        assertEquals(Role.SPECTATOR, game.session(carol.getUniqueId()).orElseThrow().role());
    }

    @Test
    void gamesShowsTheList() {
        PlayerMock alice = fixture.player("Alice");

        commands.games(alice, 1);

        assertEquals(List.of("PvP games", "No games right now."), PaperFixture.chat(alice));
    }

    @Test
    void rulesOfAnUnknownModeAreRefused() {
        PlayerMock alice = fixture.player("Alice");

        commands.rules(alice, Optional.of("ctf"));
        commands.rules(alice, Optional.of("FFA"));

        assertEquals(List.of("There is no game mode called ctf.", "Free For All rules",
                "Everyone for themselves, madly killing everyone! Highest number of kills wins."), PaperFixture.chat(alice));
    }

    @Test
    void statsOfThePlayerThemselvesOrNoneYet() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.services().stats().recordSpectated(alice.getUniqueId(), "Alice");

        commands.stats(alice, Optional.empty());
        commands.stats(bob, Optional.empty());

        assertEquals("Stats for Alice", PaperFixture.chat(alice).getFirst());
        assertEquals(List.of("No PvP stats for Bob."), PaperFixture.chat(bob));
    }

    @Test
    void statsOfAPlayerWhoLeftTheServerComeFromItsCache() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.services().stats().recordSpectated(bob.getUniqueId(), "Bob");
        bob.disconnect();

        commands.stats(alice, Optional.of("Bob"));
        commands.stats(alice, Optional.of("Nobody"));

        List<String> seen = PaperFixture.chat(alice);
        assertEquals("Stats for Bob", seen.getFirst());
        assertEquals("No PvP stats for Nobody.", seen.getLast());
    }

    @Test
    void statsFoundOnlyIn112FilesShowThePlayersName() throws IOException {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Files.createDirectories(dir.resolve("stats"));
        Files.writeString(dir.resolve("stats").resolve(bob.getUniqueId().toString()),
                "{\"kills\":3,\"deaths\":2,\"gamesPlayed\":4,\"gamesWon\":1,\"gamesLost\":2,\"gamesSpectated\":1}");

        commands.stats(alice, Optional.of("Bob"));

        assertEquals(List.of("Stats for Bob", "Kills: 3 · Deaths: 2 · K/D: 1.50",
                "Games played: 4 · Won: 1 · Lost: 2 · Spectated: 1"), PaperFixture.chat(alice));
    }

    @Test
    void lobbyTeleportsOrLeavesTheGame() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.join(fixture.open("AS-TDM"), bob);

        commands.lobby(alice);
        commands.lobby(bob);

        assertEquals(List.of("You're back in the lobby."), PaperFixture.chat(alice));
        assertEquals(344.47, alice.getLocation().getX(), 1e-9);
        assertTrue(fixture.services().games().gameOf(bob.getUniqueId()).isEmpty());
        assertEquals(List.of(), PaperFixture.chat(bob));
    }

    @Test
    void lobbyOutsideAGameKeepsAPlayersThings() {
        PlayerMock alice = fixture.player("Alice");
        alice.setGameMode(GameMode.CREATIVE);
        alice.getInventory().setItem(0, new ItemStack(Material.DIAMOND));

        commands.lobby(alice);

        assertEquals(List.of("You're back in the lobby."), PaperFixture.chat(alice));
        assertEquals(GameMode.CREATIVE, alice.getGameMode());
        assertEquals(new ItemStack(Material.DIAMOND), alice.getInventory().getItem(0));
    }
}
