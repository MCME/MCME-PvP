package com.mcmiddleearth.pvp.stats;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.TestFiles;
import com.mcmiddleearth.pvp.storage.AsyncWriter;
import com.mcmiddleearth.pvp.storage.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static com.mcmiddleearth.pvp.stats.TestRecords.ALICE;
import static com.mcmiddleearth.pvp.stats.TestRecords.BOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsServiceTest {

    private static final String ENDED_AT = "2026-09-24T12:00:00Z";

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();
    private final MemoryStore store = new MemoryStore();

    @Test
    void storedStatsAreLoadedOnceAndRemembered() {
        PlayerStats alice = new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 5, 2, 3, 1, 1, 0, List.of());
        store.saved.put(ALICE, alice);
        StatsService stats = service();

        assertEquals(alice, stats.get(ALICE, "Alice"));
        assertEquals(alice, stats.get(ALICE, "Alice"));
        assertEquals(Optional.of(alice), stats.find(ALICE));
        assertEquals(1, store.loads);
    }

    @Test
    void legacyStatsAreImportedAndSavedAtOnce() throws IOException {
        legacyFile(BOB);

        PlayerStats bob = service().get(BOB, "Bob");

        assertEquals(7, bob.kills());
        assertEquals("Bob", bob.name());
        assertEquals(bob, store.saved.get(BOB));
    }

    @Test
    void theStoreWinsOverTheLegacyFile() throws IOException {
        legacyFile(BOB);
        PlayerStats stored = PlayerStats.empty(BOB, "Bob").plusSpectated("Bob");
        store.saved.put(BOB, stored);

        assertEquals(stored, service().get(BOB, "Bob"));
    }

    @Test
    void aNewPlayerGetsEmptyStatsThatAreNotSaved() {
        assertEquals(PlayerStats.empty(ALICE, "Alice"), service().get(ALICE, "Alice"));
        assertEquals(Map.of(), store.saved);
    }

    @Test
    void anEmptyRecordIsNotRemembered() {
        StatsService stats = service();

        stats.get(ALICE, "Alice");

        assertEquals(Optional.empty(), stats.find(ALICE));
    }

    @Test
    void findNeverReturnsAnEmptyRecord() {
        assertEquals(Optional.empty(), service().find(ALICE));
    }

    @Test
    void findReadsLegacyStatsWithoutSavingThem() throws IOException {
        legacyFile(BOB);

        PlayerStats found = service().find(BOB).orElseThrow();

        assertEquals(7, found.kills());
        assertEquals(BOB.toString(), found.name(), "1.1.2 files hold no name");
        assertEquals(Map.of(), store.saved);
    }

    @Test
    void statsFoundOnlyIn112AreReadOnce() throws IOException {
        legacyFile(BOB);
        StatsService stats = service();

        PlayerStats first = stats.find(BOB).orElseThrow();
        Files.delete(dir.resolve("stats").resolve(BOB.toString()));
        PlayerStats second = stats.find(BOB).orElseThrow();

        assertEquals(7, first.kills());
        assertEquals(7, second.kills());
    }

    @Test
    void aBroken112FileIsLoggedOnceNotOnEveryLookup() throws IOException {
        Files.createDirectories(dir.resolve("stats"));
        Files.writeString(dir.resolve("stats").resolve(BOB.toString()), "{");
        StatsService stats = service();

        assertEquals(Optional.empty(), stats.find(BOB));
        assertEquals(Optional.empty(), stats.find(BOB));

        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void aPlayerWithoutStatsIsFoundOnceTheyHaveAny() {
        UUID carol = UUID.randomUUID();
        StatsService stats = service();

        assertEquals(Optional.empty(), stats.find(carol));

        stats.recordSpectated(carol, "Carol");

        PlayerStats found = stats.find(carol).orElseThrow();
        assertEquals(1, found.gamesSpectated());
    }

    @Test
    void recordMatchAddsTheGameToEveryParticipant() {
        StatsService stats = service();

        stats.recordMatch(TestRecords.tdmGame(ENDED_AT));

        PlayerStats alice = new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 3, 1, 1, 1, 0, 0, List.of());
        PlayerStats bob = new PlayerStats(PlayerStats.SCHEMA_VERSION, BOB, "Bob", 1, 3, 1, 0, 1, 0, List.of());
        assertEquals(alice, store.saved.get(ALICE));
        assertEquals(bob, store.saved.get(BOB));
        assertEquals(alice, stats.get(ALICE, "Alice"), "remembered after recording");
    }

    @Test
    void recordMatchBuildsOnImportedLegacyStats() throws IOException {
        legacyFile(ALICE);

        service().recordMatch(TestRecords.tdmGame(ENDED_AT));

        PlayerStats alice = store.saved.get(ALICE);
        assertEquals(10, alice.kills());
        assertEquals(4, alice.deaths());
        assertEquals(12, alice.gamesPlayed());
        assertEquals(6, alice.gamesWon());
        assertEquals(List.of("Legolas", "Gimli", "Boromir"), alice.playersKilled());
    }

    @Test
    void aFailingParticipantDoesNotCostTheOthersAndRecordMatchThrows() {
        SingleFailureStore store = new SingleFailureStore(ALICE);
        StatsService stats = new StatsService(store, legacy());

        assertThrows(RuntimeException.class, () -> stats.recordMatch(TestRecords.tdmGame(ENDED_AT)));

        PlayerStats bob = new PlayerStats(PlayerStats.SCHEMA_VERSION, BOB, "Bob", 1, 3, 1, 0, 1, 0, List.of());
        assertEquals(bob, store.saved.get(BOB));
        assertNull(store.saved.get(ALICE));
    }

    @Test
    void recordSpectatedAddsOneWatchedGame() {
        StatsService stats = service();

        stats.recordSpectated(ALICE, "Alice");
        stats.recordSpectated(ALICE, "Alice");

        assertEquals(2, store.saved.get(ALICE).gamesSpectated());
        assertEquals(0, store.saved.get(ALICE).gamesPlayed());
    }

    @Test
    void theJsonStoreKeepsTheTotalsForTheNextStart() {
        AsyncWriter writer = new AsyncWriter(Runnable::run, log.logger());
        new StatsService(new JsonStatsStore(dir.resolve("players"), Json.pretty(), writer, log.logger()), legacy())
                .recordMatch(TestRecords.tdmGame(ENDED_AT));

        StatsService afterRestart = new StatsService(
                new JsonStatsStore(dir.resolve("players"), Json.pretty(), writer, log.logger()), legacy());

        assertEquals(3, afterRestart.get(ALICE, "Alice").kills());
        assertEquals(1, afterRestart.get(BOB, "Bob").gamesLost());
    }

    @Test
    void aBrokenStatsFileIsSetAsideAndReplacedByTheLegacyImport() throws IOException {
        Path playersDir = dir.resolve("players");
        Files.createDirectories(playersDir);
        Path file = playersDir.resolve(ALICE + ".json");
        Files.writeString(file, "{\"kills\":");
        legacyFile(ALICE);
        AsyncWriter writer = new AsyncWriter(Runnable::run, log.logger());
        StatsService stats = new StatsService(
                new JsonStatsStore(playersDir, Json.pretty(), writer, log.logger()), legacy());

        PlayerStats alice = stats.get(ALICE, "Alice");

        assertEquals(7, alice.kills());
        assertEquals("Alice", alice.name());
        assertEquals(alice, Json.pretty().readValue(Files.readAllBytes(file), PlayerStats.class));
        assertEquals("{\"kills\":", Files.readString(playersDir.resolve(ALICE + ".json.broken")));
    }

    private StatsService service() {
        return new StatsService(store, legacy());
    }

    private LegacyStatsImporter legacy() {
        return new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
    }

    /** The synthetic 1.1.2 fixture (7 kills, 3 deaths, 11 played, 5 won) as this player's legacy file. */
    private void legacyFile(UUID id) throws IOException {
        TestFiles.copyResource("/fixtures/stats/sample-1.1.2.json", dir.resolve("stats").resolve(id.toString()));
    }

    /** A StatsStore in memory that counts its loads. */
    private static final class MemoryStore implements StatsStore {

        private final Map<UUID, PlayerStats> saved = new HashMap<>();
        private int loads;

        @Override
        public Optional<PlayerStats> load(UUID id) {
            loads++;
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public void save(PlayerStats stats) {
            saved.put(stats.id(), stats);
        }
    }

    /** A StatsStore that fails to save one uuid, to prove the other participants aren't lost with it. */
    private static final class SingleFailureStore implements StatsStore {

        private final UUID failing;
        private final Map<UUID, PlayerStats> saved = new HashMap<>();

        private SingleFailureStore(UUID failing) {
            this.failing = failing;
        }

        @Override
        public Optional<PlayerStats> load(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public void save(PlayerStats stats) {
            if (stats.id().equals(failing)) {
                throw new IllegalStateException("disk is full");
            }
            saved.put(stats.id(), stats);
        }
    }
}
