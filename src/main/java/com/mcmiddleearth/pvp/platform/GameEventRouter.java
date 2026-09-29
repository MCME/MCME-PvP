package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The engine's one Bukkit listener. It finds the game of the player, or of the game-spawned entity, that an event is
 * about and hands the event to that game. Players who are not in a game are never touched: pvpserver also runs
 * other minigames and plugins. (The optional protected lobby of 1.1.2 is a listener of its own.)
 */
public final class GameEventRouter implements Listener {

    /** 1.1.2 removed arrows stuck in blocks after 9 seconds. */
    static final long ARROW_REMOVAL_TICKS = 180;

    /**
     * Blocks players in a game may not use; chests are supply points, handled by {@link SupplyChests}. Every
     * anvil kind is covered by {@link Tag#ANVIL} instead of listed here.
     */
    private static final Set<Material> CONTAINERS = Set.of(
            Material.BEACON, Material.FURNACE, Material.TRAPPED_CHEST, Material.CRAFTING_TABLE);

    private final GameManager games;
    private final Messages messages;
    private final Plugin plugin;
    private final DamageRules damage;
    private final RegionGuard region = new RegionGuard();
    /** The vanilla death text of each dying game player, from the edits at HIGH until the count at MONITOR. */
    private final Map<UUID, Component> deathTexts = new HashMap<>();

    public GameEventRouter(PvpServices services, Plugin plugin) {
        this.games = services.games();
        this.messages = services.messages();
        this.plugin = plugin;
        this.damage = new DamageRules(services.games(), services.config().lavaDamage());
    }

    // ----- death, respawn and quit

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID victim = event.getPlayer().getUniqueId();
        if (games.gameOf(victim).isEmpty()) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        Component vanilla = event.deathMessage();
        event.deathMessage(null);
        if (vanilla != null && !Component.empty().equals(vanilla)) {
            deathTexts.put(victim, vanilla);
        }
    }

    // A plugin can cancel a player's death too (Paper then revives them), so the game counts a death only once no
    // plugin can cancel it any more. Not ignoreCancelled: the kept death text is always dropped.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDied(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Component vanilla = deathTexts.remove(victim.getUniqueId());
        if (event.isCancelled()) {
            return;
        }
        games.gameOf(victim.getUniqueId()).ifPresent(game -> {
            Player killer = victim.getKiller();
            UUID killerId = killer == null ? null : killer.getUniqueId();
            if (vanilla != null && game.creditedKiller(victim.getUniqueId(), killerId).isEmpty()) {
                // The game's listeners announce a credited kill. Every other death (no killer, a killer who left
                // the game, a teammate) keeps the vanilla text, for this game only.
                tell(game.audience(), vanilla);
            }
            game.handleDeath(victim.getUniqueId(), killerId);
        });
    }

    // A plugin can cancel an entity's death (Paper then revives it), so the game only hears of a death that stands.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        damage.owner(entity.getUniqueId()).ifPresent(game -> {
            Player killer = entity.getKiller();
            game.handleEntityDeath(entity.getUniqueId(), killer == null ? null : killer.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        games.gameOf(player).ifPresent(game -> {
            game.respawnPoint(player).flatMap(Locations::location).ifPresent(event::setRespawnLocation);
            game.handleRespawn(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        region.forget(player);
        games.leave(player, LeaveReason.QUIT);
    }

    // ----- damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (damage.cancel(event)) {
            event.setCancelled(true);
        }
    }

    // A player who leaves soon after another player hurt them counts as killed, so the game remembers who hit whom,
    // directly or with a projectile, once no plugin can cancel the damage any more.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim && event instanceof EntityDamageByEntityEvent byEntity
                && DamageRules.responsible(byEntity.getDamager()) instanceof Player attacker) {
            games.gameOf(victim.getUniqueId())
                    .ifPresent(game -> game.handleHit(victim.getUniqueId(), attacker.getUniqueId()));
        }
    }

    // ----- movement: the countdown freeze and the arena's region

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Optional<Game> found = games.gameOf(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        Game game = found.get();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (game.isFrozen(player.getUniqueId())) {
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                // Frozen players stay on their spawn but may look around.
                Location still = from.clone();
                still.setYaw(to.getYaw());
                still.setPitch(to.getPitch());
                event.setTo(still);
            }
            return;
        }
        region.pushBack(player.getUniqueId(), game, from, to).ifPresent(back -> {
            event.setTo(back);
            if (region.shouldWarn(player.getUniqueId(), plugin.getServer().getCurrentTick())) {
                player.sendMessage(messages.get(Msg.GAME_BOUNDARY));
            }
        });
    }

    // ----- items, blocks and hunger

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isPlayer(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // ADVENTURE mode does not stop these on Paper 26.2, so the router closes them itself: no outside gear comes
    // into a game, and the kit stays exactly as handed out.
    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameChange(PlayerItemFrameChangeEvent event) {
        if (isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapWithEquipmentSlot(PlayerSwapWithEquipmentSlotEvent event) {
        if (isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isPlayer(player)) {
            event.setCancelled(true);
        }
    }

    // Separate from EntityPickupItemEvent (1.1.2's ArrowHandler): below 24 arrows, a shot arrow is still picked up.
    @EventHandler(ignoreCancelled = true)
    public void onPickupArrow(PlayerPickupArrowEvent event) {
        Player player = event.getPlayer();
        if (isPlayer(player) && player.getInventory().contains(Material.ARROW, 24)) {
            event.setCancelled(true);
        }
    }

    // A minecart on rails, or a boat a player doesn't steer, never fires a PlayerMoveEvent, so it could otherwise
    // carry a game member out of the region.
    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player player && games.gameOf(player.getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    // Not ignoreCancelled: a click must still reach the game when another plugin denied the block use, for future
    // capture modes.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        Optional<Game> found = games.gameOf(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        if (isContainer(block.getType())) {
            event.setCancelled(true);
        }
        // Right-clicks fire once per hand; the game hears about each click once.
        if (event.getHand() == EquipmentSlot.HAND) {
            found.get().handleInteract(player.getUniqueId(), Locations.blockPoint(block));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && games.gameOf(player.getUniqueId()).isPresent()) {
            event.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow) || event.getHitBlock() == null) {
            return;
        }
        if (arrow.getShooter() instanceof Entity shooter && damage.gameOf(shooter).isPresent()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, arrow::remove, ARROW_REMOVAL_TICKS);
        }
    }

    private static boolean isContainer(Material type) {
        return CONTAINERS.contains(type) || type.name().endsWith("SHULKER_BOX") || Tag.ANVIL.isTagged(type)
                || type == Material.DECORATED_POT || Tag.WOODEN_SHELVES.isTagged(type);
    }

    /** A present player of a game (not a spectator): the kit is theirs to keep, not to drop or rearrange. */
    private boolean isPlayer(Player player) {
        UUID id = player.getUniqueId();
        return games.gameOf(id).flatMap(game -> game.session(id)).map(s -> s.role() == Role.PLAYER).orElse(false);
    }

    private void tell(List<UUID> audience, Component message) {
        for (UUID id : audience) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}
