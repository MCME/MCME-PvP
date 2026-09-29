package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hands out spawn points round-robin, one cursor per spawn list, shared by the countdown, respawns and mid-game
 * joins (1.1.2's Free For All did the same, but crashed at the end of the list instead of wrapping).
 */
final class SpawnCursor {

    private final Arena arena;
    private final SpawnPoints spawnPoints;
    private final Map<List<NamedPoint>, Integer> cursors = new HashMap<>();

    SpawnCursor(Arena arena, SpawnPoints spawnPoints) {
        this.arena = arena;
        this.spawnPoints = spawnPoints;
    }

    /** The next spawn of the team's list (null: the list everyone shares); the arena spawn when the list is empty. */
    NamedPoint next(TeamSpec team) {
        List<NamedPoint> points = spawnPoints.points(arena, team);
        if (points.isEmpty()) {
            return arena.spawn();
        }
        int index = cursors.getOrDefault(points, 0);
        cursors.put(points, (index + 1) % points.size());
        return points.get(index);
    }
}
