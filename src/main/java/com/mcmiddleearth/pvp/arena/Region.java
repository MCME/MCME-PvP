package com.mcmiddleearth.pvp.arena;

import java.util.List;
import java.util.Objects;

/**
 * A play area: a polygon on the x/z plane between two heights, in one world. Block columns are tested with
 * WorldEdit's {@code Polygonal2DRegion.contains}, the check 1.1.2 used, so every map's boundary stays exactly
 * where it was: corners and edges count as inside.
 */
public record Region(String world, List<Region.Vertex> vertices, int minY, int maxY) {

    /** A corner of the polygon, in block coordinates. */
    public record Vertex(int x, int z) {
    }

    /** The smallest box around the polygon, in block coordinates; both ends are inside. */
    public record Bounds(int minX, int minZ, int maxX, int maxZ) {

        /** True when the boxes share at least one block column; touching edges count. */
        public boolean intersects(Bounds other) {
            return minX <= other.maxX && other.minX <= maxX && minZ <= other.maxZ && other.minZ <= maxZ;
        }
    }

    public Region {
        vertices = List.copyOf(Objects.requireNonNull(vertices, "\"vertices\" is missing"));
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("a region needs at least 3 vertices, got " + vertices.size());
        }
        if (minY > maxY) {
            throw new IllegalArgumentException("minY " + minY + " is above maxY " + maxY);
        }
    }

    public Bounds bounds() {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.x());
            minZ = Math.min(minZ, vertex.z());
            maxX = Math.max(maxX, vertex.x());
            maxZ = Math.max(maxZ, vertex.z());
        }
        return new Bounds(minX, minZ, maxX, maxZ);
    }

    /** True when the block at (x, y, z) in {@code world} is inside: the right world, height and column. */
    public boolean contains(String world, double x, double y, double z) {
        if (!this.world.equals(world)) {
            return false;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }
        int blockY = (int) Math.floor(y);
        if (blockY < minY || blockY > maxY) {
            return false;
        }
        return containsColumn((int) Math.floor(x), (int) Math.floor(z));
    }

    /** Same world and overlapping bounding boxes: two games there could run into each other. */
    public boolean overlaps(Region other) {
        return world.equals(other.world) && bounds().intersects(other.bounds());
    }

    /** WorldEdit's Polygonal2DRegion.contains for the column (bx, bz), kept as it is so boundaries match 1.1.2. */
    private boolean containsColumn(int bx, int bz) {
        boolean inside = false;
        Vertex last = vertices.get(vertices.size() - 1);
        int xOld = last.x();
        int zOld = last.z();
        for (Vertex vertex : vertices) {
            int xNew = vertex.x();
            int zNew = vertex.z();
            if (xNew == bx && zNew == bz) {
                return true; // on a corner
            }
            int x1;
            int z1;
            int x2;
            int z2;
            if (xNew > xOld) {
                x1 = xOld;
                z1 = zOld;
                x2 = xNew;
                z2 = zNew;
            } else {
                x1 = xNew;
                z1 = zNew;
                x2 = xOld;
                z2 = zOld;
            }
            if (x1 <= bx && bx <= x2) {
                long cross = ((long) bz - z1) * (x2 - x1) - ((long) z2 - z1) * (bx - x1);
                if (cross == 0) {
                    if ((z1 <= bz) == (bz <= z2)) {
                        return true; // on an edge
                    }
                } else if (cross < 0 && x1 != bx) {
                    inside = !inside;
                }
            }
            xOld = xNew;
            zOld = zNew;
        }
        return inside;
    }
}
