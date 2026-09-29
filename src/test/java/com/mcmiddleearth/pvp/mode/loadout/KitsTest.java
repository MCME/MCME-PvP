package com.mcmiddleearth.pvp.mode.loadout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class KitsTest {

    @Test
    void theStandardKitIsTheOneOf112() {
        assertEquals(List.of(
                new KitItem("head", "LEATHER_HELMET", 1, true, true),
                new KitItem("chest", "LEATHER_CHESTPLATE", 1, true, true),
                new KitItem("legs", "LEATHER_LEGGINGS", 1, true, true),
                new KitItem("feet", "LEATHER_BOOTS", 1, true, true),
                new KitItem("0", "IRON_SWORD", 1, true, false),
                new KitItem("1", "BOW", 1, true, false),
                new KitItem("8", "ARROW", 24, false, false)), Kits.STANDARD.items());
        assertEquals("standard", Kits.STANDARD.id());
    }

    @Test
    void aKitKeepsItsOwnCopyOfTheItems() {
        List<KitItem> items = new ArrayList<>(List.of(new KitItem("0", "STICK", 1, false, false)));
        Kit kit = new Kit("sticks", items);
        items.clear();

        assertEquals(1, kit.items().size());
    }

    @Test
    void aFixedLoadoutGivesEveryoneTheSameKit() {
        LoadoutPolicy policy = LoadoutPolicy.fixed(Kits.STANDARD);

        // Fixed ignores the session, so no game is needed here.
        assertSame(Kits.STANDARD, policy.kitFor(null));
    }
}
