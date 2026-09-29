package com.mcmiddleearth.pvp.stats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One finished game, appended to {@code matches/<yyyy-MM>.jsonl}. Times are ISO-8601 ({@code Instant.toString()}).
 *
 * @param winner    the winning team id, the winning player's uuid, or null for a draw
 * @param endReason "win_condition", "time_limit", "stopped", "shutdown" or "abandoned"
 */
public record MatchRecord(int schemaVersion, String gameId, String mode, String arena, String startedAt, String endedAt,
                          Map<String, Object> settings, List<TeamEntry> teams, List<Participant> participants,
                          String winner, String endReason) {

    public static final int SCHEMA_VERSION = 1;
    public static final String WON = "won";
    public static final String LOST = "lost";
    public static final String DRAW = "draw";
    public static final String LEFT = "left";

    public MatchRecord {
        settings = Collections.unmodifiableMap(new TreeMap<>(settings));
        teams = List.copyOf(teams);
        participants = List.copyOf(participants);
    }

    /** A team and the uuids of its participants. */
    public record TeamEntry(String id, List<String> members) {
        public TeamEntry {
            members = List.copyOf(members);
        }
    }

    /**
     * One participant. {@code team} is the team id, or null in modes without teams.
     * {@code result} is {@link #WON}, {@link #LOST}, {@link #DRAW} or {@link #LEFT}.
     */
    public record Participant(String uuid, String name, String team, int kills, int deaths, int score, String result) {
    }
}
