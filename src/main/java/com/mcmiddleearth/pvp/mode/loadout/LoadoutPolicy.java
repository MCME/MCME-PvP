package com.mcmiddleearth.pvp.mode.loadout;

import com.mcmiddleearth.pvp.game.PlayerSession;

/**
 * Which kit each player gets. Only a fixed policy exists; a policy where players pick a class can be added as another
 * subtype.
 */
public sealed interface LoadoutPolicy permits LoadoutPolicy.Fixed {

    static LoadoutPolicy fixed(Kit kit) {
        return new Fixed(kit);
    }

    Kit kitFor(PlayerSession player);

    /** Everyone gets the same kit. */
    record Fixed(Kit kit) implements LoadoutPolicy {
        @Override
        public Kit kitFor(PlayerSession player) {
            return kit;
        }
    }
}
