package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Chests and copper chests in arenas are supply points, never storage (1.1.2): a player in a game cannot open one,
 * and a fighting player with fewer than 24 arrows gets their quiver refilled, at the cost of a short slowness. The
 * action bar counts the restock down ("Restocking supplies… 3, 2, 1"), then shows "Restocked!" for a few seconds.
 */
public final class SupplyChests implements Listener, GameListener {

    /** 1.1.2's quiver: 24 arrows in the last hotbar slot. */
    static final int ARROWS = 24;
    static final int ARROW_SLOT = 8;
    /** One countdown step: 1 second. */
    private static final int STEP_TICKS = 20;
    /** The slowness, and the countdown to "Restocked!", both last 3 seconds. */
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int SLOWNESS_TICKS = COUNTDOWN_SECONDS * STEP_TICKS;
    /** Held from the first line until 3 more seconds after "Restocked!" shows. */
    private static final int HOLD_TICKS = SLOWNESS_TICKS + 3 * STEP_TICKS;

    private final PvpServices services;
    private final Plugin plugin;
    private final ActionBarHolds holds;
    /** The running countdown of each player currently being restocked, so a leave, death or new restock cancels it. */
    private final Map<UUID, BukkitTask> countdowns = new HashMap<>();

    public SupplyChests(PvpServices services, Plugin plugin, ActionBarHolds holds) {
        this.services = services;
        this.plugin = plugin;
        this.holds = holds;
    }

    // Not ignoreCancelled: arenas sit inside protected regions, so a protection plugin may already have denied this
    // click, but the restock must still happen.
    @EventHandler(priority = EventPriority.HIGH)
    public void onChest(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || !isSupplyChest(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        Optional<Game> found = services.games().gameOf(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND || !mayRestock(found.get(), player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(Material.ARROW, ARROWS)) {
            return;
        }
        inventory.setItem(ARROW_SLOT, new ItemStack(Material.ARROW, ARROWS));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_TICKS, 5, true, false));
        startCountdown(found.get(), player);
    }

    /** A player who left, quit or died gets no further restock notices, and their action bar is freed at once. */
    @Override
    public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
        cancelCountdown(session.id());
        holds.release(session.id());
    }

    @Override
    public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        cancelCountdown(victim.id());
        holds.release(victim.id());
    }

    /** Shows the first line at once, then schedules the rest of the countdown. */
    private void startCountdown(Game game, Player player) {
        UUID id = player.getUniqueId();
        cancelCountdown(id);
        holds.hold(id, HOLD_TICKS);
        player.sendActionBar(restocking(COUNTDOWN_SECONDS));
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, new Countdown(game, player), STEP_TICKS, STEP_TICKS);
        countdowns.put(id, task);
    }

    private void cancelCountdown(UUID id) {
        BukkitTask task = countdowns.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private Component restocking(int secondsLeft) {
        return services.messages().get(Msg.GAME_RESTOCKING, Map.of("seconds", String.valueOf(secondsLeft)));
    }

    /** A chest, or any copper chest (oxidized or waxed alike): both are supply points. */
    private static boolean isSupplyChest(Material type) {
        return type == Material.CHEST || Tag.COPPER_CHESTS.isTagged(type);
    }

    /** A present, fighting player (alive, not a spectator) of a running game whose mode restocks from chests. */
    static boolean mayRestock(Game game, Player player) {
        return game.phase() == Phase.RUNNING
                && game.rules().chestRestock()
                && game.session(player.getUniqueId())
                        .map(s -> s.role() == Role.PLAYER && s.alive() && s.present()).orElse(false);
    }

    /**
     * The two remaining counted lines and then "Restocked!", one step (1 second) apart; cancels itself once shown,
     * or as soon as the player is no longer fighting in {@code game}.
     */
    private final class Countdown implements Runnable {

        private final Game game;
        private final Player player;
        private int secondsLeft = COUNTDOWN_SECONDS - 1;

        private Countdown(Game game, Player player) {
            this.game = game;
            this.player = player;
        }

        @Override
        public void run() {
            UUID id = player.getUniqueId();
            if (!mayRestock(game, player)) {
                cancelCountdown(id);
                return;
            }
            if (secondsLeft > 0) {
                player.sendActionBar(restocking(secondsLeft));
                secondsLeft--;
            } else {
                player.sendActionBar(services.messages().get(Msg.GAME_RESTOCKED));
                cancelCountdown(id);
            }
        }
    }
}
