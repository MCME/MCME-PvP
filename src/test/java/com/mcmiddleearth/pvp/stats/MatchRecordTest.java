package com.mcmiddleearth.pvp.stats;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchRecordTest {

    @Test
    void settingsAreSortedByIdAndCannotBeChanged() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("teamCount", 2);
        settings.put("maxPlayers", 16);
        settings.put("midgameJoin", true);

        MatchRecord record = record(settings, new ArrayList<>(), new ArrayList<>());

        assertEquals(List.of("maxPlayers", "midgameJoin", "teamCount"), List.copyOf(record.settings().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> record.settings().put("scoreGoal", 5));
    }

    @Test
    void teamsParticipantsAndMembersAreImmutableCopies() {
        List<String> members = new ArrayList<>(List.of("a"));
        List<MatchRecord.TeamEntry> teams = new ArrayList<>(List.of(new MatchRecord.TeamEntry("red", members)));
        List<MatchRecord.Participant> participants = new ArrayList<>(
                List.of(new MatchRecord.Participant("a", "Alice", "red", 1, 0, 0, MatchRecord.WON)));

        MatchRecord record = record(Map.of(), teams, participants);
        members.add("b");
        teams.clear();
        participants.clear();

        assertEquals(List.of("a"), record.teams().getFirst().members());
        assertEquals(1, record.participants().size());
        assertThrows(UnsupportedOperationException.class, () -> record.teams().getFirst().members().add("c"));
        assertThrows(UnsupportedOperationException.class, () -> record.participants().clear());
    }

    @Test
    void resultNamesAreTheWordsOfTheRecordFormat() {
        assertEquals(List.of("won", "lost", "draw", "left"),
                List.of(MatchRecord.WON, MatchRecord.LOST, MatchRecord.DRAW, MatchRecord.LEFT));
    }

    private static MatchRecord record(Map<String, Object> settings, List<MatchRecord.TeamEntry> teams,
                                      List<MatchRecord.Participant> participants) {
        return new MatchRecord(MatchRecord.SCHEMA_VERSION, "game", "tdm", "AS-TDM", "2026-09-24T11:50:00Z",
                "2026-09-24T12:00:00Z", settings, teams, participants, "red", "win_condition");
    }
}
