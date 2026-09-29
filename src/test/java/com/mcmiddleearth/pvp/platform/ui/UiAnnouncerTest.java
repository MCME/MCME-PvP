package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.UiFixture;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.UiScreens;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;

import static com.mcmiddleearth.pvp.testing.UiTesting.linkLabels;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAnnouncerTest {

    @TempDir
    Path dir;

    private final RecordingNavigator navigator = new RecordingNavigator();
    private ServerMock server;
    private UiFixture ui;
    private boolean enabled = true;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        ui = new UiFixture(dir, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        UiAnnouncer announcer = new UiAnnouncer(ui.services, new UiScreens(ui.services, navigator), navigator,
                () -> enabled);
        ui.listeners.add(announcer);
        server.getPluginManager().registerEvents(announcer, MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void join(Game game, PlayerMock player) {
        ui.games().join(JoinUnit.solo(new PlayerRef(player.getUniqueId(), player.getName())), game);
    }

    @Test
    void anOpenedGameGetsButtonsForThePlayersOutsideGames() {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        join(ui.open("AS-TDM"), alice);
        navigator.clear();

        ui.open("AS-FFA");

        assertTrue(navigator.shownTo(alice.getUniqueId()).isEmpty());
        List<Object> toBob = navigator.shownTo(bob.getUniqueId());
        assertEquals(1, toBob.size());
        ChatMenu row = assertInstanceOf(ChatMenu.class, toBob.get(0));
        assertNull(row.header());
        assertEquals(List.of("Join", "Spectate", "Rules"), linkLabels(row.lines().get(0)));
    }

    @Test
    void aLockedServerShowsNoOpenedRowToPlayersItKeepsOut() {
        PlayerMock carol = server.addPlayer("Carol");
        PlayerMock dave = server.addPlayer("Dave");
        dave.setOp(true);
        ui.services.lock().lock();
        navigator.clear();

        ui.open("AS-TDM");

        assertTrue(navigator.shownTo(carol.getUniqueId()).isEmpty());
        assertEquals(1, navigator.shownTo(dave.getUniqueId()).size());
    }

    @Test
    void theSummaryIsFollowedByGamesAndMyStatsForEveryoneInTheGame() {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        PlayerMock carol = server.addPlayer("Carol");
        PlayerMock dave = server.addPlayer("Dave");
        Game game = ui.open("AS-TDM");
        join(game, alice);
        join(game, bob);
        ui.games().spectate(new PlayerRef(carol.getUniqueId(), "Carol"), game);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        navigator.clear();

        game.handleDeath(bob.getUniqueId(), alice.getUniqueId());

        for (PlayerMock player : List.of(alice, bob, carol)) {
            List<Object> shown = navigator.shownTo(player.getUniqueId());
            assertEquals(1, shown.size());
            ChatMenu row = assertInstanceOf(ChatMenu.class, shown.get(0));
            assertNull(row.header());
            assertEquals(List.of("Games", "My stats"), linkLabels(row.lines().get(0)));
        }
        assertTrue(navigator.shownTo(dave.getUniqueId()).isEmpty());
    }

    @Test
    void aGameEndedByTheShutdownGetsNoButtons() {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        Game game = ui.open("AS-TDM");
        join(game, alice);
        join(game, bob);
        ui.games().startCountdown(game);
        ui.advanceSeconds(7);
        assertEquals(Phase.RUNNING, game.phase());
        enabled = false;
        navigator.clear();

        ui.games().closeAll();

        assertEquals(Phase.CLOSED, game.phase());
        assertTrue(navigator.shownTo(alice.getUniqueId()).isEmpty());
        assertTrue(navigator.shownTo(bob.getUniqueId()).isEmpty());
    }

    @Test
    void anArrivingPlayerSeesTheOpenGames() {
        ui.open("AS-TDM");
        PlayerMock carol = server.addPlayer("Carol");
        ChatMenu notice = assertInstanceOf(ChatMenu.class, navigator.shownTo(carol.getUniqueId()).get(0));
        assertEquals("PvP games open:", plain(notice.header()));
        assertEquals(List.of("Join", "Spectate"), linkLabels(notice.lines().get(0)));
    }

    @Test
    void withoutOpenGamesAnArrivingPlayerSeesNothing() {
        server.addPlayer("Carol");
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void aLockedServerShowsNoNoticeToPlayersItSendsAway() {
        ui.open("AS-TDM");
        navigator.clear();
        ui.services.lock().lock();
        PlayerMock carol = server.addPlayer("Carol");
        assertTrue(navigator.shownTo(carol.getUniqueId()).isEmpty());
        carol.setOp(true);   // op holds pvp.join.locked, so the lock lets Carol stay
        server.getPluginManager().callEvent(new PlayerJoinEvent(carol, Component.empty()));
        assertEquals(1, navigator.shownTo(carol.getUniqueId()).size());
    }
}
