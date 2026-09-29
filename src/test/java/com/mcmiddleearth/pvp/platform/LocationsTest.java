package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationsTest {

    private WorldMock arena;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        arena = server.addSimpleWorld("arena");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aPointInALoadedWorldBecomesALocationWithItsFacing() {
        Location location = Locations.location(new NamedPoint("arena", 1.5, 64, -2.5, 90f, -10f)).orElseThrow();

        assertSame(arena, location.getWorld());
        assertEquals(1.5, location.getX());
        assertEquals(64, location.getY());
        assertEquals(-2.5, location.getZ());
        assertEquals(90f, location.getYaw());
        assertEquals(-10f, location.getPitch());
    }

    @Test
    void aPointInAWorldThatIsNotLoadedHasNoLocation() {
        assertTrue(Locations.location(NamedPoint.of("nether", 0, 64, 0)).isEmpty());
    }

    @Test
    void aLocationBecomesAPointAndBack() {
        NamedPoint point = new NamedPoint("arena", 10.25, 70, 3.75, -45f, 12.5f);

        assertEquals(point, Locations.point(Locations.location(point).orElseThrow()));
    }

    @Test
    void aBlockBecomesItsCornerWithoutFacing() {
        assertEquals(NamedPoint.of("arena", 3, 70, -4), Locations.blockPoint(arena.getBlockAt(3, 70, -4)));
    }
}
