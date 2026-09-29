package com.mcmiddleearth.pvp.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    public record Sample(String name, int count) {
    }

    @Test
    void prettyIndentsItsOutput() throws JsonProcessingException {
        String json = Json.pretty().writeValueAsString(new Sample("a", 1));

        assertTrue(json.lines().count() > 1, json);
        assertTrue(json.lines().anyMatch(line -> line.startsWith("  \"name\"")), json);
    }

    @Test
    void compactWritesOneLine() throws JsonProcessingException {
        assertEquals("{\"name\":\"a\",\"count\":1}", Json.compact().writeValueAsString(new Sample("a", 1)));
    }

    @Test
    void unknownPropertiesAreIgnoredSoOlderBuildsReadNewerFiles() throws JsonProcessingException {
        String newer = "{\"name\":\"a\",\"count\":1,\"addedLater\":true}";

        assertEquals(new Sample("a", 1), Json.pretty().readValue(newer, Sample.class));
        assertEquals(new Sample("a", 1), Json.compact().readValue(newer, Sample.class));
    }

    @Test
    void everyCallBuildsItsOwnMapper() {
        assertNotSame(Json.pretty(), Json.pretty());
        assertNotSame(Json.compact(), Json.compact());
    }
}
