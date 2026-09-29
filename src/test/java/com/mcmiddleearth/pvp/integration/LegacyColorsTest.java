package com.mcmiddleearth.pvp.integration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorsTest {

    @Test
    void teamColorsGet112sCodes() {
        assertEquals("§c", LegacyColors.code(NamedTextColor.RED));
        assertEquals("§9", LegacyColors.code(NamedTextColor.BLUE));
        assertEquals("§a", LegacyColors.code(NamedTextColor.GREEN));
        assertEquals("§e", LegacyColors.code(NamedTextColor.YELLOW));
        assertEquals("§b", LegacyColors.code(NamedTextColor.AQUA));
        assertEquals("§6", LegacyColors.code(NamedTextColor.GOLD));
        assertEquals("§7", LegacyColors.code(NamedTextColor.GRAY));
    }

    @Test
    void everyNamedColorMatchesAdventuresLegacySerializer() {
        for (NamedTextColor color : NamedTextColor.NAMES.values()) {
            String legacy = LegacyComponentSerializer.legacySection().serialize(Component.text("x", color));
            assertEquals(legacy.substring(0, 2), LegacyColors.code(color), color.toString());
        }
    }
}
