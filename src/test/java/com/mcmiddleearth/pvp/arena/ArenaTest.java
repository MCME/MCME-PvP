package com.mcmiddleearth.pvp.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaTest {

    private static final Region REGION = new Region("pvp",
            List.of(new Region.Vertex(0, 0), new Region.Vertex(10, 0), new Region.Vertex(10, 10)), 0, 1000);
    private static final NamedPoint SPAWN = NamedPoint.of("spawn_world", 1.5, 64, 1.5);
    private static final ImportInfo IMPORTED =
            new ImportInfo("AS-TDM", "ab12", "2026-09-24T12:00:00Z", "Team Deathmatch", List.of());

    @Test
    void theWorldIsTheRegionsThenTheSpawnsThenWorld() {
        assertEquals("pvp", arena("tdm", null, SPAWN, REGION).world());
        assertEquals("spawn_world", arena("tdm", null, SPAWN, null).world());
        assertEquals("world", arena(null, "the map has no spawn point", null, null).world());
    }

    @Test
    void withModeMakesAnArenaPlayableAndClearsTheReason() {
        Arena unassigned = arena(null, "no 3.0 mode for \"Team Deathmatch\" yet", SPAWN, REGION);

        Arena assigned = unassigned.withMode("tdm");

        assertFalse(unassigned.playable());
        assertTrue(assigned.playable());
        assertEquals("tdm", assigned.modeId());
        assertNull(assigned.unassignedReason());
        assertEquals(unassigned, assigned.unassigned(unassigned.unassignedReason()), "nothing else changes");
    }

    @Test
    void unassignedClearsTheModeAndKeepsTheReason() {
        Arena unassigned = arena("ffa", null, SPAWN, null).unassigned("the map has no spawn point");

        assertNull(unassigned.modeId());
        assertEquals("the map has no spawn point", unassigned.unassignedReason());
        assertFalse(unassigned.playable());
    }

    @Test
    void pointsAndTeamSpawnsAreSortedImmutableCopies() {
        NamedPoint point = NamedPoint.of("world", 1, 64, 1);
        Map<String, NamedPoint> points = new HashMap<>();
        points.put("RedSpawn", point);
        points.put("BlueSpawn", point);
        points.put("Hill", point);
        List<NamedPoint> red = new ArrayList<>(List.of(point, point));
        Map<String, List<NamedPoint>> teamSpawns = new HashMap<>();
        teamSpawns.put("red", red);
        teamSpawns.put("blue", List.of(point));

        Arena arena = new Arena(Arena.SCHEMA_VERSION, "AS-KOTH", "koth", null, "Hill", 8, null, point,
                points, teamSpawns, null, null);
        points.put("Later", point);
        red.add(point);

        assertEquals(List.of("BlueSpawn", "Hill", "RedSpawn"), List.copyOf(arena.points().keySet()));
        assertEquals(List.of("blue", "red"), List.copyOf(arena.teamSpawns().keySet()));
        assertEquals(2, arena.teamSpawns().get("red").size());
        assertThrows(UnsupportedOperationException.class, () -> arena.points().put("x", point));
        assertThrows(UnsupportedOperationException.class, () -> arena.teamSpawns().put("green", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> arena.teamSpawns().get("red").add(point));
    }

    @Test
    void importInfoKeepsACopyOfTheUnknownProperties() {
        List<String> unknown = new ArrayList<>(List.of("fbt"));

        ImportInfo info = new ImportInfo("OLD-LOBBYSIGN", "00", "2026-09-24T12:00:00Z", null, unknown);
        unknown.add("lobbySign");

        assertEquals(List.of("fbt"), info.unknownProperties());
        assertNull(info.legacyGmType());
    }

    private static Arena arena(String modeId, String reason, NamedPoint spawn, Region region) {
        return new Arena(Arena.SCHEMA_VERSION, "AS-TDM", modeId, reason, "Amon Sul", 16, null, spawn,
                Map.of(), Map.of(), region, IMPORTED);
    }
}
