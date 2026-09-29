package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.command.Menus;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.ui.FormScreen;
import com.mcmiddleearth.pvp.ui.UiScreens;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiMenusTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private final RecordingMenus fallback = new RecordingMenus();
    private ServerMock server;
    private UiFixture ui;
    private UiMenus menus;
    private PlayerMock alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2),
                TestArenas.tdm("OLD", 100, 2).unassigned("the map has no game mode"));
        menus = new UiMenus(new UiScreens(ui.services, navigator), navigator, fallback);
        alice = server.addPlayer("Alice");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Arena arena(String id) {
        return ui.services.arenas().get(id).orElseThrow();
    }

    /** Stats as {@code /pvp stats} hands them over: under the player's current name. */
    private PlayerStats stats() {
        return new PlayerStats(1, alice.getUniqueId(), "Alice", 3, 1, 2, 1, 1, 0, List.of());
    }

    @Test
    void playersGetTheMainMenuAndTheGamesListFromTheMenuKit() {
        menus.mainMenu(alice);
        assertEquals("PvP", plain(navigator.lastScreen().title()));
        assertEquals(1, navigator.shownTo(alice.getUniqueId()).size());
        menus.games(alice, 1);
        assertEquals("PvP games", plain(navigator.lastMenu().header()));
        assertTrue(fallback.calls.isEmpty());
    }

    @Test
    void theConsoleGetsTheTextVersion() {
        CommandSender console = server.getConsoleSender();
        menus.games(console, 2);
        menus.rules(console, Optional.empty());
        menus.stats(console, stats());
        assertEquals(List.of("games 2", "rules all", "stats Alice"), fallback.calls);
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void startGameShowsThePickerOrTheArenasForm() {
        menus.startGame(alice, Optional.empty());
        assertEquals("Start a game", plain(navigator.lastScreen().title()));
        menus.startGame(alice, Optional.of(arena("AS-TDM")));
        FormScreen form = assertInstanceOf(FormScreen.class, navigator.lastScreen());
        assertEquals("Team Deathmatch on AS-TDM", plain(form.title()));
    }

    @Test
    void anArenaThatCannotBePlayedIsRefusedInChat() {
        menus.startGame(alice, Optional.of(arena("OLD")));
        assertEquals(List.of("OLD can't be played: the map has no game mode"), navigator.messages());
    }

    @Test
    void rulesAndStatsAreNoticesForPlayers() {
        menus.rules(alice, ui.services.modes().get("tdm"));
        assertEquals("Team Deathmatch rules", plain(navigator.lastScreen().title()));
        menus.stats(alice, stats());
        assertEquals("Stats for Alice", plain(navigator.lastScreen().body().get(0)));
    }

    /** The text menus, reduced to a record of what they were asked. */
    private static final class RecordingMenus implements Menus {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void mainMenu(Player player) {
            calls.add("main " + player.getName());
        }

        @Override
        public void games(CommandSender sender, int page) {
            calls.add("games " + page);
        }

        @Override
        public void startGame(Player player, Optional<Arena> arena) {
            calls.add("start " + arena.map(Arena::id).orElse("picker"));
        }

        @Override
        public void rules(CommandSender sender, Optional<GameMode> mode) {
            calls.add("rules " + mode.map(GameMode::id).orElse("all"));
        }

        @Override
        public void stats(CommandSender sender, PlayerStats stats) {
            calls.add("stats " + stats.name());
        }
    }
}
