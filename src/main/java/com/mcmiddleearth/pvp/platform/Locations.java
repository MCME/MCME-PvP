package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;

/** Converts between the engine's {@link NamedPoint} and Bukkit locations. */
public final class Locations {

    private Locations() {
    }

    /** The location of a point, or empty when its world is not loaded. */
    public static Optional<Location> location(NamedPoint point) {
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch()));
    }

    /** A location, with its facing, as a point. */
    public static NamedPoint point(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("a location without a world has no point");
        }
        return new NamedPoint(world.getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    /** The corner of a block, as a point without a facing (what {@code Game.handleInteract} receives). */
    public static NamedPoint blockPoint(Block block) {
        return NamedPoint.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
