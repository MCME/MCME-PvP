package com.mcmiddleearth.pvp.arena;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Geometry that is present but malformed (such an import must throw, not silently
 * become a wrong arena), plus a BOM-prefixed file. Kept apart from {@link LegacyMapImporterTest} so that class
 * stays under ~300 lines.
 */
class LegacyMapImporterOddInputTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), ZoneOffset.UTC);
    private static final Function<String, Optional<String>> MODES = gmType -> Optional.ofNullable(
            Map.of("teamdeathmatch", "tdm", "freeforall", "ffa").get(gmType.replaceAll("\\s", "").toLowerCase(Locale.ROOT)));

    @TempDir
    Path maps;

    private final LegacyMapImporter importer = new LegacyMapImporter(MODES, CLOCK);

    @Test
    void aNonObjectRegionVertexThrowsNamingItsIndex() throws IOException {
        Path file = write("BAD-VERTEX", "{\"regionPoints\":[{\"x\":0,\"z\":0},{\"x\":1,\"z\":0},\"oops\"]}");

        IOException e = assertThrows(IOException.class, () -> importer.importFile(file));

        assertTrue(e.getMessage().contains("regionPoints[2]"), e.getMessage());
    }

    @Test
    void aMissingCoordinateOnAnImportantPointThrowsNamingThePoint() throws IOException {
        Path file = write("BAD-POINT", "{\"importantPoints\":{\"RedSpawn\":{\"world\":\"world\",\"y\":64,\"z\":0}}}");

        IOException e = assertThrows(IOException.class, () -> importer.importFile(file));

        assertTrue(e.getMessage().contains("importantPoints.RedSpawn"), e.getMessage());
        assertTrue(e.getMessage().contains("\"x\""), e.getMessage());
    }

    @Test
    void regionPointsThatIsNotAnArrayThrows() throws IOException {
        Path file = write("BAD-REGION", "{\"regionPoints\":\"nope\"}");

        assertThrows(IOException.class, () -> importer.importFile(file));
    }

    @Test
    void importantPointsThatIsNotAnObjectThrows() throws IOException {
        Path file = write("BAD-IMPORTANT", "{\"importantPoints\":[1,2,3]}");

        assertThrows(IOException.class, () -> importer.importFile(file));
    }

    @Test
    void aNullImportantPointsStillImportsWithNoPoints() throws IOException {
        Arena arena = importer.importFile(write("NULL-IMPORTANT", "{\"importantPoints\":null}"));

        assertEquals(Map.of(), arena.points());
    }

    @Test
    void aDecimalCoordinateIsTruncatedTowardZero() throws IOException {
        Arena arena = importer.importFile(write("DECIMAL",
                "{\"importantPoints\":{\"RedSpawn\":{\"world\":\"world\",\"x\":-500.7,\"y\":10,\"z\":0}}}"));

        assertEquals(NamedPoint.fromLegacyFloor("world", -500, 10, 0), arena.points().get("RedSpawn"));
        assertEquals(-499.5, arena.points().get("RedSpawn").x(), "-500.7 truncates to block -500, then floor's +0.5");
    }

    @Test
    void aFileStartingWithAUtf8BomImports() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] json = ("{\"gmType\":\"Team Deathmatch\",\"title\":\"Bommed\",\"spawn\":"
                + "{\"world\":\"world\",\"x\":0,\"y\":64,\"z\":0}}").getBytes(StandardCharsets.UTF_8);
        Path file = maps.resolve("BOM");
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(bom);
            out.write(json);
        }

        Arena arena = importer.importFile(file);

        assertEquals("Bommed", arena.title());
        assertEquals("tdm", arena.modeId());
    }

    private Path write(String name, String json) throws IOException {
        return Files.writeString(maps.resolve(name), json);
    }
}
