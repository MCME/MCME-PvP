package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.ProbeMode;
import com.mcmiddleearth.pvp.testing.TestArenas;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Items, blocks, hunger, stuck arrows and clicks, for the players of a game only. */
class GameEventRouterItemsTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private ProbeMode probe;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        probe = new ProbeMode("probe", 0, true);
        fixture = PaperFixture.create(server, dir, PvpConfig.defaults(), List.of(probe),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-PROBE", 100, 4).withMode("probe"));
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        world = server.getWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playersInAGameCannotDropRearrangeOrSwapTheirKit() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));

        assertTrue(drop(alice).isCancelled());
        assertTrue(click(alice).isCancelled());
        assertTrue(swap(alice).isCancelled());
    }

    @Test
    void playersOutsideGamesKeepControlOfTheirItems() {
        PlayerMock dave = fixture.player("Dave");

        assertFalse(drop(dave).isCancelled());
        assertFalse(click(dave).isCancelled());
        assertFalse(swap(dave).isCancelled());
    }

    @Test
    void containersCannotBeUsedInAGame() {
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        for (Material type : List.of(Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.FURNACE,
                Material.TRAPPED_CHEST, Material.CRAFTING_TABLE, Material.SHULKER_BOX, Material.RED_SHULKER_BOX,
                Material.DECORATED_POT, Material.OAK_SHELF)) {
            assertEquals(Event.Result.DENY, rightClick(alice, block(type), EquipmentSlot.HAND).useInteractedBlock(),
                    type.name());
        }
        assertEquals(Event.Result.ALLOW, rightClick(alice, block(Material.STONE_BUTTON), EquipmentSlot.HAND).useInteractedBlock());
        assertEquals(Event.Result.ALLOW, rightClick(alice, block(Material.CHEST), EquipmentSlot.HAND).useInteractedBlock(),
                "chests are left to SupplyChests");
    }

    @Test
    void containersStayUsableOutsideGames() {
        PlayerMock dave = fixture.player("Dave");

        assertEquals(Event.Result.ALLOW, rightClick(dave, block(Material.FURNACE), EquipmentSlot.HAND).useInteractedBlock());
    }

    @Test
    void hungerIsPinnedForPlayersInAGameOnly() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertEquals(20, hunger(alice).getFoodLevel());
        assertEquals(5, hunger(dave).getFoodLevel());
    }

    @Test
    void arrowsOfGamePlayersDisappearFromBlocksAfter9Seconds() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        running("AS-TDM", alice, fixture.player("Bob"));
        Arrow own = stuckArrow(alice);
        Arrow outsider = stuckArrow(dave);

        server.getScheduler().performTicks(GameEventRouter.ARROW_REMOVAL_TICKS - 1);
        assertTrue(own.isValid());
        server.getScheduler().performTicks(1);
        assertFalse(own.isValid());
        assertTrue(outsider.isValid());
    }

    @Test
    void aClickOnABlockReachesTheGameOnce() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-PROBE", alice, fixture.player("Bob"));
        Block target = world.getBlockAt(103, 64, 105);
        target.setType(Material.STONE);

        rightClick(alice, target, EquipmentSlot.HAND);
        rightClick(alice, target, EquipmentSlot.OFF_HAND);

        assertEquals(List.of("Alice world 103 64 105"), probe.interactions());
    }

    @Test
    void playersInAGameCannotManipulateArmorStandsOrItemFramesOrSwapEquipmentByHand() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));

        assertTrue(armorStandManipulate(alice).isCancelled());
        assertTrue(itemFrameChange(alice).isCancelled());
        assertTrue(swapWithEquipmentSlot(alice).isCancelled());
    }

    @Test
    void playersOutsideGamesCanManipulateArmorStandsAndItemFramesAndSwapEquipmentByHand() {
        PlayerMock dave = fixture.player("Dave");

        assertFalse(armorStandManipulate(dave).isCancelled());
        assertFalse(itemFrameChange(dave).isCancelled());
        assertFalse(swapWithEquipmentSlot(dave).isCancelled());
    }

    @Test
    void spectatorsCanDropAndRearrangeItems() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, fixture.player("Bob"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertFalse(drop(carol).isCancelled());
        assertFalse(click(carol).isCancelled());
    }

    @Test
    void playersInAGameCannotPickUpOutsideItems() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));

        assertTrue(pickupItem(alice).isCancelled());
    }

    @Test
    void playersOutsideGamesCanPickUpItems() {
        PlayerMock dave = fixture.player("Dave");

        assertFalse(pickupItem(dave).isCancelled());
    }

    @Test
    void anArrowIsPickedUpOnlyBelow24() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        alice.getInventory().setItem(8, new ItemStack(Material.ARROW, 23));

        assertFalse(pickupArrow(alice).isCancelled());

        alice.getInventory().setItem(8, new ItemStack(Material.ARROW, 24));

        assertTrue(pickupArrow(alice).isCancelled());
    }

    @Test
    void gameMembersCannotMountAnything() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, fixture.player("Bob"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertTrue(mount(alice).isCancelled());
        assertTrue(mount(carol).isCancelled());
    }

    @Test
    void playersOutsideGamesCanMount() {
        PlayerMock dave = fixture.player("Dave");

        assertFalse(mount(dave).isCancelled());
    }

    @Test
    void arrowsThatHitAnEntityAreNotScheduledForRemoval() {
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));
        Arrow arrow = world.spawn(alice.getLocation(), Arrow.class);
        arrow.setShooter(alice);
        Zombie target = world.spawn(alice.getLocation(), Zombie.class);

        server.getPluginManager().callEvent(new ProjectileHitEvent(arrow, target));

        server.getScheduler().performTicks(GameEventRouter.ARROW_REMOVAL_TICKS);
        assertTrue(arrow.isValid());
    }

    private Game running(String arena, PlayerMock... players) {
        Game game = fixture.open(arena);
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private Block block(Material type) {
        Block block = world.getBlockAt(5, 64, 5);
        block.setType(type);
        return block;
    }

    private PlayerInteractEvent rightClick(Player player, Block block, EquipmentSlot hand) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, hand);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private PlayerDropItemEvent drop(Player player) {
        Item item = world.dropItem(player.getLocation(), new ItemStack(Material.IRON_SWORD));
        PlayerDropItemEvent event = new PlayerDropItemEvent(player, item);
        server.getPluginManager().callEvent(event);
        return event;
    }

    /** MockBukkit's default inventory view cannot convert slots, so the click goes to an opened chest view. */
    private InventoryClickEvent click(PlayerMock player) {
        InventoryView view = player.openInventory(server.createInventory(null, 9));
        return new PlayerSimulation(player).simulateInventoryClick(view, 0);
    }

    private PlayerSwapHandItemsEvent swap(Player player) {
        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, new ItemStack(Material.BOW),
                new ItemStack(Material.IRON_SWORD));
        server.getPluginManager().callEvent(event);
        return event;
    }

    private FoodLevelChangeEvent hunger(Player player) {
        FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, 5);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private Arrow stuckArrow(Player shooter) {
        Location at = shooter.getLocation();
        Arrow arrow = world.spawn(at, Arrow.class);
        arrow.setShooter(shooter);
        server.getPluginManager().callEvent(new ProjectileHitEvent(arrow, null, world.getBlockAt(at), BlockFace.UP));
        return arrow;
    }

    private PlayerArmorStandManipulateEvent armorStandManipulate(Player player) {
        ArmorStand stand = world.spawn(player.getLocation(), ArmorStand.class);
        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(player, stand,
                new ItemStack(Material.AIR), new ItemStack(Material.AIR), EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private PlayerItemFrameChangeEvent itemFrameChange(Player player) {
        ItemFrame frame = world.spawn(player.getLocation(), ItemFrame.class);
        PlayerItemFrameChangeEvent event = new PlayerItemFrameChangeEvent(player, frame,
                new ItemStack(Material.ARROW), PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private PlayerSwapWithEquipmentSlotEvent swapWithEquipmentSlot(Player player) {
        PlayerSwapWithEquipmentSlotEvent event = new PlayerSwapWithEquipmentSlotEvent(player,
                new ItemStack(Material.IRON_HELMET), EquipmentSlot.HEAD, new ItemStack(Material.AIR));
        server.getPluginManager().callEvent(event);
        return event;
    }

    private EntityPickupItemEvent pickupItem(Player player) {
        Item item = world.dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT));
        EntityPickupItemEvent event = new EntityPickupItemEvent(player, item, 0);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private PlayerPickupArrowEvent pickupArrow(Player player) {
        Item item = world.dropItem(player.getLocation(), new ItemStack(Material.ARROW));
        Arrow arrow = world.spawn(player.getLocation(), Arrow.class);
        PlayerPickupArrowEvent event = new PlayerPickupArrowEvent(player, item, arrow);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private EntityMountEvent mount(Player player) {
        EntityMountEvent event = new EntityMountEvent(player, world.spawn(player.getLocation(), Zombie.class));
        server.getPluginManager().callEvent(event);
        return event;
    }
}
