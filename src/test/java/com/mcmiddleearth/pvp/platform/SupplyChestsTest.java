package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.ProbeMode;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chests in arenas refill a fighter's arrows (1.1.2) and are never opened by players in a game. */
class SupplyChestsTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Block chest;
    private ActionBarHolds holds;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, PvpConfig.defaults(), List.of(new ProbeMode("norestock", 0, false)),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 200, 4),
                TestArenas.ffa("AS-NR", 100, 4).withMode("norestock"));
        holds = new ActionBarHolds(server::getCurrentTick);
        SupplyChests supplyChests = new SupplyChests(fixture.services(), fixture.plugin(), holds);
        fixture.register(supplyChests);
        fixture.listeners().add(supplyChests);
        chest = server.getWorld("world").getBlockAt(5, 64, 5);
        chest.setType(Material.CHEST);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aFighterWithFewArrowsIsRestockedAndSlowedDown() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertEquals(SupplyChests.ARROWS, alice.getInventory().getItem(SupplyChests.ARROW_SLOT).getAmount());
        PotionEffect slowness = alice.getPotionEffect(PotionEffectType.SLOWNESS);
        assertEquals(5, slowness.getAmplifier());
        assertEquals(60, slowness.getDuration());
        assertTrue(slowness.isAmbient());
        assertFalse(slowness.hasParticles());
        assertEquals("Restocking supplies… 3", PaperFixture.plain(alice.nextActionBar()));
        assertTrue(holds.held(alice.getUniqueId()));
    }

    @Test
    void theRestockCountdownCountsDownThenShowsRestocked() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(alice, EquipmentSlot.HAND);
        assertEquals("Restocking supplies… 3", PaperFixture.plain(alice.nextActionBar()));

        server.getScheduler().performTicks(20);
        assertEquals("Restocking supplies… 2", PaperFixture.plain(alice.nextActionBar()));

        server.getScheduler().performTicks(20);
        assertEquals("Restocking supplies… 1", PaperFixture.plain(alice.nextActionBar()));

        server.getScheduler().performTicks(20);
        assertEquals("Restocked!", PaperFixture.plain(alice.nextActionBar()));
    }

    @Test
    void theActionBarStaysHeldUntilThreeSecondsAfterRestocked() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(alice, EquipmentSlot.HAND);

        assertTrue(holds.held(alice.getUniqueId()));
        server.getScheduler().performTicks(119);
        assertTrue(holds.held(alice.getUniqueId()));
        server.getScheduler().performTicks(1);
        assertFalse(holds.held(alice.getUniqueId()));
    }

    @Test
    void leavingTheGamePartwayStopsTheCountdown() {
        // A three-player game so the leaver's team still has a member left: the game keeps running, and only the
        // countdown's own bookkeeping (not the game ending) can be what silences the notices.
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock leaver = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();
        leaver.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(leaver, EquipmentSlot.HAND);
        assertEquals("Restocking supplies… 3", PaperFixture.plain(leaver.nextActionBar()));

        fixture.services().games().leave(leaver.getUniqueId(), LeaveReason.LEFT);
        assertEquals(Phase.RUNNING, game.phase());
        server.getScheduler().performTicks(60);

        assertNull(leaver.nextActionBar());
    }

    @Test
    void dyingCancelsTheRestockCountdownEvenAfterAQuickRespawn() {
        // FFA respawns within a tick, alive again: only the death notification (not a lingering "not alive" state)
        // can be what stops the stale countdown from a life that already ended.
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(alice, EquipmentSlot.HAND);
        assertEquals("Restocking supplies… 3", PaperFixture.plain(alice.nextActionBar()));

        alice.simulateDamage(100, bob);
        alice.respawn();
        server.getScheduler().performOneTick();
        assertEquals(Phase.RUNNING, game.phase());

        server.getScheduler().performTicks(20);

        assertNull(alice.nextActionBar());
    }

    @Test
    void dyingMidCountdownReleasesTheHeldActionBar() {
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-FFA", alice, bob);
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(alice, EquipmentSlot.HAND);
        assertTrue(holds.held(alice.getUniqueId()));

        alice.simulateDamage(100, bob);

        assertFalse(holds.held(alice.getUniqueId()));
    }

    @Test
    void leavingMidCountdownReleasesTheHeldActionBar() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock leaver = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();
        leaver.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        rightClick(leaver, EquipmentSlot.HAND);
        assertTrue(holds.held(leaver.getUniqueId()));

        fixture.services().games().leave(leaver.getUniqueId(), LeaveReason.LEFT);

        assertFalse(holds.held(leaver.getUniqueId()));
    }

    @Test
    void mayRestockRequiresThePlayerToStillBePresent() {
        // Drives mayRestock directly, bypassing playerLeft, to prove the check does not depend on that
        // notification alone.
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock leaver = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();

        assertTrue(SupplyChests.mayRestock(game, leaver));

        fixture.services().games().leave(leaver.getUniqueId(), LeaveReason.LEFT);

        assertFalse(SupplyChests.mayRestock(game, leaver));
    }

    @Test
    void aCopperChestRestocksJustLikeAChest() {
        chest.setType(Material.COPPER_CHEST);
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertEquals(SupplyChests.ARROWS, alice.getInventory().getItem(SupplyChests.ARROW_SLOT).getAmount());
        assertEquals("Restocking supplies… 3", PaperFixture.plain(alice.nextActionBar()));
    }

    @Test
    void aWaxedOxidizedCopperChestRestocksJustLikeAChest() {
        chest.setType(Material.WAXED_OXIDIZED_COPPER_CHEST);
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertEquals(SupplyChests.ARROWS, alice.getInventory().getItem(SupplyChests.ARROW_SLOT).getAmount());
        assertEquals("Restocking supplies… 3", PaperFixture.plain(alice.nextActionBar()));
    }

    @Test
    void aFullQuiverIsLeftAsItIs() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertNull(alice.getPotionEffect(PotionEffectType.SLOWNESS));
        assertNull(alice.nextActionBar());
    }

    @Test
    void onlyTheMainHandRestocks() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.OFF_HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertEquals(3, alice.getInventory().getItem(SupplyChests.ARROW_SLOT).getAmount());
    }

    @Test
    void waitingPlayersAndSpectatorsGetNoArrows() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice);
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertEquals(Event.Result.DENY, rightClick(alice, EquipmentSlot.HAND).useInteractedBlock());
        assertEquals(Event.Result.DENY, rightClick(carol, EquipmentSlot.HAND).useInteractedBlock());
        assertNull(alice.getInventory().getItem(SupplyChests.ARROW_SLOT));
        assertNull(carol.getInventory().getItem(SupplyChests.ARROW_SLOT));
    }

    @Test
    void modesWithoutRestockOnlyKeepTheChestShut() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-NR", alice, fixture.player("Bob"));
        alice.getInventory().setItem(SupplyChests.ARROW_SLOT, new ItemStack(Material.ARROW, 3));

        PlayerInteractEvent click = rightClick(alice, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertEquals(3, alice.getInventory().getItem(SupplyChests.ARROW_SLOT).getAmount());
    }

    @Test
    void chestsOutsideGamesAreLeftAlone() {
        PlayerMock dave = fixture.player("Dave");
        running("AS-TDM", fixture.player("Alice"), fixture.player("Bob"));

        assertEquals(Event.Result.ALLOW, rightClick(dave, EquipmentSlot.HAND).useInteractedBlock());
        assertNull(dave.getInventory().getItem(SupplyChests.ARROW_SLOT));
    }

    @Test
    void aSpectatorOfARunningGameGetsNoArrows() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, fixture.player("Bob"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        PlayerInteractEvent click = rightClick(carol, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertNull(carol.getInventory().getItem(SupplyChests.ARROW_SLOT));
        assertNull(carol.getPotionEffect(PotionEffectType.SLOWNESS));
    }

    @Test
    void anEliminatedPlayerGetsNoArrows() {
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock loner = players.stream().filter(p -> teamSize(game, p) == 1).findFirst().orElseThrow();
        PlayerMock victim = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();
        victim.simulateDamage(100, loner);

        PlayerInteractEvent click = rightClick(victim, EquipmentSlot.HAND);

        assertEquals(Event.Result.DENY, click.useInteractedBlock());
        assertNull(victim.getInventory().getItem(SupplyChests.ARROW_SLOT));
        assertNull(victim.getPotionEffect(PotionEffectType.SLOWNESS));
        assertEquals(Phase.RUNNING, game.phase());
    }

    private Game running(String arena, PlayerMock... players) {
        Game game = fixture.open(arena);
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private static int teamSize(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().team().orElseThrow().members().size();
    }

    private PlayerInteractEvent rightClick(Player player, EquipmentSlot hand) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, chest, BlockFace.UP, hand);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
