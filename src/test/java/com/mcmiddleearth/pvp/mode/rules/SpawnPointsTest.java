package com.mcmiddleearth.pvp.mode.rules;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpawnPointsTest {

    private static NamedPoint at(double x) {
        return NamedPoint.of("world", x, 64, 0);
    }

    private static Arena withPoints(Map<String, NamedPoint> points, Map<String, List<NamedPoint>> teamSpawns) {
        return new Arena(Arena.SCHEMA_VERSION, "Custom", "tdm", null, "Custom", 16, null, at(0), points, teamSpawns,
                null, null);
    }

    @Test
    void perTeamReadsTheTeamSpawnsOfTheTeam() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);
        SpawnPoints spawns = SpawnPoints.perTeam();

        assertEquals(arena.teamSpawns().get("red"), spawns.points(arena, Palette.RED));
        assertEquals(1, spawns.points(arena, Palette.BLUE).size());
        assertEquals(List.of(), spawns.points(arena, Palette.GREEN));
        assertEquals(List.of(), spawns.points(arena, null));
    }

    @Test
    void perTeamSupportsTheTeamsWithSpawnsFromTheFirstTeamOn() {
        assertEquals(3, SpawnPoints.perTeam().supportedTeams(TestArenas.tdm("A", 0, 3)));
        Arena gap = withPoints(Map.of(), Map.of("red", List.of(at(1)), "green", List.of(at(2))));
        assertEquals(1, SpawnPoints.perTeam().supportedTeams(gap));
    }

    @Test
    void perTeamRefusesMoreTeamsThanSpawnSets() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);

        PvpException e = assertThrows(PvpException.class, () -> SpawnPoints.perTeam().validate(arena, 3));

        assertEquals(Msg.ERROR_ARENA_TEAMS, e.messageKey());
        assertEquals(Map.of("arena", "AS-TDM", "max", "2"), e.args());
        assertDoesNotThrow(() -> SpawnPoints.perTeam().validate(arena, 2));
    }

    @Test
    void numberedListsTheUnnumberedPointFirstThenByNumber() {
        Arena arena = withPoints(Map.of("PlayerSpawn2", at(2), "playerspawn", at(0), "PlayerSpawn10", at(10),
                "PlayerSpawn1", at(1), "RedSpawn", at(99), "PlayerSpawnX", at(98)), Map.of());

        assertEquals(List.of(at(0), at(1), at(2), at(10)), SpawnPoints.numbered("PlayerSpawn").points(arena, null));
        assertEquals(Integer.MAX_VALUE, SpawnPoints.numbered("PlayerSpawn").supportedTeams(arena));
    }

    @Test
    void numberedSortsANumberBeyondLongRange() {
        Arena arena = withPoints(Map.of("PlayerSpawn1", at(1), "PlayerSpawn99999999999999999999", at(2)), Map.of());

        assertEquals(List.of(at(1), at(2)), SpawnPoints.numbered("PlayerSpawn").points(arena, null));
    }

    @Test
    void numberedIgnoresTheTeam() {
        Arena arena = TestArenas.ffa("AS-FFA", 0, 3);

        assertEquals(3, SpawnPoints.numbered("PlayerSpawn").points(arena, Palette.RED).size());
    }

    @Test
    void numberedRefusesAnArenaWithoutThosePoints() {
        Arena arena = TestArenas.tdm("AS-TDM", 0, 2);

        PvpException e = assertThrows(PvpException.class, () -> SpawnPoints.numbered("PlayerSpawn").validate(arena, 0));

        assertEquals(Msg.ERROR_ARENA_SPAWNS, e.messageKey());
        assertEquals(Map.of("arena", "AS-TDM", "points", "PlayerSpawn"), e.args());
        assertDoesNotThrow(() -> SpawnPoints.numbered("PlayerSpawn").validate(TestArenas.ffa("F", 0, 1), 0));
    }
}
