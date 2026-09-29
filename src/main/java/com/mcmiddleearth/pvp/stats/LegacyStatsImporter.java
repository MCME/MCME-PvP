package com.mcmiddleearth.pvp.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads a 1.1.2 stats file ({@code stats/<uuid>}: JSON, no extension) the first time a player is seen under 3.0.
 * It only reads: 3.0 never writes into {@code stats/} (1.1.2 kept a {@code backup/} there), so going back to 2.x
 * finds the files as they were. The file is read as a JSON tree, so unknown keys are simply ignored.
 */
public final class LegacyStatsImporter {

    private final Path legacyStatsDir;
    private final ObjectMapper mapper;
    private final Logger logger;

    public LegacyStatsImporter(Path legacyStatsDir, ObjectMapper mapper, Logger logger) {
        this.legacyStatsDir = legacyStatsDir;
        this.mapper = mapper;
        this.logger = logger;
    }

    /**
     * The player's 1.1.2 stats under {@code name} (1.1.2 files hold no name); absent counters are 0. Empty when
     * there is no file, or when it cannot be read (logged at WARNING).
     */
    public Optional<PlayerStats> read(UUID id, String name) {
        Path file = legacyStatsDir.resolve(id.toString());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonNode root = mapper.readTree(Files.readAllBytes(file));
            if (root == null || !root.isObject()) {
                throw new IOException("the file does not hold a JSON object");
            }
            List<String> playersKilled = new ArrayList<>();
            JsonNode victims = root.path("playersKilled");
            if (victims.isArray()) {
                for (JsonNode victim : victims) {
                    if (victim.isTextual()) {
                        playersKilled.add(victim.asText());
                    }
                }
            }
            return Optional.of(new PlayerStats(PlayerStats.SCHEMA_VERSION, id, name,
                    count(root, "kills"), count(root, "deaths"), count(root, "gamesPlayed"),
                    count(root, "gamesWon"), count(root, "gamesLost"), count(root, "gamesSpectated"), playersKilled));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Legacy stats file " + file + " could not be read, so it is not imported", e);
            return Optional.empty();
        }
    }

    /** A counter; a missing one, a number too big for an int and a negative one read as 0 (1.1.2 wrote ints ≥ 0). */
    private static int count(JsonNode root, String key) {
        JsonNode node = root.path(key);
        int value = node.isNumber() ? (node.canConvertToInt() ? node.intValue() : 0) : node.asInt(0);
        return Math.max(0, value);
    }
}
