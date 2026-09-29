package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperLobbyTest {

    private ServerMock server;
    private PaperLobby lobby;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        lobby = new PaperLobby(PvpConfig.defaults().lobby());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void sendKeepsInventoryGameModeAndEffects() {
        PlayerMock player = server.addPlayer("Alice");
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(5);
        player.setFoodLevel(3);

        lobby.send(player.getUniqueId());

        Location at = player.getLocation();
        assertEquals("world", at.getWorld().getName());
        assertEquals(344.47, at.getX(), 1e-9);
        assertEquals(39, at.getY(), 1e-9);
        assertEquals(521.58, at.getZ(), 1e-9);
        assertEquals(0.3f, at.getYaw());
        assertEquals(-24.15f, at.getPitch());
        assertEquals(new ItemStack(Material.IRON_SWORD), player.getInventory().getItem(0));
        assertEquals(new ItemStack(Material.LEATHER_HELMET), player.getInventory().getHelmet());
        assertFalse(player.getActivePotionEffects().isEmpty());
        assertEquals(GameMode.SPECTATOR, player.getGameMode());
        assertEquals(5, player.getHealth());
        assertEquals(3, player.getFoodLevel());
    }

    @Test
    void resetTurnsFlightOff() {
        PlayerMock player = server.addPlayer("Bob");
        player.setAllowFlight(true);
        player.setFlying(true);

        lobby.reset(player);

        assertFalse(player.isFlying());
        assertFalse(player.getAllowFlight());
    }

    @Test
    void aDeadPlayerIsNotHealed() {
        PlayerMock player = server.addPlayer("Casey");
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.setHealth(0);

        lobby.reset(player);

        assertEquals(0, player.getHealth());
        assertEquals(GameMode.ADVENTURE, player.getGameMode());
        assertTrue(player.getInventory().isEmpty());
    }

    @Test
    void sendDoesNotTeleportOrSetTheRespawnOfADeadPlayer() {
        // Paper refuses to teleport a dead player, and LobbyRespawn places their eventual respawn at the lobby, so
        // send() leaves a dead player alone.
        PlayerMock player = server.addPlayer("Erin");
        Location before = player.getLocation();
        Location respawnBefore = player.getRespawnLocation();
        player.setHealth(0);

        lobby.send(player.getUniqueId());

        Location at = player.getLocation();
        assertEquals(before.getWorld(), at.getWorld());
        assertEquals(before.getX(), at.getX(), 1e-9);
        assertEquals(before.getY(), at.getY(), 1e-9);
        assertEquals(before.getZ(), at.getZ(), 1e-9);
        assertEquals(respawnBefore, player.getRespawnLocation());
    }

    @Test
    void resetClearsFireFallDistanceAndAbsorption() {
        PlayerMock player = server.addPlayer("Frank");
        player.setFireTicks(100);
        player.setFallDistance(12.5f);
        player.setAbsorptionAmount(4);

        lobby.reset(player);

        assertEquals(0, player.getFireTicks());
        assertEquals(0f, player.getFallDistance());
        assertEquals(0, player.getAbsorptionAmount());
    }

    @Test
    void resetClearsExperience() {
        PlayerMock player = server.addPlayer("Gina");
        player.setExp(0.5f);
        player.setLevel(9);
        player.setTotalExperience(123);

        lobby.reset(player);

        assertEquals(0f, player.getExp());
        assertEquals(0, player.getLevel());
        assertEquals(0, player.getTotalExperience());
    }

    @Test
    void sendIgnoresAPlayerWhoIsNotOnline() {
        assertDoesNotThrow(() -> lobby.send(UUID.randomUUID()));
    }
}
