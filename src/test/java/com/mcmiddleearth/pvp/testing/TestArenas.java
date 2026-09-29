package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.arena.LegacyMapImporter;
import com.mcmiddleearth.pvp.arena.LoadReport;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.arena.Region;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.ffa.FreeForAll;
import com.mcmiddleearth.pvp.mode.tdm.TeamDeathmatch;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

/** Arenas, registries and players for engine tests. Every arena is 20x20 blocks in world "world", max 16 players. */
public final class TestArenas {

    public static final String WORLD = "world";

    private TestArenas() {
    }

    /**
     * A playable TDM arena: a 20x20 region starting at (x0, 0), spawn in the middle, spawns for the first
     * {@code teams} palette teams.
     */
    public static Arena tdm(String id, int x0, int teams) {
        Map<String, List<NamedPoint>> spawns = new TreeMap<>();
        List<TeamSpec> specs = Palette.teams(teams);
        for (int i = 0; i < specs.size(); i++) {
            spawns.put(specs.get(i).id(), List.of(NamedPoint.of(WORLD, x0 + 2.5 + 3 * i, 64, 2.5)));
        }
        return arena(id, "tdm", x0, Map.of(), spawns);
    }

    /** A playable FFA arena with {@code spawns} points PlayerSpawn, PlayerSpawn1, ...; the same region shape. */
    public static Arena ffa(String id, int x0, int spawns) {
        Map<String, NamedPoint> points = new TreeMap<>();
        for (int i = 0; i < spawns; i++) {
            points.put(i == 0 ? "PlayerSpawn" : "PlayerSpawn" + i, NamedPoint.of(WORLD, x0 + 1.5 + i, 64, 17.5));
        }
        return arena(id, "ffa", x0, points, Map.of());
    }

    /** The same arena without a region: it reserves its whole world. */
    public static Arena withoutRegion(Arena arena) {
        return new Arena(arena.schemaVersion(), arena.id(), arena.modeId(), arena.unassignedReason(), arena.title(),
                arena.maxPlayers(), arena.resourcePackUrl(), arena.spawn(), arena.points(), arena.teamSpawns(), null,
                arena.imported());
    }

    /** The same arena, moved to another world. */
    public static Arena inWorld(Arena arena, String world) {
        Map<String, NamedPoint> points = new TreeMap<>();
        arena.points().forEach((name, point) -> points.put(name, move(point, world)));
        Map<String, List<NamedPoint>> spawns = new TreeMap<>();
        arena.teamSpawns().forEach((team, list) ->
                spawns.put(team, list.stream().map(point -> move(point, world)).toList()));
        Region region = arena.region() == null ? null
                : new Region(world, arena.region().vertices(), arena.region().minY(), arena.region().maxY());
        return new Arena(arena.schemaVersion(), arena.id(), arena.modeId(), arena.unassignedReason(), arena.title(),
                arena.maxPlayers(), arena.resourcePackUrl(), move(arena.spawn(), world), points, spawns, region,
                arena.imported());
    }

    /** Saves the arenas under dir/arenas and loads a registry from there (no legacy maps). */
    public static ArenaRegistry registry(Path dir, Arena... arenas) {
        ArenaStore store = new ArenaStore(dir.resolve("arenas"), Json.pretty());
        try {
            for (Arena arena : arenas) {
                store.save(arena);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // There are no legacy maps here, so no gmType ever needs a mode.
        Function<String, Optional<String>> noLegacyModes = gmType -> Optional.empty();
        ArenaRegistry registry = new ArenaRegistry(store, new LegacyMapImporter(noLegacyModes, Clock.systemUTC()),
                dir.resolve("maps"), noLegacyModes, new RecordingLog().logger());
        LoadReport report = registry.load();
        if (!report.failed().isEmpty()) {
            throw new IllegalStateException("test arenas did not load: " + report.failed());
        }
        return registry;
    }

    /** TDM and FFA. */
    public static ModeRegistry modes() {
        return new ModeRegistry(List.of(new TeamDeathmatch(), new FreeForAll()));
    }

    /** A player ref with a stable uuid derived from the name. */
    public static PlayerRef player(String name) {
        return new PlayerRef(UUID.nameUUIDFromBytes(("player:" + name).getBytes(StandardCharsets.UTF_8)), name);
    }

    private static Arena arena(String id, String modeId, int x0, Map<String, NamedPoint> points,
                               Map<String, List<NamedPoint>> teamSpawns) {
        Region region = new Region(WORLD, List.of(new Region.Vertex(x0, 0), new Region.Vertex(x0 + 19, 0),
                new Region.Vertex(x0 + 19, 19), new Region.Vertex(x0, 19)), 0, 1000);
        NamedPoint middle = NamedPoint.of(WORLD, x0 + 10.5, 64, 10.5);
        return new Arena(Arena.SCHEMA_VERSION, id, modeId, null, id, 16, null, middle, points, teamSpawns, region,
                null);
    }

    private static NamedPoint move(NamedPoint point, String world) {
        return new NamedPoint(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
    }
}
