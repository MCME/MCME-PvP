package com.mcmiddleearth.mcme.pvp.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.mcme.pvp.PVP.PlayerStat;
import com.mcmiddleearth.mcme.pvp.maps.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Map and stat files on the servers were written by 1.1.2 (Jackson 2.17.1). These tests pin that
 * format: every fixture must load and re-serialize to the same JSON tree. Property order may
 * differ between Jackson versions, so trees are compared, not bytes.
 *
 * Real map files from production can be dropped into src/test/resources/fixtures/maps/ and are
 * picked up automatically. Stat files are NOT committed from production: they hold player UUIDs
 * and names.
 */
class PersistenceFormatTest {

    private static final ObjectMapper MAPPER = DBmanager.getJSonParser();

    static Stream<Path> mapFixtures() throws IOException, URISyntaxException {
        return fixtures("/fixtures/maps");
    }

    static Stream<Path> statFixtures() throws IOException, URISyntaxException {
        return fixtures("/fixtures/stats");
    }

    private static Stream<Path> fixtures(String dir) throws IOException, URISyntaxException {
        Path root = Path.of(PersistenceFormatTest.class.getResource(dir).toURI());
        try (Stream<Path> files = Files.list(root)) {
            return files.filter(p -> p.toString().endsWith(".json")).sorted().toList().stream();
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
    void mapFixtureLoadsEveryField() throws IOException, URISyntaxException {
        Map map = MAPPER.readValue(resource("/fixtures/maps/helmsdeep-1.1.2.json"), Map.class);

        assertEquals(16, map.getMax());
        assertEquals("Team Deathmatch", map.getGmType());
        assertEquals("helmsdeep", map.getName());
        assertEquals("Helm's Deep", map.getTitle());
        assertEquals("https://example.invalid/pvp.zip", map.getResourcePackURL());
        assertEquals(100, map.getSpawn().getX());
        assertEquals(64, map.getSpawn().getY());
        assertEquals(-200, map.getSpawn().getZ());
        assertEquals("world", map.getSpawn().getWorld());
        assertEquals(Set.of("RedSpawn1", "BlueSpawn1"), map.getImportantPoints().keySet());
        assertEquals(-190, map.getImportantPoints().get("RedSpawn1").getZ());
        assertEquals(3, map.getRegionPoints().size());
        assertEquals(-400, map.getRegionPoints().get(2).getZ());
    }

    @Test
    void statFixtureLoadsEveryField() throws IOException, URISyntaxException {
        PlayerStat stat = MAPPER.readValue(resource("/fixtures/stats/sample-1.1.2.json"), PlayerStat.class);

        assertEquals(2, stat.getKills());
        assertEquals(1, stat.getDeaths());
        assertEquals(1, stat.getGamesPlayed());
        assertEquals(1, stat.getGamesWon());
        assertEquals(0, stat.getGamesLost());
        assertEquals(1, stat.getGamesSpectated());
        assertEquals(List.of("Legolas", "Gimli"), stat.getPlayersKilled());
    }

    private static void assertRoundTrip(Path file, Class<?> type) throws IOException {
        String json = Files.readString(file);
        JsonNode written = MAPPER.readTree(json);
        JsonNode rewritten = MAPPER.valueToTree(MAPPER.readValue(json, type));
        assertEquals(written, rewritten, file.getFileName() + " changed on a load/save cycle");
    }

    private static String resource(String name) throws IOException, URISyntaxException {
        return Files.readString(Path.of(PersistenceFormatTest.class.getResource(name).toURI()));
    }
}
