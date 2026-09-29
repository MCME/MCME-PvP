package com.mcmiddleearth.pvp.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * The action bar is shared: the HUD redraws it every second while a game runs, and a short notice such as a supply
 * chest's "Restocked!" needs it for a few seconds. A notice holds the player's action bar, and the HUD skips held
 * players. Server thread only.
 */
public final class ActionBarHolds {

    private final Map<UUID, Integer> heldUntil = new HashMap<>();
    private final IntSupplier currentTick;

    /** @param currentTick the server's tick counter, {@code Server::getCurrentTick} */
    public ActionBarHolds(IntSupplier currentTick) {
        this.currentTick = currentTick;
    }

    /** Holds {@code player}'s action bar for {@code ticks} ticks from now. */
    public void hold(UUID player, int ticks) {
        heldUntil.put(player, currentTick.getAsInt() + ticks);
    }

    /** Ends any hold on {@code player}'s action bar right away, such as when a notice no longer applies. */
    public void release(UUID player) {
        heldUntil.remove(player);
    }

    /** Whether a notice still holds {@code player}'s action bar. An expired hold is forgotten. */
    public boolean held(UUID player) {
        Integer until = heldUntil.get(player);
        if (until == null) {
            return false;
        }
        if (currentTick.getAsInt() < until) {
            return true;
        }
        heldUntil.remove(player);
        return false;
    }
}
