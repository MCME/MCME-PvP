package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.UiTesting.button;
import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.linkLabels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiScreensTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private UiFixture ui;
    private UiScreens screens;

    @BeforeEach
    void setUp() {
        ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2),
                TestArenas.tdm("OLD", 100, 2).unassigned("the map has no game mode"));
        screens = new UiScreens(ui.services, navigator);
    }

    @Test
    void theMainMenuLeadsToTheOtherScreens() {
        Viewer staff = ui.staff("Staff");
        Screen menu = screens.mainMenu(staff);
        assertEquals("PvP", plain(menu.title()));
        assertTrue(click(button(menu, "Start a game"), staff).isEmpty());
        assertEquals("Start a game", plain(navigator.lastScreen().title()));
        assertTrue(click(button(menu, "Rules"), staff).isEmpty());
        assertEquals("Rules", plain(navigator.lastScreen().title()));
    }

    @Test
    void startGameIsThePickerOrTheForm() {
        assertEquals("Start a game", plain(screens.startPicker().title()));
        FormScreen form = assertInstanceOf(FormScreen.class,
                screens.startForm(ui.services.arenas().get("AS-TDM").orElseThrow()));
        assertEquals("Team Deathmatch on AS-TDM", plain(form.title()));
        PvpException refusal = assertThrows(PvpException.class,
                () -> screens.startForm(ui.services.arenas().get("OLD").orElseThrow()));
        assertEquals(Msg.ERROR_ARENA_UNASSIGNED, refusal.messageKey());
    }

    @Test
    void rulesAreOneModeOrAll() {
        assertEquals("Rules", plain(screens.rules(Optional.empty()).title()));
        assertEquals("Free For All rules", plain(screens.rules(ui.services.modes().get("ffa")).title()));
    }

    @Test
    void statsAreANotice() {
        Viewer alice = ui.player("Alice");
        PlayerStats imported = new PlayerStats(1, alice.id(), alice.id().toString(), 1, 1, 1, 1, 0, 0, List.of());
        Screen stats = screens.stats(imported, "Alice");
        assertEquals("Stats for Alice", plain(stats.body().get(0)));
    }

    @Test
    void theChatMenusComeFromTheListViews() {
        assertTrue(screens.arrival().isEmpty());
        Game game = ui.open("AS-TDM");
        assertEquals("PvP games", plain(screens.games(ui.player("Alice"), 1).header()));
        assertEquals(List.of("Join", "Spectate", "Rules"), linkLabels(screens.opened(game).lines().get(0)));
        assertEquals(List.of("Games", "My stats"), linkLabels(screens.summary().lines().get(0)));
        assertEquals("PvP games open:", plain(screens.arrival().orElseThrow().header()));
        assertSame(ui.services.messages(), screens.messages());
    }
}
