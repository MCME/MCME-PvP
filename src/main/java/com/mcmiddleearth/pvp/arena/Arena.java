package com.mcmiddleearth.pvp.arena;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A place games are played in, stored as {@code arenas/<id>.json}.
 *
 * @param modeId           the mode that plays here, or null when the arena is unassigned (not playable)
 * @param unassignedReason why it is unassigned; null when {@code modeId} is set
 * @param resourcePackUrl  kept from 1.1.2 but not sent to players; nullable
 * @param spawn            where waiting players, spectators and eliminated players stand; null only when unassigned
 * @param points           1.1.2's important points by name, as standing positions
 * @param teamSpawns       spawn points per team id ({@code red}, {@code blue}, …), in order
 * @param region           the play area, or null for none (the arena then reserves its whole world)
 * @param imported         set when the importer made the arena, else null
 */
public record Arena(
        int schemaVersion,
        String id,
        String modeId,
        String unassignedReason,
        String title,
        int maxPlayers,
        String resourcePackUrl,
        NamedPoint spawn,
        Map<String, NamedPoint> points,
        Map<String, List<NamedPoint>> teamSpawns,
        Region region,
        ImportInfo imported) {

    public static final int SCHEMA_VERSION = 1;

    public Arena {
        points = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(points, "\"points\" is missing; write {} for none")));
        TreeMap<String, List<NamedPoint>> spawns = new TreeMap<>();
        Objects.requireNonNull(teamSpawns, "\"teamSpawns\" is missing; write {} for none")
                .forEach((team, list) -> spawns.put(team, List.copyOf(list)));
        teamSpawns = Collections.unmodifiableMap(spawns);
    }

    public boolean playable() {
        return modeId != null;
    }

    /** The world the arena is in: the region's, else the spawn's, else "world". */
    public String world() {
        if (region != null) {
            return region.world();
        }
        return spawn != null ? spawn.world() : "world";
    }

    /** This arena, playable in {@code newModeId}, with no unassigned reason. */
    public Arena withMode(String newModeId) {
        return new Arena(schemaVersion, id, newModeId, null, title, maxPlayers, resourcePackUrl, spawn, points, teamSpawns, region, imported);
    }

    /** This arena with no mode, unassigned for {@code reason}. */
    public Arena unassigned(String reason) {
        return new Arena(schemaVersion, id, null, reason, title, maxPlayers, resourcePackUrl, spawn, points, teamSpawns, region, imported);
    }
}
