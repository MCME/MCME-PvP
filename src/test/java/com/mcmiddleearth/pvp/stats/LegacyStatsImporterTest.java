package com.mcmiddleearth.pvp.stats;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.TestFiles;
import com.mcmiddleearth.pvp.storage.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static com.mcmiddleearth.pvp.stats.TestRecords.ALICE;
import static com.mcmiddleearth.pvp.stats.TestRecords.BOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real 1.1.2 stat files hold player UUIDs and names and are never committed; the broken and unusual files these
 * tests need are written at runtime.
 */
class LegacyStatsImporterTest {

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();

    @Test
    void readsA112StatsFileUnderTheCurrentName() throws IOException {
        TestFiles.copyResource("/fixtures/stats/sample-1.1.2.json", statsFile(ALICE));

        PlayerStats stats = importer().read(ALICE, "Alice").orElseThrow();

        assertEquals(new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 7, 3, 11, 5, 4, 2,
                List.of("Legolas", "Gimli", "Boromir")), stats);
    }

    @Test
    void aPlayerWithoutAFileHasNoLegacyStats() {
        assertEquals(Optional.empty(), importer().read(ALICE, "Alice"));
    }

    @Test
    void missingCountersAreZeroAndUnknownKeysAreIgnored() throws IOException {
        write(ALICE, "{\"kills\":2,\"addedByAFork\":true,\"playersKilled\":{\"not\":\"a list\"}}");

        PlayerStats stats = importer().read(ALICE, "Alice").orElseThrow();

        assertEquals(new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 2, 0, 0, 0, 0, 0, List.of()), stats);
    }

    @Test
    void countersOutOfRangeReadAsZero() throws IOException {
        write(ALICE, "{\"kills\":4294967297,\"deaths\":-5,\"gamesPlayed\":\"5\"}");

        PlayerStats stats = importer().read(ALICE, "Alice").orElseThrow();

        assertEquals(0, stats.kills());
        assertEquals(0, stats.deaths());
        assertEquals(5, stats.gamesPlayed());
    }

    @Test
    void onlyNamesAreKeptFromPlayersKilled() throws IOException {
        write(ALICE, "{\"playersKilled\":[\"Legolas\",5,null]}");

        PlayerStats stats = importer().read(ALICE, "Alice").orElseThrow();

        assertEquals(List.of("Legolas"), stats.playersKilled());
    }

    @Test
    void aBrokenFileIsLoggedAndReadAsAbsent() throws IOException {
        write(ALICE, "{\"kills\":");

        assertEquals(Optional.empty(), importer().read(ALICE, "Alice"));

        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.getFirst().contains(ALICE.toString()), warnings.getFirst());
    }

    @Test
    void anEmptyFileIsLoggedAndReadAsAbsent() throws IOException {
        write(ALICE, "");

        assertEquals(Optional.empty(), importer().read(ALICE, "Alice"));

        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.getFirst().contains(ALICE.toString()), warnings.getFirst());
    }

    @Test
    void readingNeverWritesIntoTheStatsFolder() throws IOException {
        TestFiles.copyResource("/fixtures/stats/sample-1.1.2.json", statsFile(ALICE));
        write(BOB, "{\"kills\":");
        Map<String, String> before = TestFiles.snapshot(dir.resolve("stats"));

        importer().read(ALICE, "Alice");
        importer().read(BOB, "Bob");
        importer().read(UUID.randomUUID(), "Cara");

        assertEquals(before, TestFiles.snapshot(dir.resolve("stats")), "no backup/ folder, no new or changed files");
    }

    private LegacyStatsImporter importer() {
        return new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
    }

    /** 1.1.2's file name: the dashed uuid, without extension. */
    private Path statsFile(UUID id) {
        return dir.resolve("stats").resolve(id.toString());
    }

    private void write(UUID id, String json) throws IOException {
        Files.createDirectories(dir.resolve("stats"));
        Files.writeString(statsFile(id), json);
    }
}
