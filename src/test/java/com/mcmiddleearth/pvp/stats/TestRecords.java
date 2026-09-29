package com.mcmiddleearth.pvp.stats;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Players and finished games for the stats tests. */
final class TestRecords {

    static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");

    private TestRecords() {
    }

    /** A Team Deathmatch game on AS-TDM that ended at {@code endedAt}: Alice (red, 3 kills) beat Bob (blue). */
    static MatchRecord tdmGame(String endedAt) {
        return new MatchRecord(MatchRecord.SCHEMA_VERSION, "7d1f0c52-9a3e-4b8e-a1f4-2c6d8e0b5a91", "tdm", "AS-TDM",
                "2026-09-24T11:50:00Z", endedAt,
                Map.of("teamCount", 2, "maxPlayers", 16, "midgameJoin", true),
                List.of(new MatchRecord.TeamEntry("red", List.of(ALICE.toString())),
                        new MatchRecord.TeamEntry("blue", List.of(BOB.toString()))),
                List.of(new MatchRecord.Participant(ALICE.toString(), "Alice", "red", 3, 1, 0, MatchRecord.WON),
                        new MatchRecord.Participant(BOB.toString(), "Bob", "blue", 1, 3, 0, MatchRecord.LOST)),
                "red", "win_condition");
    }
}
