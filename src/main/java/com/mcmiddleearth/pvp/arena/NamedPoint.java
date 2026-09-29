package com.mcmiddleearth.pvp.arena;

/** A position with a facing, in a world given by name. */
public record NamedPoint(String world, double x, double y, double z, float yaw, float pitch) {

    public static NamedPoint of(String world, double x, double y, double z) {
        return new NamedPoint(world, x, y, z, 0f, 0f);
    }

    /** A 1.1.2 map spawn, stored as the admin's feet block: the centre of that block. */
    public static NamedPoint fromLegacyFeet(String world, int x, int y, int z) {
        return new NamedPoint(world, x + 0.5, y, z + 0.5, 0f, 0f);
    }

    /** A 1.1.2 important point, stored one block below the admin's feet: standing on that block, at its centre. */
    public static NamedPoint fromLegacyFloor(String world, int x, int y, int z) {
        return new NamedPoint(world, x + 0.5, y + 1, z + 0.5, 0f, 0f);
    }
}
