package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.game.PlayerActions;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Marking a player's saved game state, for {@link CrashRecovery} to notice after a restart. */
class PaperPlayerActionsTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Arena arena;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        arena = TestArenas.tdm("AS-TDM", 0, 2);
        fixture = PaperFixture.create(server, dir, arena);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void resetForPlayAndSpectatingMarkThePlayerAndTheLobbyClearsIt() {
        PlayerActions players = fixture.contexts().create(GameId.random(), arena).players();
        GameStateMarker marker = fixture.contexts().marker();
        PlayerMock alice = fixture.player("Alice");

        players.resetForPlay(alice.getUniqueId());
        assertTrue(marker.marked(alice));

        marker.clear(alice);
        players.setSpectator(alice.getUniqueId(), true);
        assertTrue(marker.marked(alice));

        players.sendToLobby(alice.getUniqueId());
        assertFalse(marker.marked(alice));
    }
}
