package com.mcmiddleearth.pvp.arena;

import com.mcmiddleearth.pvp.arena.Region.Bounds;
import com.mcmiddleearth.pvp.arena.Region.Vertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTest {

    /** A 10x10 square with y 0..1000, 1.1.2's height range. */
    private static final Region SQUARE = region("world", 0, 0, 10, 0, 10, 10, 0, 10);
    /** An L: the square minus its upper-left quarter (x 0..5, z 5..10). */
    private static final Region L_SHAPE = region("world", 0, 0, 10, 0, 10, 10, 5, 10, 5, 5, 0, 5);
    private static final Region TRIANGLE = region("world", 0, 0, 10, 0, 0, 10);

    @Test
    void cornersAreInside() {
        assertTrue(SQUARE.contains("world", 0, 64, 0));
        assertTrue(SQUARE.contains("world", 10.9, 64, 10.9), "block (10, 10) is the far corner");
        assertTrue(L_SHAPE.contains("world", 5, 64, 5), "the inner corner of the L");
    }

    @Test
    void edgesAreInside() {
        assertTrue(SQUARE.contains("world", 5, 64, 0));
        assertTrue(SQUARE.contains("world", 10, 64, 5));
        assertTrue(L_SHAPE.contains("world", 3, 64, 5), "the inner edge of the L");
        assertTrue(L_SHAPE.contains("world", 5, 64, 7), "the other inner edge of the L");
        assertTrue(TRIANGLE.contains("world", 5, 64, 5), "the diagonal edge");
    }

    @Test
    void theInsideIsInside() {
        assertTrue(SQUARE.contains("world", 5.5, 64, 5.5));
        assertTrue(L_SHAPE.contains("world", 2, 64, 2));
        assertTrue(L_SHAPE.contains("world", 7, 64, 8));
        assertTrue(TRIANGLE.contains("world", 4, 64, 4));
    }

    @Test
    void theOutsideIsOutside() {
        assertFalse(SQUARE.contains("world", 11, 64, 5));
        assertFalse(SQUARE.contains("world", -0.01, 64, 5), "x -0.01 is in block -1");
        assertFalse(SQUARE.contains("world", 5, 64, 11));
        assertFalse(SQUARE.contains("world", 5, 64, -1));
        assertFalse(TRIANGLE.contains("world", 6, 64, 6));
    }

    @Test
    void theNotchOfAConcavePolygonIsOutside() {
        assertFalse(L_SHAPE.contains("world", 2, 64, 8));
        assertFalse(L_SHAPE.contains("world", 4, 64, 7));
    }

    @Test
    void heightsOutsideTheRangeAreOutside() {
        assertTrue(SQUARE.contains("world", 5, 0, 5));
        assertTrue(SQUARE.contains("world", 5, 1000.99, 5), "y 1000.99 is in block 1000");
        assertFalse(SQUARE.contains("world", 5, 1001, 5));
        assertFalse(SQUARE.contains("world", 5, -0.01, 5), "y -0.01 is in block -1");
    }

    @Test
    void anotherWorldIsOutside() {
        assertFalse(SQUARE.contains("world_nether", 5, 64, 5));
    }

    @Test
    void nonFiniteCoordinatesAreOutside() {
        assertTrue(SQUARE.contains("world", 0, 0, 0), "block (0, 0, 0) is inside SQUARE: the precondition for below");

        assertFalse(SQUARE.contains("world", Double.NaN, 0, 0), "NaN must not floor to block 0");
        assertFalse(SQUARE.contains("world", 0, Double.NaN, 0), "NaN must not floor to block 0");
        assertFalse(SQUARE.contains("world", 0, 0, Double.NaN), "NaN must not floor to block 0");
        assertFalse(SQUARE.contains("world", Double.POSITIVE_INFINITY, 0, 0));
        assertFalse(SQUARE.contains("world", 0, Double.POSITIVE_INFINITY, 0));
        assertFalse(SQUARE.contains("world", 0, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    void boundsSpanEveryVertex() {
        assertEquals(new Bounds(0, 0, 10, 10), SQUARE.bounds());
        assertEquals(new Bounds(0, 0, 10, 10), L_SHAPE.bounds());
        assertEquals(new Bounds(-20, -5, -10, 3), region("world", -20, -5, -10, -5, -15, 3).bounds());
    }

    @Test
    void boundsIntersectWhenTheyShareAColumn() {
        Bounds box = new Bounds(0, 0, 10, 10);

        assertTrue(box.intersects(new Bounds(10, 0, 20, 10)), "touching edges share column x 10");
        assertTrue(new Bounds(10, 0, 20, 10).intersects(box));
        assertTrue(box.intersects(new Bounds(2, 2, 3, 3)), "one inside the other");
        assertFalse(box.intersects(new Bounds(11, 0, 20, 10)));
        assertFalse(box.intersects(new Bounds(0, 11, 10, 20)));
    }

    @Test
    void regionsOverlapInTheSameWorldOnly() {
        assertTrue(SQUARE.overlaps(region("world", 10, 0, 20, 0, 20, 10)));
        assertFalse(SQUARE.overlaps(region("world", 11, 0, 20, 0, 20, 10)));
        assertFalse(SQUARE.overlaps(region("world_nether", 0, 0, 10, 0, 10, 10)));
    }

    @Test
    void aRegionNeedsThreeVerticesAndAnOrderedHeightRange() {
        List<Vertex> two = List.of(new Vertex(0, 0), new Vertex(1, 1));
        List<Vertex> three = List.of(new Vertex(0, 0), new Vertex(1, 0), new Vertex(0, 1));

        assertThrows(IllegalArgumentException.class, () -> new Region("world", two, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new Region("world", three, 5, 4));
        assertDoesNotThrow(() -> new Region("world", three, 64, 64));
    }

    @Test
    void theVerticesAreAnImmutableCopy() {
        List<Vertex> vertices = new ArrayList<>(List.of(new Vertex(0, 0), new Vertex(1, 0), new Vertex(0, 1)));
        Region region = new Region("world", vertices, 0, 1000);

        vertices.add(new Vertex(5, 5));

        assertEquals(3, region.vertices().size());
        assertThrows(UnsupportedOperationException.class, () -> region.vertices().add(new Vertex(9, 9)));
    }

    /** A region in {@code world} with y 0..1000 through the (x, z) pairs of {@code xz}. */
    private static Region region(String world, int... xz) {
        List<Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < xz.length; i += 2) {
            vertices.add(new Vertex(xz[i], xz[i + 1]));
        }
        return new Region(world, vertices, 0, 1000);
    }
}
