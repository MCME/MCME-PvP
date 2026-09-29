package com.mcmiddleearth.pvp.arena;

import com.mcmiddleearth.pvp.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMapImporterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), ZoneOffset.UTC);
    /** 3.0's two modes, matched as ModeRegistry.byLegacyName will: whitespace removed, case ignored. */
    private static final Function<String, Optional<String>> MODES = gmType -> Optional.ofNullable(
            Map.of("teamdeathmatch", "tdm", "freeforall", "ffa").get(gmType.replaceAll("\\s", "").toLowerCase(Locale.ROOT)));

    @TempDir
    Path maps;

    private final LegacyMapImporter importer = new LegacyMapImporter(MODES, CLOCK);

    @Test
    void importsATeamDeathmatchMap() throws IOException {
        Arena arena = importer.importFile(fixture("AS-TDM"));

        assertEquals(Arena.SCHEMA_VERSION, arena.schemaVersion());
        assertEquals("AS-TDM", arena.id());
        assertEquals("tdm", arena.modeId());
        assertNull(arena.unassignedReason());
        assertEquals("Amon Sul", arena.title());
        assertEquals(16, arena.maxPlayers());
        assertEquals("http://www.mcmiddleearth.com/content/Eriador.zip", arena.resourcePackUrl());
        assertEquals(new NamedPoint("world", 1050.5, 70, 2050.5, 0f, 0f), arena.spawn());
        assertEquals(new NamedPoint("world", 1010.5, 70, 2010.5, 0f, 0f), arena.points().get("RedSpawn"));
        assertEquals(Map.of("red", List.of(NamedPoint.fromLegacyFloor("world", 1010, 69, 2010)),
                "blue", List.of(NamedPoint.fromLegacyFloor("world", 1090, 69, 2090))), arena.teamSpawns());
        assertEquals(new Region("world", List.of(new Region.Vertex(1000, 2000), new Region.Vertex(1100, 2000),
                new Region.Vertex(1100, 2100), new Region.Vertex(1000, 2100)), 0, 1000), arena.region());
        assertEquals("AS-TDM", arena.imported().sourceFile());
        assertEquals("Team Deathmatch", arena.imported().legacyGmType());
        assertEquals("2026-09-24T12:00:00Z", arena.imported().importedAt());
        assertEquals(List.of(), arena.imported().unknownProperties(), "curr is a 1.1.2 key, just not used");
    }

    @Test
    void importsAFreeForAllMap() throws IOException {
        Arena arena = importer.importFile(fixture("AS-FFA"));

        assertEquals("ffa", arena.modeId());
        assertEquals(20, arena.maxPlayers());
        assertNull(arena.resourcePackUrl(), "a JSON null stays null");
        assertEquals(List.of("PlayerSpawn", "PlayerSpawn1", "PlayerSpawn2", "PlayerSpawn3"),
                List.copyOf(arena.points().keySet()));
        assertEquals(Map.of(), arena.teamSpawns());
        assertTrue(arena.region().overlaps(importer.importFile(fixture("AS-TDM")).region()),
                "like prod's AS-* maps, the two fixtures share one place");
    }

    @Test
    void keysOfOlderBuildsAreReportedSortedAndDoNotFailTheImport() throws IOException {
        Arena arena = importer.importFile(fixture("OLD-LOBBYSIGN"));

        assertEquals(List.of("fbt", "lobbySign"), arena.imported().unknownProperties());
        assertEquals("tdm", arena.modeId());
        assertNull(arena.region(), "an empty regionPoints list gives no region");
    }

    @Test
    void mapSpectatorSpawnStandsInForAMissingSpawn() throws IOException {
        Arena arena = importer.importFile(fixture("OLD-SPECTATOR"));

        assertEquals(NamedPoint.fromLegacyFeet("world", 210, 65, 190), arena.spawn());
        assertEquals(List.of("mapSpectatorSpawn"), arena.imported().unknownProperties());
        assertEquals("ffa", arena.modeId());
    }

    @Test
    void pointsInAnotherWorldKeepThatWorld() throws IOException {
        Arena arena = importer.importFile(write("NETHER-POINTS", "{\"spawn\":"
                + "{\"world\":\"arena_nether\",\"x\":0,\"y\":64,\"z\":0},\"importantPoints\":{\"Flag\":"
                + "{\"world\":\"arena_nether\",\"x\":1,\"y\":64,\"z\":1}}}"));

        assertEquals("arena_nether", arena.spawn().world());
        assertEquals("arena_nether", arena.points().get("Flag").world());
    }

    @Test
    void theHelmsDeepFixtureMapsItsNumberedTeamSpawns() throws IOException {
        Arena arena = importer.importFile(fixture("helmsdeep-1.1.2.json", "helmsdeep"));

        assertEquals("helmsdeep", arena.id());
        assertEquals("Helm's Deep", arena.title());
        assertEquals("tdm", arena.modeId());
        assertEquals(new NamedPoint("world", 100.5, 64, -199.5, 0f, 0f), arena.spawn());
        assertEquals(List.of(NamedPoint.fromLegacyFloor("world", 110, 65, -190)), arena.teamSpawns().get("red"));
        assertEquals(List.of(NamedPoint.fromLegacyFloor("world", 90, 66, -210)), arena.teamSpawns().get("blue"));
        assertEquals(3, arena.region().vertices().size());
    }

    @Test
    void aMapWithoutAGameModeIsUnassigned() throws IOException {
        Arena arena = importer.importFile(fixture("newmap-1.1.2.json", "newmap"));

        assertFalse(arena.playable());
        assertEquals("the map has no game mode", arena.unassignedReason());
        assertNull(arena.imported().legacyGmType());
        assertEquals("newmap", arena.title());
        assertEquals(0, arena.maxPlayers());
        assertNull(arena.resourcePackUrl());
        assertNull(arena.region());
        assertEquals(NamedPoint.fromLegacyFeet("world", 12, 70, -34), arena.spawn());
    }

    @Test
    void aBlankGameModeIsNoGameMode() throws IOException {
        Arena arena = importer.importFile(write("BLANK", "{\"gmType\":\"  \",\"spawn\":" + point(0) + "}"));

        assertEquals("the map has no game mode", arena.unassignedReason());
        assertEquals("  ", arena.imported().legacyGmType());
    }

    @Test
    void aGameModeWithoutA30ModeIsUnassignedWithItsName() throws IOException {
        Arena arena = importer.importFile(write("CTF", "{\"gmType\":\"Capture The Flag\",\"spawn\":" + point(0) + "}"));

        assertNull(arena.modeId());
        assertEquals("no 3.0 mode for \"Capture The Flag\" yet", arena.unassignedReason());
        assertEquals("Capture The Flag", arena.imported().legacyGmType());
    }

    @Test
    void aMappedMapWithoutASpawnIsUnassigned() throws IOException {
        Arena arena = importer.importFile(write("NO-SPAWN", "{\"gmType\":\"Team Deathmatch\"}"));

        assertNull(arena.modeId());
        assertNull(arena.spawn());
        assertEquals("the map has no spawn point", arena.unassignedReason());
    }

    @Test
    void missingValuesTakeTheirDefaults() throws IOException {
        assertEquals("Name", importer.importFile(write("A", "{\"title\":null,\"name\":\"Name\"}")).title());
        Arena empty = importer.importFile(write("B", "{\"title\":\"\"}"));

        assertEquals("B", empty.title(), "no title and no name: the id");
        assertEquals(0, empty.maxPlayers());
        assertNull(empty.resourcePackUrl());
        assertEquals(Map.of(), empty.points());
    }

    @Test
    void teamSpawnsGoByNumberWhenThereIsNoUnnumberedPoint() throws IOException {
        Arena arena = importer.importFile(write("SPAWNS", "{\"importantPoints\":{"
                + "\"RedSpawn2\":" + point(2) + ",\"RedSpawn10\":" + point(10) + ",\"RedSpawn1\":" + point(1)
                + ",\"BLUESPAWN3\":" + point(3) + ",\"GreenSpawn\":" + point(4)
                + ",\"PurpleSpawn\":" + point(5) + ",\"PlayerSpawn\":" + point(6) + ",\"RedSpawnX\":" + point(7) + "}}"));

        assertEquals(List.of(floor(1), floor(2), floor(10)), arena.teamSpawns().get("red"), "numeric order: 10 after 2");
        assertEquals(List.of(floor(3)), arena.teamSpawns().get("blue"), "case-insensitive team, one number");
        assertEquals(List.of(floor(4)), arena.teamSpawns().get("green"), "an unnumbered point alone is still that team's only spawn");
        assertEquals(Set.of("red", "blue", "green"), arena.teamSpawns().keySet(),
                "purple is not a team id; PlayerSpawn and RedSpawnX (trailing X) do not match");
        assertEquals(8, arena.points().size(), "every point is also kept under its own name");
    }

    @Test
    void theUnnumberedTeamSpawnAloneWinsOverNumberedOnes() throws IOException {
        Arena arena = importer.importFile(write("SPAWNS2", "{\"importantPoints\":{"
                + "\"RedSpawn\":" + point(0) + ",\"RedSpawn1\":" + point(1) + ",\"RedSpawn2\":" + point(2) + "}}"));

        assertEquals(List.of(floor(0)), arena.teamSpawns().get("red"),
                "1.1.2's Team Deathmatch used only RedSpawn/BlueSpawn; a map once used by another mode can still carry numbered ones");
        assertEquals(3, arena.points().size(), "every point is also kept under its own name, even the unused numbered ones");
    }

    @Test
    void whenTwoTeamSpawnsShareANumberTheFirstNameWins() throws IOException {
        Arena arena = importer.importFile(write("SPAWNS3", "{\"importantPoints\":{"
                + "\"RedSpawn01\":" + point(1) + ",\"RedSpawn1\":" + point(2) + "}}"));

        assertEquals(List.of(floor(1)), arena.teamSpawns().get("red"),
                "RedSpawn01 sorts before RedSpawn1, so it wins the shared number 1");
    }

    @Test
    void theRegionNeedsThreePointsAndTakesTheWorldOfTheFirst() throws IOException {
        assertNull(importer.importFile(write("TWO", "{\"regionPoints\":[" + point(0) + "," + point(1) + "]}")).region());

        Region region = importer.importFile(write("NETHER", "{\"regionPoints\":["
                + "{\"world\":\"arena_nether\",\"x\":0,\"y\":1,\"z\":0}," + point(10) + ",{\"x\":0,\"y\":1,\"z\":10}]}")).region();
        Region noWorld = importer.importFile(write("NO-WORLD",
                "{\"regionPoints\":[{\"x\":0,\"z\":0},{\"x\":1,\"z\":0},{\"x\":0,\"z\":1}]}")).region();

        assertEquals("arena_nether", region.world());
        assertEquals(List.of(new Region.Vertex(0, 0), new Region.Vertex(10, 0), new Region.Vertex(0, 10)), region.vertices());
        assertEquals(0, region.minY());
        assertEquals(1000, region.maxY());
        assertEquals("world", noWorld.world());
    }

    @Test
    void sha256IsTheLowercaseHexOfTheFileBytes() throws IOException {
        Arena arena = importer.importFile(write("HASHED", "{\"gmType\":\"Team Deathmatch\"}"));

        assertEquals("26ad51ea055e3def650dfd0a50a376dc661c2d87a41339167543382a516c4b41", arena.imported().sha256());
    }

    @Test
    void aFileWithoutAJsonObjectThrows() throws IOException {
        Path broken = write("BROKEN", "{\"gmType\":");
        Path array = write("ARRAY", "[1, 2]");
        Path empty = write("EMPTY", "");

        assertThrows(IOException.class, () -> importer.importFile(broken));
        assertThrows(IOException.class, () -> importer.importFile(array));
        assertThrows(IOException.class, () -> importer.importFile(empty));
    }

    @Test
    void legacyFilesSkipFoldersNewFilesAndHiddenFiles() throws IOException {
        fixture("AS-TDM");
        fixture("AS-FFA");
        fixture("OLD-SPECTATOR");
        fixture("AS-TDM", "backup/AS-TDM");
        fixture("AS-TDM", "AS-TDM.new");
        write(".DS_Store", "junk");
        write("desktop.ini", "junk");
        write("Thumbs.db", "junk");
        write("AS-TDM~", "junk");
        write("x.bak", "junk");
        write("y.tmp", "junk");
        write("z.orig", "junk");
        write("w.swp", "junk");
        write("AS-TDM - Copy", "junk");
        write("AS-TDM - kopie", "junk");

        List<String> names = LegacyMapImporter.legacyFiles(maps).stream()
                .map(file -> file.getFileName().toString())
                .toList();

        assertEquals(List.of("AS-FFA", "AS-TDM", "OLD-SPECTATOR"), names);
    }

    @Test
    void anAbsentMapsFolderHasNoFiles() throws IOException {
        assertEquals(List.of(), LegacyMapImporter.legacyFiles(maps.resolve("missing")));
    }

    @Test
    void importingNeverChangesTheMapsFolder() throws IOException {
        fixture("AS-TDM");
        fixture("OLD-LOBBYSIGN");
        fixture("AS-TDM", "backup/AS-TDM");
        fixture("AS-TDM", "AS-TDM.new");
        write("BROKEN", "{");
        Map<String, String> before = TestFiles.snapshot(maps);

        for (Path file : LegacyMapImporter.legacyFiles(maps)) {
            try {
                importer.importFile(file);
            } catch (IOException expected) {
                // BROKEN: a failed import must not write either
            }
        }

        assertEquals(before, TestFiles.snapshot(maps));
    }

    @ParameterizedTest
    @MethodSource("mapFixtures")
    void everyMapFixtureImportsAsPlayableOrWithAReason(Path file) throws IOException {
        Arena arena = importer.importFile(file);

        assertEquals(arena.playable(), arena.unassignedReason() == null, arena.id() + ": " + arena.unassignedReason());
    }

    /** Every map fixture; production map files dropped into fixtures/maps/ are picked up here. */
    static List<Path> mapFixtures() throws IOException, URISyntaxException {
        return LegacyMapImporter.legacyFiles(Path.of(LegacyMapImporterTest.class.getResource("/fixtures/maps").toURI()));
    }

    private Path fixture(String name) throws IOException {
        return fixture(name, name);
    }

    private Path fixture(String resource, String asName) throws IOException {
        return TestFiles.copyResource("/fixtures/maps/" + resource, maps.resolve(asName));
    }

    private Path write(String name, String json) throws IOException {
        return Files.writeString(maps.resolve(name), json);
    }

    /** A 1.1.2 point object at block (x, 64, 0). */
    private static String point(int x) {
        return "{\"world\":\"world\",\"x\":" + x + ",\"y\":64,\"z\":0}";
    }

    private static NamedPoint floor(int x) {
        return NamedPoint.fromLegacyFloor("world", x, 64, 0);
    }
}
