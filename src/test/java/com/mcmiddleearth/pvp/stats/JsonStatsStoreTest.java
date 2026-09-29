package com.mcmiddleearth.pvp.stats;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.storage.AsyncWriter;
import com.mcmiddleearth.pvp.storage.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

import static com.mcmiddleearth.pvp.stats.TestRecords.ALICE;
import static com.mcmiddleearth.pvp.stats.TestRecords.BOB;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStatsStoreTest {

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();

    @Test
    void savedStatsLoadBack() {
        PlayerStats alice = new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 7, 3, 11, 5, 4, 2,
                List.of("Legolas", "Gimli"));

        store().save(alice);

        assertEquals(Optional.of(alice), store().load(ALICE));
    }

    @Test
    void theFileIsPlayersUuidJsonPrettyPrinted() throws IOException {
        store().save(PlayerStats.empty(ALICE, "Alice"));

        String json = Files.readString(file(ALICE));

        assertTrue(json.lines().count() > 1, json);
        assertTrue(json.matches("(?s).*\"name\"\\s*:\\s*\"Alice\".*"), json);
    }

    @Test
    void aPlayerWithoutAFileHasNoStats() {
        assertEquals(Optional.empty(), store().load(ALICE));
    }

    @Test
    void aBrokenFileIsLoggedAndTreatedAsAbsent() throws IOException {
        write(ALICE, "{\"kills\":");

        assertEquals(Optional.empty(), store().load(ALICE));

        // One warning that it is broken, one naming the file it was set aside to (see below).
        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.getFirst().contains(file(ALICE).toString()), warnings.getFirst());
    }

    @Test
    void theFileOfAnotherPlayerIsTreatedAsBroken() throws IOException {
        store().save(PlayerStats.empty(ALICE, "Alice"));
        Files.copy(file(ALICE), file(BOB));

        assertEquals(Optional.empty(), store().load(BOB));
        assertEquals(2, log.messages(Level.WARNING).size());
    }

    @Test
    void aBrokenFileIsSetAside() throws IOException {
        write(ALICE, "{\"kills\":");

        assertEquals(Optional.empty(), store().load(ALICE));

        assertFalse(Files.exists(file(ALICE)), "the broken file is renamed away");
        assertEquals("{\"kills\":", Files.readString(brokenFile(ALICE, 1)));
        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(1).contains(brokenFile(ALICE, 1).getFileName().toString()), warnings.get(1));
        assertTrue(warnings.get(1).contains("matches/"), warnings.get(1));

        store().save(PlayerStats.empty(ALICE, "Alice"));

        assertTrue(Files.exists(file(ALICE)), "the save writes a fresh file");
        assertEquals("{\"kills\":", Files.readString(brokenFile(ALICE, 1)), "the .broken file is unchanged");
    }

    @Test
    void theNextFreeNameIsUsed() throws IOException {
        write(ALICE, "{\"kills\":");
        Files.writeString(brokenFile(ALICE, 1), "already set aside");

        assertEquals(Optional.empty(), store().load(ALICE));

        assertEquals("already set aside", Files.readString(brokenFile(ALICE, 1)), "the existing .broken file is untouched");
        assertEquals("{\"kills\":", Files.readString(brokenFile(ALICE, 2)));
    }

    @Test
    void aFileOfAnotherPlayerIsSetAsideToo() throws IOException {
        store().save(PlayerStats.empty(BOB, "Bob"));
        Files.copy(file(BOB), file(ALICE));

        assertEquals(Optional.empty(), store().load(ALICE));

        assertFalse(Files.exists(file(ALICE)));
        assertTrue(Files.exists(brokenFile(ALICE, 1)));
    }

    @Test
    void aFileHoldingNullIsSetAside() throws IOException {
        write(ALICE, "null");

        assertEquals(Optional.empty(), store().load(ALICE));

        assertFalse(Files.exists(file(ALICE)));
        assertEquals("null", Files.readString(brokenFile(ALICE, 1)));
    }

    @Test
    void whenItCannotBeSetAsideThePlayerIsNotSaved() throws IOException {
        write(ALICE, "{\"kills\":");
        for (int attempt = 1; attempt <= 20; attempt++) {
            Files.writeString(brokenFile(ALICE, attempt), "taken");
        }
        byte[] before = Files.readAllBytes(file(ALICE));
        JsonStatsStore store = store();

        assertEquals(Optional.empty(), store.load(ALICE));

        assertEquals(1, log.messages(Level.SEVERE).size(), log.messages(Level.SEVERE).toString());
        assertArrayEquals(before, Files.readAllBytes(file(ALICE)), "the broken file is still in place, unchanged");

        store.save(PlayerStats.empty(ALICE, "Alice"));

        assertArrayEquals(before, Files.readAllBytes(file(ALICE)), "still unchanged after the save attempt");
        assertTrue(log.messages(Level.WARNING).stream().anyMatch(message -> message.contains("are not saved")),
                log.messages(Level.WARNING).toString());

        int severeBefore = log.messages(Level.SEVERE).size();
        int warningsBefore = log.messages(Level.WARNING).size();

        assertEquals(Optional.empty(), store.load(ALICE));

        assertEquals(severeBefore, log.messages(Level.SEVERE).size(), "no new SEVERE from the repeat load");
        assertEquals(warningsBefore, log.messages(Level.WARNING).size(), "no new WARNING from the repeat load");
    }

    @Test
    void aDirectoryAtTheFileNameIsUnreadable() throws IOException {
        Files.createDirectories(file(ALICE));
        JsonStatsStore store = store();

        assertEquals(Optional.empty(), store.load(ALICE));

        assertTrue(Files.isDirectory(file(ALICE)), "the directory is still there");
        try (Stream<Path> siblings = Files.list(dir.resolve("players"))) {
            assertEquals(List.of(file(ALICE)), siblings.toList(), "no .broken* file appears");
        }
        assertEquals(1, log.messages(Level.SEVERE).size(), log.messages(Level.SEVERE).toString());

        store.save(PlayerStats.empty(ALICE, "Alice"));

        assertTrue(Files.isDirectory(file(ALICE)), "the save writes nothing");
        assertTrue(log.messages(Level.WARNING).stream().anyMatch(message -> message.contains("must be kept as it is")),
                log.messages(Level.WARNING).toString());

        int severeBefore = log.messages(Level.SEVERE).size();
        int warningsBefore = log.messages(Level.WARNING).size();

        assertEquals(Optional.empty(), store.load(ALICE));

        assertEquals(severeBefore, log.messages(Level.SEVERE).size(), "no new SEVERE from the repeat load");
        assertEquals(warningsBefore, log.messages(Level.WARNING).size(), "no new WARNING from the repeat load");
    }

    @Test
    void aNewerFileIsShownButNotSaved() throws IOException {
        write(ALICE, "{\"schemaVersion\":2,\"id\":\"" + ALICE + "\",\"name\":\"Alice\",\"kills\":9,\"deaths\":1,"
                + "\"gamesPlayed\":10,\"gamesWon\":9,\"gamesLost\":1,\"gamesSpectated\":0,\"playersKilled\":[],"
                + "\"flagsCaptured\":5}");
        byte[] before = Files.readAllBytes(file(ALICE));
        JsonStatsStore store = store();

        PlayerStats loaded = store.load(ALICE).orElseThrow();

        assertEquals(9, loaded.kills());
        assertEquals(1, loaded.deaths());
        assertEquals(10, loaded.gamesPlayed());
        assertEquals(1, log.messages(Level.WARNING).size(), log.messages(Level.WARNING).toString());
        assertTrue(log.messages(Level.WARNING).getFirst().contains("newer MCME-PvP (schema 2)"),
                log.messages(Level.WARNING).getFirst());

        store.save(loaded);

        assertArrayEquals(before, Files.readAllBytes(file(ALICE)), "the file is unchanged");
        assertTrue(log.messages(Level.WARNING).stream().anyMatch(message -> message.contains("must be kept as it is")),
                log.messages(Level.WARNING).toString());
    }

    @Test
    void unknownKeysAreIgnoredSoOlderBuildsReadNewerFiles() throws IOException {
        write(ALICE, "{\"schemaVersion\":1,\"id\":\"" + ALICE + "\",\"name\":\"Alice\",\"kills\":4,\"deaths\":0,"
                + "\"gamesPlayed\":1,\"gamesWon\":1,\"gamesLost\":0,\"gamesSpectated\":0,\"playersKilled\":[],"
                + "\"rating\":1500}");

        assertEquals(4, store().load(ALICE).orElseThrow().kills());
    }

    @Test
    void savingGoesThroughTheWriter() {
        List<Runnable> queued = new ArrayList<>();
        JsonStatsStore store = new JsonStatsStore(dir.resolve("players"), Json.pretty(),
                new AsyncWriter(queued::add, log.logger()), log.logger());

        store.save(PlayerStats.empty(ALICE, "Alice"));
        assertFalse(Files.exists(file(ALICE)), "the write waits in the queue");
        queued.forEach(Runnable::run);

        assertTrue(Files.exists(file(ALICE)));
    }

    /** A store that writes at once, so the tests can read the file right after a save. */
    private JsonStatsStore store() {
        return new JsonStatsStore(dir.resolve("players"), Json.pretty(), new AsyncWriter(Runnable::run, log.logger()),
                log.logger());
    }

    private Path file(UUID id) {
        return dir.resolve("players").resolve(id + ".json");
    }

    /** Where the {@code attempt}-th set-aside name lands: {@code .broken} for 1, else {@code .broken-<attempt>}. */
    private Path brokenFile(UUID id, int attempt) {
        return dir.resolve("players").resolve(id + ".json" + (attempt == 1 ? ".broken" : ".broken-" + attempt));
    }

    private void write(UUID id, String json) throws IOException {
        Files.createDirectories(dir.resolve("players"));
        Files.writeString(file(id), json);
    }
}
