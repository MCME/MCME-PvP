package com.mcmiddleearth.pvp.mode.loadout;

import java.util.List;

/** The fixed kits of the current modes (kits that players choose are not built yet). */
public final class Kits {

    /** 1.1.2's STANDARD kit: dyed unbreakable leather armor, iron sword, bow and 24 arrows. */
    public static final Kit STANDARD = new Kit("standard", List.of(
            new KitItem("head", "LEATHER_HELMET", 1, true, true),
            new KitItem("chest", "LEATHER_CHESTPLATE", 1, true, true),
            new KitItem("legs", "LEATHER_LEGGINGS", 1, true, true),
            new KitItem("feet", "LEATHER_BOOTS", 1, true, true),
            new KitItem("0", "IRON_SWORD", 1, true, false),
            new KitItem("1", "BOW", 1, true, false),
            new KitItem("8", "ARROW", 24, false, false)));

    private Kits() {
    }
}
