package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.GameManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * 1.1.2's protected lobby, behind the config switch {@code lobbyProtection}: players who are not in any game take
 * no damage (except from the void, /kill, and lava or fire when {@code lavaDamage} is on), cannot hurt players,
 * and do not get hungry. A lobby attacker in Creative may still damage a non-player entity, so staff can edit
 * decorations and hit mobs outside games. Players in a game are left to {@link GameEventRouter}. With the switch
 * off, this does nothing.
 */
public final class LobbyProtection implements Listener {

    private final GameManager games;
    private final boolean enabled;
    private final boolean lavaDamage;

    public LobbyProtection(PvpServices services) {
        this.games = services.games();
        this.enabled = services.config().lobbyProtection();
        this.lavaDamage = services.config().lavaDamage();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enabled) {
            return;
        }
        if (event.getEntity() instanceof Player victim && inLobby(victim) && !letThrough(event.getCause())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity
                && responsible(byEntity.getDamager()) instanceof Player attacker && inLobby(attacker)
                && !mayEditInCreative(attacker, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (enabled && event.getEntity() instanceof Player player && inLobby(player)) {
            event.setFoodLevel(20);
        }
    }

    private boolean inLobby(Player player) {
        return games.gameOf(player.getUniqueId()).isEmpty();
    }

    /** Creative is for staff work: they may still break decorations, pop end crystals or hit mobs, never players. */
    private static boolean mayEditInCreative(Player attacker, Entity victim) {
        return attacker.getGameMode() == GameMode.CREATIVE && !(victim instanceof Player);
    }

    /** Falling out of the world and /kill must still work; lava and fire hurt everywhere when lavaDamage is on. */
    private boolean letThrough(DamageCause cause) {
        return cause == DamageCause.VOID || cause == DamageCause.KILL
                || (lavaDamage && (cause == DamageCause.LAVA || cause == DamageCause.FIRE_TICK));
    }

    private static Entity responsible(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return damager;
    }
}
