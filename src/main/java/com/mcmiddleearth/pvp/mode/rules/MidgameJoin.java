package com.mcmiddleearth.pvp.mode.rules;

/** What happens to players who join after the countdown began. */
public enum MidgameJoin {
    /** They play at once. */
    JOIN,
    /** They join the team with the fewest alive players, while no team has fewer alive players than it started with; after that they spectate (1.1.2's Team Deathmatch). */
    SMALLEST_TEAM_WHILE_FULL,
    /** They spectate. */
    SPECTATE
}
