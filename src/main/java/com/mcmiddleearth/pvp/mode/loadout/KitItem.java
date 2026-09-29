package com.mcmiddleearth.pvp.mode.loadout;

/**
 * One item of a kit. {@code slot} is "head", "chest", "legs", "feet", "offhand" or an inventory index "0".."35";
 * {@code material} is a Bukkit Material name; {@code dyed} items (leather armor) take the player's color.
 */
public record KitItem(String slot, String material, int amount, boolean unbreakable, boolean dyed) {
}
