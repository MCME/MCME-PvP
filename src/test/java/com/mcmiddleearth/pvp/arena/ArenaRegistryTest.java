package com.mcmiddleearth.pvp.arena;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.TestFiles;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRegistryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path dir;

    /** The modes of "this version"; a test adds one to act out an update that brings a new mode. */
    private final Map<String, String> modes = new HashMap<>(Map.of("teamdeathmatch", "tdm", "freeforall", "ffa"));
    private final Function<String, Optional<String>> modeFor =
            gmType -> Optional.ofNullable(modes.get(gmType.replaceAll("\\s", "").toLowerCase(Locale.ROOT)));
    private final RecordingLog log = new RecordingLog();

    @Test
    void theFirstLoadImportsAndSavesEveryLegacyMap() throws IOException {
        map("AS-TDM");
        map("AS-FFA");
        map("OLD-SPECTATOR");
        ArenaRegistry registry = registry();

        LoadReport report = registry.load();

        assertEquals(List.of("AS-FFA", "AS-TDM", "OLD-SPECTATOR"), report.imported());
        assertEquals(3, report.arenas());
        assertEquals(Map.of(), report.failed());
        Arena tdm = registry.get("AS-TDM").orElseThrow();
        assertEquals("tdm", tdm.modeId());
        assertEquals(tdm, store().read(store().fileFor("AS-TDM")), "saved as imported");
    }

    @Test
    void aSecondLoadKeepsTheSavedArenaAndImportsNothing() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();
        store().save(registry.get("AS-TDM").orElseThrow().withMode("ffa"));

        LoadReport report = registry.load();

        assertEquals(List.of(), report.imported());
        assertEquals("ffa", registry.get("AS-TDM").orElseThrow().modeId(), "the edit is not overwritten");
    }

    @Test
    void unassignedArenasGetTheirModeOnceItExists() throws IOException {
        modes.clear();
        map("AS-TDM");
        ArenaRegistry registry = registry();
        assertEquals(Map.of("AS-TDM", "no 3.0 mode for \"Team Deathmatch\" yet"), registry.load().unassigned());

        modes.put("teamdeathmatch", "tdm");
        LoadReport report = registry.load();

        assertEquals(List.of(), report.imported(), "mapped again, not imported again");
        assertEquals(Map.of(), report.unassigned());
        assertEquals("tdm", registry.get("AS-TDM").orElseThrow().modeId());
        assertEquals("tdm", store().read(store().fileFor("AS-TDM")).modeId(), "the new mode is saved");
        assertEquals(1, log.messages(Level.INFO).size());
    }

    @Test
    void anArenaWithoutASpawnStaysUnassignedWhenItsModeAppears() throws IOException {
        modes.clear();
        write("NO-SPAWN", "{\"gmType\":\"Team Deathmatch\"}");
        ArenaRegistry registry = registry();
        registry.load();

        modes.put("teamdeathmatch", "tdm");
        LoadReport report = registry.load();

        assertEquals(Map.of("NO-SPAWN", "the map has no spawn point"), report.unassigned());
        assertFalse(registry.get("NO-SPAWN").orElseThrow().playable());
    }

    @Test
    void mapAgainNeverOverridesAHandSetReason() throws IOException {
        modes.clear();
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();
        store().save(registry.get("AS-TDM").orElseThrow().unassigned("taken out of rotation"));

        modes.put("teamdeathmatch", "tdm");
        LoadReport report = registry.load();

        assertEquals(Map.of("AS-TDM", "taken out of rotation"), report.unassigned());
        assertEquals("taken out of rotation", registry.get("AS-TDM").orElseThrow().unassignedReason());
    }

    @Test
    void reimportReadsTheLegacyMapAgain() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();
        store().save(registry.get("AS-TDM").orElseThrow().withMode("ffa"));
        registry.load();

        Arena reimported = registry.reimport("AS-TDM");

        assertEquals("tdm", reimported.modeId());
        assertEquals(reimported, registry.get("AS-TDM").orElseThrow());
        assertEquals(reimported, store().read(store().fileFor("AS-TDM")));
    }

    @Test
    void reimportFindsTheMapWhateverTheCase() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();

        assertEquals("AS-TDM", registry.reimport("as-tdm").id());
        assertEquals(1, registry.all().size(), "no second arena under the other spelling");
    }

    @Test
    void reimportOfAMissingMapIsAnUnknownArena() {
        ArenaRegistry registry = registry();
        registry.load();

        PvpException e = assertThrows(PvpException.class, () -> registry.reimport("NOPE"));

        assertEquals(Msg.ERROR_UNKNOWN_ARENA, e.messageKey());
        assertEquals(Map.of("arena", "NOPE"), e.args());
    }

    @Test
    void reimportOfABrokenMapThrowsUnchecked() throws IOException {
        write("BROKEN", "{");
        ArenaRegistry registry = registry();
        registry.load();

        assertThrows(UncheckedIOException.class, () -> registry.reimport("BROKEN"));
    }

    @Test
    void aBrokenArenaFileIsReportedAndSkipped() throws IOException {
        map("AS-TDM");
        Files.createDirectories(dir.resolve("arenas"));
        Files.writeString(dir.resolve("arenas/BROKEN.json"), "{not json");
        ArenaRegistry registry = registry();

        LoadReport report = registry.load();

        assertEquals(Set.of("BROKEN.json"), report.failed().keySet());
        assertEquals(1, report.arenas());
        assertTrue(registry.get("AS-TDM").isPresent());
    }

    @Test
    void aBrokenLegacyMapIsReportedAndNotSaved() throws IOException {
        write("BROKEN", "{");

        LoadReport report = registry().load();

        assertEquals(Set.of("maps/BROKEN"), report.failed().keySet());
        assertFalse(Files.exists(store().fileFor("BROKEN")));
    }

    @Test
    void aBrokenArenaFileIsNeverOverwrittenByAFreshImport() throws IOException {
        map("AS-TDM");
        Files.createDirectories(dir.resolve("arenas"));
        Files.writeString(dir.resolve("arenas/AS-TDM.json"), "{not json");
        Map<String, String> before = TestFiles.snapshot(dir.resolve("arenas"));
        ArenaRegistry registry = registry();

        LoadReport report = registry.load();

        assertEquals(Set.of("AS-TDM.json"), report.failed().keySet());
        assertEquals(List.of(), report.imported());
        assertEquals(Optional.empty(), registry.get("AS-TDM"));
        assertEquals(before, TestFiles.snapshot(dir.resolve("arenas")), "the broken file is left exactly as it was");
    }

    @Test
    void aFileMustHoldTheArenaItsNameSays() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();
        Files.copy(store().fileFor("AS-TDM"), dir.resolve("arenas").resolve("AS-TDM-night.json"));

        LoadReport report = registry.load();

        assertEquals("the file holds arena \"AS-TDM\"; name it AS-TDM.json or change its id",
                report.failed().get("AS-TDM-night.json"));
        assertEquals("AS-TDM", registry.get("AS-TDM").orElseThrow().id(), "AS-TDM still loads from its own file");
    }

    @Test
    void aStoredIdThatCannotBeAFileNameSaysSo() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();
        Arena original = store().read(store().fileFor("AS-TDM"));
        Arena withBadId = new Arena(original.schemaVersion(), "a/b", original.modeId(), original.unassignedReason(),
                original.title(), original.maxPlayers(), original.resourcePackUrl(), original.spawn(), original.points(),
                original.teamSpawns(), original.region(), original.imported());
        Files.write(dir.resolve("arenas").resolve("AB.json"), Json.pretty().writeValueAsBytes(withBadId));

        LoadReport report = registry.load();

        String message = report.failed().get("AB.json");
        assertTrue(message.contains("is not a valid file name"), message);
        assertFalse(message.contains("name it"), message);
    }

    @Test
    void aTypeMismatchKeepsTheFieldNameAndLocation() throws IOException {
        Files.createDirectories(dir.resolve("arenas"));
        Files.writeString(dir.resolve("arenas/BROKEN.json"),
                "{\"schemaVersion\":1,\"id\":\"BROKEN\",\"maxPlayers\":\"ten\",\"points\":{},\"teamSpawns\":{}}");

        LoadReport report = registry().load();

        String message = report.failed().get("BROKEN.json");
        assertTrue(message.contains("maxPlayers"), message);
        assertTrue(message.contains("line"), message);
    }

    @Test
    void aMissingPointsMapIsReportedClearly() throws IOException {
        Files.createDirectories(dir.resolve("arenas"));
        Files.writeString(dir.resolve("arenas/BROKEN.json"),
                "{\"schemaVersion\":1,\"id\":\"BROKEN\",\"title\":\"Broken\",\"maxPlayers\":0,\"teamSpawns\":{}}");

        LoadReport report = registry().load();

        assertTrue(report.failed().get("BROKEN.json").contains("\"points\" is missing"), report.failed().toString());
    }

    @Test
    void getMatchesTheIdIgnoringCase() throws IOException {
        map("AS-TDM");
        ArenaRegistry registry = registry();
        registry.load();

        assertEquals("AS-TDM", registry.get("AS-TDM").orElseThrow().id());
        assertEquals("AS-TDM", registry.get("as-tdm").orElseThrow().id());
        assertEquals(Optional.empty(), registry.get("AS-TD"));
    }

    @Test
    void allIsSortedIgnoringCaseAndPlayableSkipsUnassignedArenas() throws IOException {
        map("AS-TDM", "Zulu-tdm");
        map("AS-FFA", "alpha-ffa");
        map("newmap-1.1.2.json", "middle");
        ArenaRegistry registry = registry();
        registry.load();

        assertEquals(List.of("alpha-ffa", "middle", "Zulu-tdm"), registry.all().stream().map(Arena::id).toList());
        assertEquals(List.of("alpha-ffa", "Zulu-tdm"), registry.playable().stream().map(Arena::id).toList());
    }

    @Test
    void theReportListsUnassignedArenasAndUnknownKeys() throws IOException {
        map("OLD-LOBBYSIGN");
        map("newmap-1.1.2.json", "newmap");
        map("AS-TDM");

        LoadReport report = registry().load();

        assertEquals(Map.of("newmap", "the map has no game mode"), report.unassigned());
        assertEquals(Map.of("OLD-LOBBYSIGN", List.of("fbt", "lobbySign")), report.unknownProperties());
        assertEquals("3 arenas (3 imported now), 1 unassigned, 0 failed", report.summary());
    }

    @Test
    void loadReplacesTheArenasInMemory() throws IOException {
        map("AS-TDM");
        map("AS-FFA");
        ArenaRegistry registry = registry();
        registry.load();
        Files.delete(dir.resolve("maps/AS-FFA"));
        Files.delete(store().fileFor("AS-FFA"));

        registry.load();

        assertEquals(Optional.empty(), registry.get("AS-FFA"));
        assertEquals(List.of("AS-TDM"), registry.all().stream().map(Arena::id).toList());
    }

    @Test
    void withoutFoldersThereAreNoArenas() {
        ArenaRegistry registry = registry();

        LoadReport report = registry.load();

        assertEquals(0, report.arenas());
        assertEquals(Map.of(), report.failed());
        assertEquals(List.of(), registry.all());
    }

    @Test
    void theMapsFolderIsNeverWritten() throws IOException {
        map("AS-TDM");
        map("OLD-LOBBYSIGN");
        map("AS-TDM", "backup/AS-TDM");
        map("AS-TDM", "AS-TDM.new");
        write("BROKEN", "{");
        Map<String, String> before = TestFiles.snapshot(dir.resolve("maps"));
        ArenaRegistry registry = registry();

        registry.load();
        registry.load();
        registry.reimport("AS-TDM");

        assertEquals(before, TestFiles.snapshot(dir.resolve("maps")));
    }

    private ArenaStore store() {
        return new ArenaStore(dir.resolve("arenas"), Json.pretty());
    }

    private ArenaRegistry registry() {
        return new ArenaRegistry(store(), new LegacyMapImporter(modeFor, CLOCK), dir.resolve("maps"), modeFor, log.logger());
    }

    private void map(String fixture) throws IOException {
        map(fixture, fixture);
    }

    private void map(String fixture, String asName) throws IOException {
        TestFiles.copyResource("/fixtures/maps/" + fixture, dir.resolve("maps").resolve(asName));
    }

    private void write(String name, String json) throws IOException {
        Files.createDirectories(dir.resolve("maps"));
        Files.writeString(dir.resolve("maps").resolve(name), json);
    }
}
