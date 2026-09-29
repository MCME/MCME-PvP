package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.arena.Region;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameId;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the players and spectators of a game inside its arena's region: a move that would leave it ends at the
 * player's last known spot inside instead. A teleport or a respawn counts as a fresh start, so the next move out
 * goes back to where it landed, never to an older spot such as where the player died.
 * 1.1.2 polled every 10 ticks and teleported; 3.0 corrects each move.
 */
final class RegionGuard {

    /** 1.1.2 repeated the boundary message at most every 3 seconds. */
    static final long WARNING_TICKS = 60;

    private final Map<UUID, State> states = new HashMap<>();

    /** Where a move from {@code from} to {@code to} must end instead; empty when the move may go ahead. */
    Optional<Location> pushBack(UUID player, Game game, Location from, Location to) {
        Region region = game.arena().region();
        if (region == null) {
            return Optional.empty();
        }
        State state = states.get(player);
        if (state == null || !state.game.equals(game.id())) {
            state = new State(game.id());
            states.put(player, state);
        }
        if (inside(region, to)) {
            state.lastInside = to.clone();
            return Optional.empty();
        }
        if (inside(region, from)) {
            // The last accepted position, or wherever a teleport or respawn just put them: always the freshest
            // spot inside, so a stale one from an earlier life is never used.
            state.lastInside = from.clone();
        } else if (onSpawn(game.arena().spawn(), from)) {
            // The game put the player on the arena's spawn, which lies outside the region (a cancelled countdown,
            // an eliminated player's respawn): they walk in from there, like a player who was never inside.
            state.lastInside = null;
        }
        if (state.lastInside == null) {
            // Never inside yet (an arena spawn outside its region): nothing to go back to.
            return Optional.empty();
        }
        Location back = state.lastInside.clone();
        back.setYaw(to.getYaw());
        back.setPitch(to.getPitch());
        return Optional.of(back);
    }

    /** True when the boundary message may be shown now: at most once per {@link #WARNING_TICKS} per player. */
    boolean shouldWarn(UUID player, long currentTick) {
        State state = states.get(player);
        if (state == null || (state.warned && currentTick - state.lastWarning < WARNING_TICKS)) {
            return false;
        }
        state.warned = true;
        state.lastWarning = currentTick;
        return true;
    }

    /** Drops what is known about a player (they quit). */
    void forget(UUID player) {
        states.remove(player);
    }

    private static boolean inside(Region region, Location location) {
        return location.getWorld() != null
                && region.contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    /** Whether the location is the arena's spawn, where only the game itself puts players. */
    private static boolean onSpawn(NamedPoint spawn, Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(spawn.world())
                && Math.abs(location.getX() - spawn.x()) < 1e-6
                && Math.abs(location.getY() - spawn.y()) < 1e-6
                && Math.abs(location.getZ() - spawn.z()) < 1e-6;
    }

    /** What the guard knows about one player in one game. */
    private static final class State {

        private final GameId game;
        private Location lastInside;
        private boolean warned;
        private long lastWarning;

        private State(GameId game) {
            this.game = game;
        }
    }
}
