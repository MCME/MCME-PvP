package com.mcmiddleearth.pvp.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedPointTest {

    @Test
    void ofHasNoFacing() {
        assertEquals(new NamedPoint("world", 1.5, 2, -3, 0f, 0f), NamedPoint.of("world", 1.5, 2, -3));
    }

    @Test
    void aLegacySpawnStandsInTheCentreOfItsBlock() {
        // 1.1.2 stored the block of the admin's feet: block -200 spans z -200..-199, so its centre is -199.5.
        assertEquals(new NamedPoint("world", 100.5, 64, -199.5, 0f, 0f), NamedPoint.fromLegacyFeet("world", 100, 64, -200));
    }

    @Test
    void aLegacyImportantPointStandsOnTopOfItsBlock() {
        // 1.1.2 stored important points one block below the admin's feet and added 1 when using them.
        assertEquals(new NamedPoint("world", 110.5, 66, -189.5, 0f, 0f), NamedPoint.fromLegacyFloor("world", 110, 65, -190));
    }
}
