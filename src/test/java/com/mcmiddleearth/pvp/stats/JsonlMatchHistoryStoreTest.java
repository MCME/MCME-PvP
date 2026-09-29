package com.mcmiddleearth.pvp.stats;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;

import static com.mcmiddleearth.pvp.stats.TestRecords.tdmGame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlMatchHistoryStoreTest {

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();

    @Test
    void aRecordBecomesOneLineInTheFileOfItsMonth() throws IOException {
        MatchRecord record = tdmGame("2026-09-24T12:00:00Z");

        store().append(record);

        List<String> lines = Files.readAllLines(dir.resolve("matches/2026-09.jsonl"));
        assertEquals(1, lines.size());
        assertEquals(record, Json.compact().readValue(lines.getFirst(), MatchRecord.class));
    }

    @Test
    void recordsOfOneMonthAreAppendedInOrder() throws IOException {
        MatchRecord first = tdmGame("2026-09-01T00:00:00Z");
        MatchRecord second = tdmGame("2026-09-30T23:59:59Z");
        JsonlMatchHistoryStore store = store();

        store.append(first);
        store.append(second);

        List<MatchRecord> read = new ArrayList<>();
        for (String line : Files.readAllLines(dir.resolve("matches/2026-09.jsonl"))) {
            read.add(Json.compact().readValue(line, MatchRecord.class));
        }
        assertEquals(List.of(first, second), read);
        assertTrue(Files.readString(dir.resolve("matches/2026-09.jsonl")).endsWith("}\n"));
    }

    @Test
    void theMonthIsTheOneOfEndedAtInUtc() throws IOException {
        TimeZone serverZone = TimeZone.getDefault();
        // 23:30 UTC on 30 September is already October in Amsterdam; the file must still be September's.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
        try {
            JsonlMatchHistoryStore store = store();
            store.append(tdmGame("2026-09-30T23:30:00Z"));
            store.append(tdmGame("2026-10-01T00:00:00Z"));
        } finally {
            TimeZone.setDefault(serverZone);
        }

        assertEquals(List.of("2026-09.jsonl", "2026-10.jsonl"), names(dir.resolve("matches")));
    }

    @Test
    void theLineHoldsEveryFieldOfTheRecord() throws IOException {
        store().append(tdmGame("2026-09-24T12:00:00Z"));

        JsonNode line = Json.compact().readTree(Files.readAllLines(dir.resolve("matches/2026-09.jsonl")).getFirst());

        List<String> keys = new ArrayList<>();
        line.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("schemaVersion", "gameId", "mode", "arena", "startedAt", "endedAt", "settings", "teams",
                "participants", "winner", "endReason"), Set.copyOf(keys));
        assertEquals(2, line.get("settings").get("teamCount").asInt());
        assertEquals("won", line.get("participants").get(0).get("result").asText());
        assertEquals("win_condition", line.get("endReason").asText());
    }

    @Test
    void aDrawIsWrittenWithANullWinner() throws IOException {
        MatchRecord won = tdmGame("2026-09-24T12:00:00Z");
        MatchRecord draw = new MatchRecord(won.schemaVersion(), won.gameId(), won.mode(), won.arena(), won.startedAt(),
                won.endedAt(), Map.of(), won.teams(), won.participants(), null, "stopped");

        store().append(draw);

        String line = Files.readAllLines(dir.resolve("matches/2026-09.jsonl")).getFirst();
        assertTrue(line.contains("\"winner\":null"), line);
        assertEquals(draw, Json.compact().readValue(line, MatchRecord.class));
    }

    @Test
    void aMalformedEndedAtFailsClearlyAndQueuesNothing() {
        MatchRecord broken = tdmGame("yesterday");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> store().append(broken));

        assertTrue(thrown.getMessage().contains(broken.gameId()), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("yesterday"), thrown.getMessage());
        assertFalse(Files.exists(dir.resolve("matches")), "nothing should be queued, so matches/ is never created");
    }

    @Test
    void appendingGoesThroughTheWriter() {
        List<Runnable> queued = new ArrayList<>();
        JsonlMatchHistoryStore store = new JsonlMatchHistoryStore(dir.resolve("matches"), Json.compact(),
                new AsyncWriter(queued::add, log.logger()));

        store.append(tdmGame("2026-09-24T12:00:00Z"));
        assertFalse(Files.exists(dir.resolve("matches/2026-09.jsonl")), "the write waits in the queue");
        queued.forEach(Runnable::run);

        assertTrue(Files.exists(dir.resolve("matches/2026-09.jsonl")));
    }

    /** A store that writes at once, so the tests can read the file right after an append. */
    private JsonlMatchHistoryStore store() {
        return new JsonlMatchHistoryStore(dir.resolve("matches"), Json.compact(), new AsyncWriter(Runnable::run, log.logger()));
    }

    private static List<String> names(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }
}
