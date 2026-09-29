package com.mcmiddleearth.pvp.team;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteTest {

    @Test
    void gamesUseTheTeamsInPaletteOrder() {
        assertEquals(List.of(Palette.RED, Palette.BLUE, Palette.GREEN), Palette.teams(3));
        assertEquals(List.of(), Palette.teams(0));
        assertEquals(Palette.TEAMS, Palette.teams(6));
    }

    @Test
    void aTeamCountOutsideThePaletteIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Palette.teams(-1));
        assertThrows(IllegalArgumentException.class, () -> Palette.teams(7));
    }

    @Test
    void redAndBlueKeepTheDyeColorsOf112() {
        assertEquals(0xB02E26, Palette.RED.armorRgb());
        assertEquals(0x3C44AA, Palette.BLUE.armorRgb());
        assertEquals(NamedTextColor.RED, Palette.RED.color());
        assertEquals(NamedTextColor.BLUE, Palette.BLUE.color());
    }

    @Test
    void theOtherTeamDyesAreFixedToo() {
        assertEquals(0x5E7C16, Palette.GREEN.armorRgb());
        assertEquals(0xFED83D, Palette.YELLOW.armorRgb());
        assertEquals(0x3AB3DA, Palette.AQUA.armorRgb());
        assertEquals(0xF9801D, Palette.GOLD.armorRgb());
    }

    @Test
    void personalColorsFollowTheFreeForAllRotationOf112() {
        List<NamedTextColor> rotation = List.of(NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED, NamedTextColor.GOLD,
                NamedTextColor.GREEN, NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.YELLOW);

        assertEquals(rotation, Palette.PERSONAL.stream().map(TeamSpec::color).toList());
        assertTrue(Palette.PERSONAL.stream().allMatch(TeamSpec::personal));
        assertFalse(Palette.TEAMS.stream().anyMatch(TeamSpec::personal));
    }

    @Test
    void personalColorsHaveTheirDyeColors() {
        // p-dark-red and p-gold deliberately differ from 1.1.2, which dyed them #B02E26 (same as red) and
        // #9D9D97 (light gray), so players can tell those two apart.
        List<Integer> armorRgb = List.of(0x3AB3DA, 0x3C44AA, 0x169C9C, 0x5E7C16, 0x8932B8, 0x8E1B1B, 0xF9801D,
                0x80C71F, 0xC74EBD, 0xB02E26, 0xFED83D);

        assertEquals(armorRgb, Palette.PERSONAL.stream().map(TeamSpec::armorRgb).toList());
    }

    @Test
    void idsAreUniqueAcrossTeamsAndPersonalColors() {
        Set<String> ids = new HashSet<>();

        Stream.concat(Palette.TEAMS.stream(), Palette.PERSONAL.stream())
                .forEach(spec -> assertTrue(ids.add(spec.id()), "duplicate id " + spec.id()));
    }

    @Test
    void byIdFindsTeamsAndPersonalColors() {
        assertEquals(Optional.of(Palette.RED), Palette.byId("red"));
        assertEquals(Optional.of(Palette.PERSONAL.get(6)), Palette.byId("p-gold"));
        assertEquals(Optional.empty(), Palette.byId("purple"));
        assertEquals(Optional.empty(), Palette.byId("RED"));
    }

    @Test
    void theListsCannotBeChanged() {
        assertThrows(UnsupportedOperationException.class, () -> Palette.TEAMS.add(Palette.RED));
        assertThrows(UnsupportedOperationException.class, () -> Palette.PERSONAL.clear());
    }
}
