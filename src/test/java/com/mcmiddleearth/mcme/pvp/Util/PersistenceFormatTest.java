package com.mcmiddleearth.mcme.pvp.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Map and stat files on the servers are JSON written by DBmanager with 1.1.2's Jackson 2.17.1.
 * These tests pin that format through the calls the plugin itself makes: each fixture is read with
 * readValue(File), written back with the byte writer saveObj uses, and must give the same JSON
 * tree. Trees are compared, not bytes: property order is not stable, not even within one Jackson
 * version.
 *
 * The fixtures are real 1.1.2 output, generated with the production jar. Production map files can
 * be copied into src/test/resources/fixtures/maps/ as they are (the plugin saves them without an
 * extension) and are picked up automatically. Check a new one on the commit before a Jackson change
 * first: if it no longer loads there, it was already unloadable in 1.1.2. Production stat files are
 * NOT committed, because they hold player UUIDs and names.
 */
class PersistenceFormatTest {

    private static final ObjectMapper MAPPER = DBmanager.getJSonParser();

    static Stream<Path> mapFixtures() throws IOException, URISyntaxException {
        return fixtures("/fixtures/maps");
    }

    static Stream<Path> statFixtures() throws IOException, URISyntaxException {
        return fixtures("/fixtures/stats");
    }

    /** Every regular file in the folder, whatever its name: production files have no extension. */
    private static Stream<Path> fixtures(String dir) throws IOException, URISyntaxException {
        Path root = Path.of(PersistenceFormatTest.class.getResource(dir).toURI());
        try (Stream<Path> files = Files.list(root)) {
            return files.filter(Files::isRegularFile).sorted().toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("mapFixtures")
    void mapFileRoundTripsUnchanged(Path file) throws IOException {
        assertRoundTrip(file, Map.class);
    }

    @ParameterizedTest
    @MethodSource("statFixtures")
    void statFileRoundTripsUnchanged(Path file) throws IOException {
        assertRoundTrip(file, PlayerStat.class);
    }

    @Test
    void configuredMapLoadsEveryField() throws URISyntaxException {
        Map map = load("/fixtures/maps/helmsdeep-1.1.2.json", Map.class);

        assertEquals(16, map.getMax());
        assertEquals(3, map.getCurr());
        assertEquals("Team Deathmatch", map.getGmType());
        assertEquals("helmsdeep", map.getName());
        assertEquals("Helm's Deep", map.getTitle());
        assertEquals("https://example.invalid/pvp.zip", map.getResourcePackURL());
        assertEquals(100, map.getSpawn().getX());
        assertEquals(64, map.getSpawn().getY());
        assertEquals(-200, map.getSpawn().getZ());
        assertEquals("world", map.getSpawn().getWorld());
        assertEquals(Set.of("RedSpawn1", "BlueSpawn1"), map.getImportantPoints().keySet());
        assertEquals(66, map.getImportantPoints().get("BlueSpawn1").getY());
        assertEquals(-190, map.getImportantPoints().get("RedSpawn1").getZ());
        assertEquals(3, map.getRegionPoints().size());
        assertEquals(1, map.getRegionPoints().get(0).getX());
        assertEquals(5, map.getRegionPoints().get(1).getY());
        assertEquals(-400, map.getRegionPoints().get(2).getZ());
    }

    @Test
    void newlyCreatedMapKeepsItsNulls() throws URISyntaxException {
        Map map = load("/fixtures/maps/newmap-1.1.2.json", Map.class);

        assertEquals("newmap", map.getName());
        assertNull(map.getGmType());
        assertNull(map.getResourcePackURL());
        assertEquals(0, map.getMax());
        assertTrue(map.getImportantPoints().isEmpty());
        assertTrue(map.getRegionPoints().isEmpty());
        assertEquals(-34, map.getSpawn().getZ());
    }

    @Test
    void statLoadsEveryField() throws URISyntaxException {
        PlayerStat stat = load("/fixtures/stats/sample-1.1.2.json", PlayerStat.class);

        assertEquals(7, stat.getKills());
        assertEquals(3, stat.getDeaths());
        assertEquals(11, stat.getGamesPlayed());
        assertEquals(5, stat.getGamesWon());
        assertEquals(4, stat.getGamesLost());
        assertEquals(2, stat.getGamesSpectated());
        assertEquals(List.of("Legolas", "Gimli", "Boromir"), stat.getPlayersKilled());
    }

    /** Reads and writes the way DBmanager.loadObj and saveObj do: from a File, through the byte writer. */
    private static void assertRoundTrip(Path file, Class<?> type) throws IOException {
        File onDisk = file.toFile();
        Object loaded = assertDoesNotThrow(() -> MAPPER.readValue(onDisk, type),
                () -> file.getFileName() + " no longer loads");
        JsonNode written = MAPPER.readTree(onDisk);
        JsonNode resaved = MAPPER.readTree(MAPPER.writeValueAsBytes(loaded));
        assertEquals(written, resaved, file.getFileName() + " changed on a load/save cycle");
    }

    private static <T> T load(String resource, Class<T> type) throws URISyntaxException {
        File file = new File(PersistenceFormatTest.class.getResource(resource).toURI());
        return assertDoesNotThrow(() -> MAPPER.readValue(file, type), () -> resource + " no longer loads");
    }
}
