package com.mcmiddleearth.pvp.mode.loadout;

import java.util.List;

/** The items a player gets at the countdown, on a mid-game join and after a respawn. */
public record Kit(String id, List<KitItem> items) {
    public Kit {
        items = List.copyOf(items);
    }
}
