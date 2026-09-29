package com.mcmiddleearth.pvp.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lifetime stats. A player's 1.1.2 stats are imported the first time {@link #get} needs them and saved in the new
 * format at once; after that the legacy file is never read again. What {@link #find} learns from 1.1.2's files is
 * remembered too, until the player's stats are saved. Every record loaded stays in memory for the plugin's
 * lifetime: the files are small, and 1.1.2 did the same per online player. Used on the server thread only.
 */
public final class StatsService {

    private final StatsStore store;
    private final LegacyStatsImporter legacy;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    /**
     * What {@link #find} learned from 1.1.2's files, including that there are none; forgotten once the player's
     * stats are saved.
     */
    private final Map<UUID, Optional<PlayerStats>> legacyLookups = new HashMap<>();

    public StatsService(StatsStore store, LegacyStatsImporter legacy) {
        this.store = store;
        this.legacy = legacy;
    }

    /**
     * The player's stats: remembered, else stored, else imported from 1.1.2 (and saved at once), else new and
     * empty. An empty record is neither saved nor remembered; it is saved once the player has a game to count.
     */
    public PlayerStats get(UUID id, String name) {
        Optional<PlayerStats> known = remembered(id);
        if (known.isPresent()) {
            return known.get();
        }
        Optional<PlayerStats> imported = legacy.read(id, name);
        if (imported.isPresent()) {
            update(imported.get());
            return imported.get();
        }
        return PlayerStats.empty(id, name);
    }

    /**
     * The player's stats when they have any: remembered, stored, or in 1.1.2's files; never an empty record.
     * 1.1.2 files hold no name, so stats found only there carry the uuid as their name; they are not saved here,
     * {@link #get} imports them for good under the player's name.
     */
    public Optional<PlayerStats> find(UUID id) {
        Optional<PlayerStats> known = remembered(id);
        if (known.isPresent()) {
            return known;
        }
        return legacyLookups.computeIfAbsent(id, key -> legacy.read(key, key.toString()));
    }

    /**
     * Adds the finished game to the stats of every participant and saves them, each on its own: one failing
     * participant doesn't cost the others, and the first failure is rethrown once every participant was tried.
     */
    public void recordMatch(MatchRecord record) {
        RuntimeException failure = null;
        for (MatchRecord.Participant participant : record.participants()) {
            try {
                UUID id = UUID.fromString(participant.uuid());
                update(get(id, participant.name()).plus(participant));
            } catch (RuntimeException e) {
                failure = firstOf(failure, e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Adds one game watched as a spectator and saves the stats. */
    public void recordSpectated(UUID id, String name) {
        update(get(id, name).plusSpectated(name));
    }

    /** The cached record, else the stored one (which is then cached). */
    private Optional<PlayerStats> remembered(UUID id) {
        PlayerStats cached = cache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<PlayerStats> stored = store.load(id);
        stored.ifPresent(stats -> cache.put(id, stats));
        return stored;
    }

    private void update(PlayerStats stats) {
        cache.put(stats.id(), stats);
        // Once the id is cached, remembered() answers from the cache first; this only keeps
        // legacyLookups from growing.
        legacyLookups.remove(stats.id());
        store.save(stats);
    }

    private static RuntimeException firstOf(RuntimeException first, RuntimeException next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }
}
