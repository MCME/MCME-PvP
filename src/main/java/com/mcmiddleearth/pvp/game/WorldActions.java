package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.NamedPoint;

import java.util.UUID;

/** What a game asks the server to do with the world. */
public interface WorldActions {

    /** Spawns an entity of a Bukkit {@code EntityType} name (for example "ZOMBIE") and returns its uuid. */
    UUID spawnEntity(String entityType, NamedPoint at);

    /** Removes the entity; a no-op when it is already gone. */
    void removeEntity(UUID entity);
}
