package com.mcmiddleearth.pvp.integration;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.Objects;

/**
 * Section-sign color codes for the consumers that only understand legacy text: the PlaceholderAPI placeholders
 * (read by the chat plugin) and the proxy broadcast.
 */
public final class LegacyColors {

    private static final Map<NamedTextColor, Character> CODES = Map.ofEntries(
            Map.entry(NamedTextColor.BLACK, '0'),
            Map.entry(NamedTextColor.DARK_BLUE, '1'),
            Map.entry(NamedTextColor.DARK_GREEN, '2'),
            Map.entry(NamedTextColor.DARK_AQUA, '3'),
            Map.entry(NamedTextColor.DARK_RED, '4'),
            Map.entry(NamedTextColor.DARK_PURPLE, '5'),
            Map.entry(NamedTextColor.GOLD, '6'),
            Map.entry(NamedTextColor.GRAY, '7'),
            Map.entry(NamedTextColor.DARK_GRAY, '8'),
            Map.entry(NamedTextColor.BLUE, '9'),
            Map.entry(NamedTextColor.GREEN, 'a'),
            Map.entry(NamedTextColor.AQUA, 'b'),
            Map.entry(NamedTextColor.RED, 'c'),
            Map.entry(NamedTextColor.LIGHT_PURPLE, 'd'),
            Map.entry(NamedTextColor.YELLOW, 'e'),
            Map.entry(NamedTextColor.WHITE, 'f'));

    private LegacyColors() {
    }

    /** The code of a named color, e.g. RED -> "§c". */
    public static String code(NamedTextColor color) {
        Objects.requireNonNull(color, "color");
        return "§" + CODES.get(color);
    }
}
