package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The plain-chat screens behind /pvp and /pvpadmin start. */
class TextMenusTest {

    @TempDir
    Path dir;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void gamesListsEveryOpenGameWithPlayersAndStatus() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        Game tdm = fixture.open("AS-TDM");
        Game ffa = fixture.open("AS-FFA");
        fixture.join(tdm, alice);

        new TextMenus(fixture.services()).games(dave, 1);

        assertEquals(List.of(
                "PvP games",
                "AS-TDM · Team Deathmatch · 1/" + tdm.maxPlayers() + " · waiting",
                "AS-FFA · Free For All · 0/" + ffa.maxPlayers() + " · waiting"), PaperFixture.chat(dave));
    }

    @Test
    void withoutGamesTheListSaysSo() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2));
        PlayerMock dave = fixture.player("Dave");

        new TextMenus(fixture.services()).games(dave, 1);

        assertEquals(List.of("PvP games", "No games right now."), PaperFixture.chat(dave));
    }

    @Test
    void longListsArePaged() {
        Arena[] arenas = IntStream.range(0, 9).mapToObj(i -> TestArenas.tdm("A" + i, i * 100, 2)).toArray(Arena[]::new);
        PaperFixture fixture = fixture(arenas);
        PlayerMock dave = fixture.player("Dave");
        for (Arena arena : arenas) {
            fixture.open(arena.id());
        }

        new TextMenus(fixture.services()).games(dave, 2);

        List<String> page = PaperFixture.chat(dave);
        assertEquals(3, page.size());
        assertTrue(page.get(1).startsWith("A8 · "), page.get(1));
        assertEquals("Page 2/2", page.get(2));
    }

    @Test
    void aPageNumberPastTheEndShowsTheLastPage() {
        Arena[] arenas = IntStream.range(0, 9).mapToObj(i -> TestArenas.tdm("A" + i, i * 100, 2)).toArray(Arena[]::new);
        PaperFixture fixture = fixture(arenas);
        PlayerMock dave = fixture.player("Dave");
        for (Arena arena : arenas) {
            fixture.open(arena.id());
        }

        new TextMenus(fixture.services()).games(dave, 99);

        List<String> page = PaperFixture.chat(dave);
        assertEquals(3, page.size());
        assertTrue(page.get(1).startsWith("A8 · "), page.get(1));
        assertEquals("Page 2/2", page.getLast());
    }

    @Test
    void aPageNumberBelowOneShowsTheFirstPage() {
        Arena[] arenas = IntStream.range(0, 9).mapToObj(i -> TestArenas.tdm("A" + i, i * 100, 2)).toArray(Arena[]::new);
        PaperFixture fixture = fixture(arenas);
        PlayerMock dave = fixture.player("Dave");
        for (Arena arena : arenas) {
            fixture.open(arena.id());
        }

        new TextMenus(fixture.services()).games(dave, 0);

        List<String> page = PaperFixture.chat(dave);
        assertEquals(11, page.size());
        assertTrue(page.get(1).startsWith("A0 · "), page.get(1));
        assertEquals("Page 1/2", page.get(9));
        assertEquals("Next page: /pvp games 2", page.getLast());
    }

    @Test
    void exactlyOnePageOfGamesHasNoPageLine() {
        Arena[] arenas = IntStream.range(0, 8).mapToObj(i -> TestArenas.tdm("A" + i, i * 100, 2)).toArray(Arena[]::new);
        PaperFixture fixture = fixture(arenas);
        PlayerMock dave = fixture.player("Dave");
        for (Arena arena : arenas) {
            fixture.open(arena.id());
        }

        new TextMenus(fixture.services()).games(dave, 1);

        List<String> page = PaperFixture.chat(dave);
        assertEquals(9, page.size());
        assertTrue(page.getLast().startsWith("A7 · "), page.getLast());
    }

    @Test
    void theMainMenuShowsTheGamesAndTheCommands() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2));
        PlayerMock dave = fixture.player("Dave");

        new TextMenus(fixture.services()).mainMenu(dave);

        assertEquals(List.of("PvP games", "No games right now.",
                "/pvp join [arena] · leave · spectate <arena> · games [page] · rules [mode] · stats [player] · lobby"),
                PaperFixture.chat(dave));
    }

    @Test
    void theArenaPickerListsPlayableArenasAndWhoHoldsABusyOne() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2), TestArenas.tdm("AS-TDM2", 5, 2),
                TestArenas.ffa("AS-FFA", 100, 4), TestArenas.tdm("OLD", 300, 2).unassigned("the map has no game mode"));
        PlayerMock staff = fixture.player("Staff");
        fixture.open("AS-TDM");

        new TextMenus(fixture.services()).startGame(staff, Optional.empty());

        assertEquals(List.of(
                "Arenas (3)",
                "AS-FFA · Free For All",
                "AS-TDM is busy (game on AS-TDM).",
                "AS-TDM2 is busy (game on AS-TDM)."), PaperFixture.chat(staff));
    }

    @Test
    void theArenaPickerSkipsAnUnknownModeAndNamesAWrongCaseOneCorrectly() {
        Arena unknownMode = TestArenas.tdm("AS-CTF", 200, 2).withMode("ctf");
        Arena wrongCase = TestArenas.tdm("AS-TDM", 0, 2).withMode("TDM");
        PaperFixture fixture = fixture(unknownMode, wrongCase);
        PlayerMock staff = fixture.player("Staff");

        new TextMenus(fixture.services()).startGame(staff, Optional.empty());

        assertEquals(List.of(
                "Arenas (1)",
                "AS-TDM · Team Deathmatch"), PaperFixture.chat(staff));
    }

    @Test
    void anArenasStartScreenListsItsSettings() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        PaperFixture fixture = fixture(arena);
        PlayerMock staff = fixture.player("Staff");

        new TextMenus(fixture.services()).startGame(staff, Optional.of(arena));

        String usage = PaperFixture.chat(staff).getFirst();
        assertTrue(usage.startsWith("Usage: /pvpadmin start AS-TDM [setting=value ...]. Settings for Team Deathmatch: "), usage);
        assertTrue(usage.contains("teamCount=2 (2-2)"), usage);
        assertTrue(usage.contains("midgameJoin=true (true/false)"), usage);
    }

    @Test
    void rulesOfOneModeOrTheListOfModes() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2));
        PlayerMock dave = fixture.player("Dave");
        TextMenus menus = new TextMenus(fixture.services());

        menus.rules(dave, fixture.services().modes().get("tdm"));
        menus.rules(dave, Optional.empty());

        assertEquals(List.of(
                "Team Deathmatch rules",
                "Two to four teams, and no respawns. The last team with players left wins.",
                "Game modes:",
                "Team Deathmatch — /pvp rules tdm",
                "Free For All — /pvp rules ffa"), PaperFixture.chat(dave));
    }

    @Test
    void statsShowKillsDeathsKillDeathAndGames() {
        PaperFixture fixture = fixture(TestArenas.tdm("AS-TDM", 0, 2));
        PlayerMock dave = fixture.player("Dave");
        PlayerStats stats = new PlayerStats(PlayerStats.SCHEMA_VERSION, UUID.randomUUID(), "Alice", 7, 2, 5, 3, 1, 4,
                List.of());

        new TextMenus(fixture.services()).stats(dave, stats);

        assertEquals(List.of(
                "Stats for Alice",
                "Kills: 7 · Deaths: 2 · K/D: 3.50",
                "Games played: 5 · Won: 3 · Lost: 1 · Spectated: 4"), PaperFixture.chat(dave));
    }

    private PaperFixture fixture(Arena... arenas) {
        return PaperFixture.create(server, dir, arenas);
    }
}
