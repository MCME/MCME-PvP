package com.mcmiddleearth.pvp.mode.rules;

/** What happens after a player dies while the game runs. */
public enum Respawn {
    /** Out for the rest of the game: they spectate. */
    NEVER,
    /** Back at the next spawn point of their team (or of everyone), with a fresh kit. */
    AT_SPAWN
}
