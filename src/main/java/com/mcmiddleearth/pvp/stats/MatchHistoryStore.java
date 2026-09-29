package com.mcmiddleearth.pvp.stats;

/**
 * The history of finished games, append-only. It cannot be rebuilt later, which is why every game is recorded from
 * the first 3.0 build on: web stats and ratings are meant to be built from it later.
 */
public interface MatchHistoryStore {

    /** Adds the record. It may return before it is written. */
    void append(MatchRecord record);
}
