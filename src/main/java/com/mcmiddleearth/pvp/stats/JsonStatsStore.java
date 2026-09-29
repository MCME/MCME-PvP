package com.mcmiddleearth.pvp.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.pvp.storage.AsyncWriter;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stats as {@code players/<uuid>.json}. Reads are direct; saves go through the {@link AsyncWriter}. A file this
 * build must not overwrite — unreadable, broken and impossible to rename aside, or written by a newer build — is
 * left exactly as it is and its player becomes unsavable until the next start; for the first two, a later load
 * also skips reading it again. A broken but readable file (unparseable, or holding another player's uuid) is
 * instead renamed aside as {@code <uuid>.json.broken} (or the next free {@code .broken-N}, up to 20), so it too
 * is never overwritten.
 */
public final class JsonStatsStore implements StatsStore {

    private static final int MAX_BROKEN_ATTEMPTS = 20;

    private final Path playersDir;
    private final ObjectMapper mapper;
    private final AsyncWriter writer;
    private final Logger logger;
    /** Players whose file must be kept as it is: {@code save} skips and logs them. Lasts until the next start. */
    private final Set<UUID> unsavable = ConcurrentHashMap.newKeySet();
    /** Of those, the ones whose file stays exactly as it is for the rest of this run: {@code load} skips it too. */
    private final Set<UUID> silenced = ConcurrentHashMap.newKeySet();

    public JsonStatsStore(Path playersDir, ObjectMapper mapper, AsyncWriter writer, Logger logger) {
        this.playersDir = playersDir;
        this.mapper = mapper;
        this.writer = writer;
        this.logger = logger;
    }

    @Override
    public Optional<PlayerStats> load(UUID id) {
        if (silenced.contains(id)) {
            return Optional.empty();
        }
        Path file = fileFor(id);
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Stats file " + file + " could not be read; the stats of " + id
                    + " are not saved until the next start. Fix the file or its permissions, then restart.", e);
            unsavable.add(id);
            silenced.add(id);
            return Optional.empty();
        }
        PlayerStats stats;
        try {
            stats = mapper.readValue(bytes, PlayerStats.class);
            if (stats == null || !id.equals(stats.id())) {
                // A file holding just "null", or a copied or renamed file: using it would give this player no
                // stats, or someone else's.
                throw new IOException(
                        stats == null ? "the file holds no stats" : "the file holds the stats of " + stats.id());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Stats file " + file + " is broken, so it is treated as absent", e);
            setAside(id, file);
            return Optional.empty();
        }
        if (stats.schemaVersion() > PlayerStats.SCHEMA_VERSION) {
            // Saving would drop the fields this build doesn't know, which would lose them on a rollback.
            logger.warning("Stats file " + file + " was written by a newer MCME-PvP (schema " + stats.schemaVersion()
                    + "); the stats of " + id + " are shown but not saved, so its newer fields stay intact. Their "
                    + "games still go into matches/.");
            unsavable.add(id);
        }
        return Optional.of(stats);
    }

    @Override
    public void save(PlayerStats stats) {
        UUID id = stats.id();
        if (unsavable.contains(id)) {
            logger.warning("Stats of " + id + " are not saved: " + fileFor(id) + " must be kept as it is");
            return;
        }
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(stats);
        } catch (JsonProcessingException e) {
            // A record of numbers, strings and a uuid always serializes; failing here is a programming error.
            throw new IllegalStateException("the stats of " + id + " could not be serialized", e);
        }
        writer.write(fileFor(id), json);
    }

    /**
     * Renames {@code file} to {@code <uuid>.json.broken}, or the next free {@code .broken-N} up to 20, so that no
     * save can overwrite it while staff repair it; restoring it is stopping the server, fixing it and renaming it
     * back. When every name is taken, or the move fails for another reason, the file is left exactly where it is,
     * and {@code id} is added to both sets so that {@link #save} skips it and a later {@link #load} does too.
     */
    private void setAside(UUID id, Path file) {
        FileAlreadyExistsException allTaken = null;
        for (int attempt = 1; attempt <= MAX_BROKEN_ATTEMPTS; attempt++) {
            Path aside = file.resolveSibling(file.getFileName() + (attempt == 1 ? ".broken" : ".broken-" + attempt));
            try {
                Files.move(file, aside);
                logger.warning("Stats file " + file + " was renamed to " + aside
                        + " so no save overwrites it. To restore it, stop the server, fix it and rename it back; "
                        + "that replaces the new file, so add the games recorded since (they are in matches/).");
                return;
            } catch (FileAlreadyExistsException e) {
                allTaken = e;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Stats file " + file + " is broken and could not be set aside; the stats "
                        + "of " + id + " are not saved until the next start. Repair or move the file, then "
                        + "restart.", e);
                unsavable.add(id);
                silenced.add(id);
                return;
            }
        }
        logger.log(Level.SEVERE, "Stats file " + file + " is broken and could not be set aside; the stats of " + id
                + " are not saved until the next start. Repair or move the file, then restart.", allTaken);
        unsavable.add(id);
        silenced.add(id);
    }

    private Path fileFor(UUID id) {
        return playersDir.resolve(id + ".json");
    }
}
