package com.mcmiddleearth.pvp.platform;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seam a notice uses to keep the HUD off a player's action bar for a few seconds. */
class ActionBarHoldsTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private int tick;
    private final ActionBarHolds holds = new ActionBarHolds(() -> tick);

    @Test
    void aPlayerStaysHeldUntilTheirTicksPass() {
        holds.hold(PLAYER, 60);
        tick += 59;

        assertTrue(holds.held(PLAYER));
    }

    @Test
    void theHoldEndsExactlyOnItsExpiryTick() {
        holds.hold(PLAYER, 60);
        tick += 60;

        assertFalse(holds.held(PLAYER));
    }

    @Test
    void holdingAgainReplacesTheEarlierExpiry() {
        holds.hold(PLAYER, 60);
        tick += 40;
        holds.hold(PLAYER, 60);
        tick += 40;

        assertTrue(holds.held(PLAYER), "the second hold pushed the expiry 40 ticks further out");
    }

    @Test
    void aPlayerNeverHeldIsNotHeld() {
        assertFalse(holds.held(UUID.randomUUID()));
    }

    @Test
    void releasingEndsTheHoldRightAway() {
        holds.hold(PLAYER, 60);

        holds.release(PLAYER);

        assertFalse(holds.held(PLAYER));
    }

    @Test
    void releasingAPlayerNeverHeldDoesNothing() {
        holds.release(UUID.randomUUID());

        assertFalse(holds.held(PLAYER));
    }
}
