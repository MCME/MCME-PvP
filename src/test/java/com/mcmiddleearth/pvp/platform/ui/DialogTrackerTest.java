package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.ui.Viewer;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogTrackerTest {

    @TempDir
    Path dir;

    private final List<UUID> closed = new ArrayList<>();
    private final DialogTracker tracker = new DialogTracker(closed::add);
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
    void itRemembersWhoHasAPvpDialogOpen() {
        UUID player = UUID.randomUUID();
        assertFalse(tracker.isOpen(player));
        tracker.opened(player);
        assertTrue(tracker.isOpen(player));
        tracker.closed(player);
        assertFalse(tracker.isOpen(player));
    }

    @Test
    void theCountdownClosesTheDialogsOfThatGamesPlayersOnly() {
        UiFixture ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.tdm("BR-TDM", 100, 2));
        ui.listeners.add(tracker);
        Viewer alice = ui.player("Alice");
        Viewer bob = ui.player("Bob");
        Viewer carol = ui.player("Carol");
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, bob);
        ui.join(ui.open("BR-TDM"), carol);
        tracker.opened(alice.id());
        tracker.opened(carol.id());

        ui.games().startCountdown(game);

        assertEquals(Phase.COUNTDOWN, game.phase());
        assertEquals(List.of(alice.id()), closed);
        assertFalse(tracker.isOpen(alice.id()));
        assertTrue(tracker.isOpen(carol.id()));
    }

    @Test
    void otherPhaseChangesCloseNothing() {
        UiFixture ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2));
        ui.listeners.add(tracker);
        Viewer alice = ui.player("Alice");
        Game game = ui.open("AS-TDM");
        ui.join(game, alice, ui.player("Bob"));
        ui.games().startCountdown(game);
        tracker.opened(alice.id());   // opened again during the countdown, by the player's own command
        ui.advanceSeconds(7);
        assertEquals(Phase.RUNNING, game.phase());
        assertTrue(tracker.isOpen(alice.id()));
    }

    @Test
    void quittingForgetsTheDialog() {
        Plugin plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(tracker, plugin);
        PlayerMock alice = server.addPlayer("Alice");
        tracker.opened(alice.getUniqueId());
        alice.disconnect();
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void changingWorldForgetsTheDialog() {
        Plugin plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(tracker, plugin);
        PlayerMock alice = server.addPlayer("Alice");
        tracker.opened(alice.getUniqueId());
        server.getPluginManager().callEvent(new PlayerChangedWorldEvent(alice, alice.getWorld()));
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }
}
