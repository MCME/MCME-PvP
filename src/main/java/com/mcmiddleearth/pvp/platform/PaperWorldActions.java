package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.WorldActions;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.UUID;

/** Spawns and removes the entities a game owns. */
public final class PaperWorldActions implements WorldActions {

    private final Server server;

    public PaperWorldActions(Server server) {
        this.server = server;
    }

    @Override
    public UUID spawnEntity(String entityType, NamedPoint at) {
        Location location = Locations.location(at)
                .orElseThrow(() -> new IllegalStateException("world " + at.world() + " is not loaded"));
        EntityType type = EntityType.valueOf(entityType.toUpperCase(Locale.ROOT));
        return location.getWorld().spawnEntity(location, type).getUniqueId();
    }

    @Override
    public void removeEntity(UUID entity) {
        Entity found = server.getEntity(entity);
        if (found != null) {
            found.remove();
        }
    }
}
