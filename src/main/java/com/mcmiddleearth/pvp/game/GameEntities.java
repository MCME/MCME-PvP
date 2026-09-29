package com.mcmiddleearth.pvp.game;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** The entities one game spawned (mobs, markers). Their deaths are routed to the game, and closing removes them. */
public final class GameEntities {

    private final Set<UUID> owned = new LinkedHashSet<>();

    GameEntities() {
    }

    public boolean owns(UUID entity) {
        return owned.contains(entity);
    }

    /** An unmodifiable copy, in spawn order. */
    public Set<UUID> all() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(owned));
    }

    void add(UUID entity) {
        owned.add(entity);
    }

    void forget(UUID entity) {
        owned.remove(entity);
    }

    /**
     * Removes every owned entity from the world; one that fails to go doesn't keep the others. The first failure is
     * rethrown.
     */
    void removeAll(WorldActions world) {
        RuntimeException failure = null;
        for (UUID entity : List.copyOf(owned)) {
            owned.remove(entity);
            try {
                world.removeEntity(entity);
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
