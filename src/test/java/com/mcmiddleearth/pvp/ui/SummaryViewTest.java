package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.link;
import static com.mcmiddleearth.pvp.testing.UiTesting.linkLabels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryViewTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private SummaryView view;
    private Viewer alice;

    @BeforeEach
    void setUp() {
        UiFixture ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2));
        InfoViews info = new InfoViews(ui.services);
        GamesListView games = new GamesListView(ui.services, navigator,
                new ConfirmView(ui.services.messages(), navigator), info);
        view = new SummaryView(ui.services.messages(), navigator, games, info);
        alice = ui.player("Alice");
    }

    @Test
    void theRowUnderTheSummaryOffersGamesAndMyStats() {
        ChatMenu row = view.buttons();
        assertNull(row.header());
        ChatLine line = row.lines().get(0);
        assertEquals(List.of("Games", "My stats"), linkLabels(line));
        assertEquals("Show the open games", plain(link(line, "Games").tooltip()));
        assertEquals("Show your stats", plain(link(line, "My stats").tooltip()));
    }

    @Test
    void gamesShowsTheListAndMyStatsTheViewersStats() {
        ChatLine line = view.buttons().lines().get(0);
        assertTrue(click(link(line, "Games"), alice).isEmpty());
        assertEquals("PvP games", plain(navigator.lastMenu().header()));
        assertTrue(click(link(line, "My stats"), alice).isEmpty());
        assertEquals("Stats", plain(navigator.lastScreen().title()));
    }
}
