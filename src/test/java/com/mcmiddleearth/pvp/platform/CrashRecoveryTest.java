package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A player file saved during a game keeps its game state if the server stops without closing the game. */
class CrashRecoveryTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private GameStateMarker marker;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2));
        marker = fixture.contexts().marker();
        fixture.register(new CrashRecovery(fixture.services(), fixture.lobby(), marker));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aMarkedPlayerRejoiningOutsideAGameIsResetAndSentToTheLobby() {
        PlayerMock dave = fixture.player("Dave");
        marker.mark(dave);
        dave.setGameMode(GameMode.SPECTATOR);
        dave.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));

        server.getPluginManager().callEvent(new PlayerJoinEvent(dave, Component.empty()));

        assertEquals(GameMode.ADVENTURE, dave.getGameMode());
        assertTrue(dave.getInventory().isEmpty());
        Location lobby = Locations.location(fixture.services().config().lobby()).orElseThrow();
        assertEquals(lobby.getX(), dave.getLocation().getX(), 1e-9);
        assertEquals(lobby.getY(), dave.getLocation().getY(), 1e-9);
        assertEquals(lobby.getZ(), dave.getLocation().getZ(), 1e-9);
        assertFalse(marker.marked(dave));
    }

    @Test
    void anUnmarkedPlayerIsUntouched() {
        PlayerMock staff = fixture.player("Staff");
        staff.setGameMode(GameMode.CREATIVE);
        staff.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));

        server.getPluginManager().callEvent(new PlayerJoinEvent(staff, Component.empty()));

        assertEquals(GameMode.CREATIVE, staff.getGameMode());
        assertFalse(staff.getInventory().isEmpty());
        assertFalse(marker.marked(staff));
    }
}
