package com.mcmiddleearth.pvp.team;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A color a player wears in a game: a real team (red, blue, …) or, in modes without teams, a personal color.
 * {@code armorRgb} dyes the leather kit. Players with the same non-personal spec cannot hurt each other.
 */
public record TeamSpec(String id, String name, NamedTextColor color, int armorRgb, boolean personal) {
}
