package com.mcmiddleearth.pvp.arena;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcmiddleearth.pvp.storage.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaStoreTest {

    /** The record components of Arena: nothing else may end up in the file. */
    private static final Set<String> RECORD_KEYS = Set.of("schemaVersion", "id", "modeId", "unassignedReason", "title",
            "maxPlayers", "resourcePackUrl", "spawn", "points", "teamSpawns", "region", "imported");

    @TempDir
    Path dir;

    private ArenaStore store() {
        return new ArenaStore(dir.resolve("arenas"), Json.pretty());
    }

    @Test
    void anImportedArenaRoundTrips() throws IOException {
        Arena arena = importedArena();

        store().save(arena);

        assertEquals(arena, store().read(store().fileFor("AS-TDM")));
    }

    @Test
    void anUnassignedArenaWithNullsRoundTrips() throws IOException {
        Arena arena = new Arena(Arena.SCHEMA_VERSION, "newmap", null, "the map has no game mode", "newmap", 0, null,
                null, Map.of(), Map.of(), null, null);

        store().save(arena);

        assertEquals(arena, store().read(store().fileFor("newmap")));
    }

    @Test
    void theFileHoldsExactlyTheTwelveRecordKeys() throws IOException {
        store().save(importedArena().unassigned("test"));

        JsonNode json = Json.pretty().readTree(Files.readAllBytes(store().fileFor("AS-TDM")));
        List<String> keys = new ArrayList<>();
        json.fieldNames().forEachRemaining(keys::add);

        assertEquals(RECORD_KEYS, Set.copyOf(keys));
        assertEquals(12, keys.size());
        assertTrue(json.get("modeId").isNull(), "nulls are written, so every file shows every key");
    }

    @Test
    void theFileIsPrettyPrinted() throws IOException {
        store().save(importedArena());

        List<String> lines = Files.readString(store().fileFor("AS-TDM")).lines().toList();

        assertTrue(lines.size() > 12, "one key per line at least");
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("  \"schemaVersion\"")), lines.toString());
    }

    @Test
    void fileForIsTheIdWithJsonExtension() {
        assertEquals(dir.resolve("arenas").resolve("AS-TDM.json"), store().fileFor("AS-TDM"));
    }

    @Test
    void fileForRejectsIdsThatLeaveTheArenasFolder() {
        List<String> invalidIds = List.of("../secret", "a/b", "a\\b", "..", "",
                dir.resolve("x").toAbsolutePath().toString());
        for (String id : invalidIds) {
            assertThrows(IllegalArgumentException.class, () -> store().fileFor(id), "should reject: " + id);
        }

        assertEquals(dir.resolve("arenas"), store().fileFor("AS-TDM").getParent());
        assertEquals(dir.resolve("arenas"), store().fileFor("helmsdeep-1.1.2.json").getParent());
    }

    @Test
    void saveCreatesTheFolderAndReplacesTheFile() throws IOException {
        Arena arena = importedArena();

        store().save(arena);
        store().save(arena.withMode("ffa"));

        assertEquals("ffa", store().read(store().fileFor("AS-TDM")).modeId());
        assertEquals(List.of(store().fileFor("AS-TDM")), store().files(), "no temporary file is left");
    }

    @Test
    void filesListsOnlyJsonFilesSortedByName() throws IOException {
        Path arenas = Files.createDirectories(dir.resolve("arenas"));
        Files.writeString(arenas.resolve("b.json"), "{}");
        Files.writeString(arenas.resolve("a.json"), "{}");
        Files.writeString(arenas.resolve("notes.txt"), "not an arena");
        Files.writeString(arenas.resolve("c.json.tmp"), "an interrupted save");
        Files.createDirectories(arenas.resolve("old.json"));

        assertEquals(List.of(arenas.resolve("a.json"), arenas.resolve("b.json")), store().files());
    }

    @Test
    void anAbsentFolderHasNoFiles() throws IOException {
        assertEquals(List.of(), store().files());
        assertFalse(Files.exists(dir.resolve("arenas")), "listing does not create the folder");
    }

    @Test
    void aBrokenFileThrowsIOException() throws IOException {
        Path arenas = Files.createDirectories(dir.resolve("arenas"));
        Path malformed = Files.writeString(arenas.resolve("broken.json"), "{not json");
        Path incomplete = Files.writeString(arenas.resolve("empty.json"), "{}");

        assertThrows(IOException.class, () -> store().read(malformed));
        assertThrows(IOException.class, () -> store().read(incomplete), "an arena needs its points and team spawns");
    }

    @Test
    void readingAMissingFileThrowsNoSuchFileException() {
        assertThrows(NoSuchFileException.class, () -> store().read(store().fileFor("GHOST")));
    }

    private static Arena importedArena() {
        NamedPoint red = NamedPoint.fromLegacyFloor("world", 1010, 69, 2010);
        NamedPoint blue = new NamedPoint("world", 1090.5, 70, 2090.5, 90f, -12.5f);
        Region region = new Region("world", List.of(new Region.Vertex(1000, 2000), new Region.Vertex(1100, 2000),
                new Region.Vertex(1100, 2100)), 0, 1000);
        ImportInfo imported = new ImportInfo("AS-TDM", "26ad51ea", "2026-09-24T12:00:00Z", "Team Deathmatch",
                List.of("fbt", "lobbySign"));
        return new Arena(Arena.SCHEMA_VERSION, "AS-TDM", "tdm", null, "Amon Sul", 16,
                "http://www.mcmiddleearth.com/content/Eriador.zip", NamedPoint.fromLegacyFeet("world", 1050, 70, 2050),
                Map.of("RedSpawn", red, "BlueSpawn", blue), Map.of("red", List.of(red), "blue", List.of(blue, red)),
                region, imported);
    }
}
