package com.mcmiddleearth.pvp.team;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamLayoutTest {

    @Test
    void onlyNoneHasNoTeams() {
        assertFalse(TeamLayout.none().hasTeams());
        assertTrue(TeamLayout.fixed(2).hasTeams());
        assertTrue(TeamLayout.configurable(2, 4).hasTeams());
    }

    @Test
    void aFixedLayoutNeedsTwoToSixTeams() {
        assertThrows(IllegalArgumentException.class, () -> TeamLayout.fixed(1));
        assertThrows(IllegalArgumentException.class, () -> TeamLayout.fixed(7));
        assertDoesNotThrow(() -> TeamLayout.fixed(6));
    }

    @Test
    void aConfigurableRangeMustFitThePalette() {
        assertThrows(IllegalArgumentException.class, () -> TeamLayout.configurable(1, 4));
        assertThrows(IllegalArgumentException.class, () -> TeamLayout.configurable(3, 2));
        assertThrows(IllegalArgumentException.class, () -> TeamLayout.configurable(2, 7));
        assertDoesNotThrow(() -> TeamLayout.configurable(2, 2));
        assertDoesNotThrow(() -> TeamLayout.configurable(2, 6));
    }

    @Test
    void theLayoutsAreAClosedSetOfValues() {
        assertEquals("none", describe(TeamLayout.none()));
        assertEquals("3 teams", describe(TeamLayout.fixed(3)));
        assertEquals("2..4 teams", describe(TeamLayout.configurable(2, 4)));
        assertEquals(TeamLayout.fixed(3), new TeamLayout.Fixed(3));
    }

    /** Compiles without a default branch only because TeamLayout is sealed. */
    private static String describe(TeamLayout layout) {
        return switch (layout) {
            case TeamLayout.None none -> "none";
            case TeamLayout.Fixed fixed -> fixed.count() + " teams";
            case TeamLayout.Configurable range -> range.min() + ".." + range.max() + " teams";
        };
    }
}
