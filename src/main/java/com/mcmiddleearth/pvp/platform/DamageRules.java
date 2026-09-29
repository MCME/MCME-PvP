package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;
import java.util.UUID;

/**
 * 1.1.2's damage rules, applied per game. Only damage that involves a player in a game, or an entity a game owns,
 * is ever cancelled; anything else is left to the server and other plugins. Friendly fire is decided here and not
 * left to the scoreboard team option, so the rule holds however the scoreboard is set up.
 */
final class DamageRules {

    private final GameManager games;
    private final boolean lavaDamage;

    DamageRules(GameManager games, boolean lavaDamage) {
        this.games = games;
        this.lavaDamage = lavaDamage;
    }

    /** True when the engine cancels this damage. */
    boolean cancel(EntityDamageEvent event) {
        Entity victim = event.getEntity();
        Entity source = event instanceof EntityDamageByEntityEvent byEntity ? responsible(byEntity.getDamager()) : null;
        Optional<Game> victimGame = gameOf(victim);
        Optional<Game> sourceGame = source == null ? Optional.empty() : gameOf(source);
        // Only players and game entities count as attackers; other damagers are treated like the environment.
        Entity attacker = source instanceof Player || sourceGame.isPresent() ? source : null;
        if (victimGame.isEmpty() && sourceGame.isEmpty()) {
            return false;
        }
        if (attacker != null && !victimGame.equals(sourceGame)) {
            // Across games, or between a game and the rest of the server.
            return true;
        }
        Game game = victimGame.orElseThrow();
        if (victim instanceof Player player) {
            return cancelForPlayer(event.getCause(), game, session(game, player), attacker);
        }
        return cancelForGameEntity(game, attacker);
    }

    /** The game of a player in a game, or the game that owns an entity. */
    Optional<Game> gameOf(Entity entity) {
        if (entity instanceof Player player) {
            return games.gameOf(player.getUniqueId());
        }
        return owner(entity.getUniqueId());
    }

    /** The open game that spawned the entity. */
    Optional<Game> owner(UUID entity) {
        return games.games().stream().filter(game -> game.entities().owns(entity)).findFirst();
    }

    private boolean cancelForPlayer(DamageCause cause, Game game, PlayerSession victim, Entity attacker) {
        if (cause == DamageCause.VOID || cause == DamageCause.KILL) {
            // Falling out of the world and /kill must always work, as in the lobby: nothing else frees a stuck player.
            return false;
        }
        if (!fighting(victim)) {
            return true;
        }
        if (game.phase() != Phase.RUNNING) {
            // Waiting, countdown and ending are safe, except that lava and fire may still hurt (config lavaDamage).
            return !(attacker == null && lavaDamage && (cause == DamageCause.LAVA || cause == DamageCause.FIRE_TICK));
        }
        if (cause == DamageCause.CONTACT || game.isProtected(victim.id())) {
            return true;
        }
        if (attacker instanceof Player hitter) {
            if (hitter.getUniqueId().equals(victim.id())) {
                return true;
            }
            PlayerSession from = session(game, hitter);
            return !fighting(from) || teammates(from, victim);
        }
        return false;
    }

    private boolean cancelForGameEntity(Game game, Entity attacker) {
        if (game.phase() != Phase.RUNNING) {
            return true;
        }
        return attacker instanceof Player hitter && !fighting(session(game, hitter));
    }

    private static PlayerSession session(Game game, Player player) {
        return game.session(player.getUniqueId()).orElseThrow();
    }

    /** A player who takes part in the fight: not a spectator and not eliminated. */
    private static boolean fighting(PlayerSession session) {
        return session.role() == Role.PLAYER && session.alive();
    }

    /** Same team: the same color, when that color is a real team and not a personal one. */
    private static boolean teammates(PlayerSession a, PlayerSession b) {
        return a.color().isPresent() && a.color().equals(b.color()) && !a.color().get().personal();
    }

    /** Who caused the damage: the shooter of a projectile, else the damager itself. */
    static Entity responsible(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return damager;
    }
}
