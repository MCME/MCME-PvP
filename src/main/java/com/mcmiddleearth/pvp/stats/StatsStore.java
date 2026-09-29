package com.mcmiddleearth.pvp.stats;

import java.util.Optional;
import java.util.UUID;

/** Where lifetime stats live. JSON files come first; a database can take over later without touching game code. */
public interface StatsStore {

    /**
     * The stored stats; empty when there are none. A broken record is logged and treated as absent, and no later
     * save may overwrite it.
     */
    Optional<PlayerStats> load(UUID id);

    /**
     * Stores the stats. It may return before they are written, and may instead skip (and log) a player whose
     * stored record must be kept.
     */
    void save(PlayerStats stats);
}
