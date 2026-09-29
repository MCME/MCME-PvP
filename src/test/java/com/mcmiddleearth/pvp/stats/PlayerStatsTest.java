package com.mcmiddleearth.pvp.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static com.mcmiddleearth.pvp.stats.TestRecords.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerStatsTest {

    @Test
    void emptyStatsCountNothing() {
        assertEquals(new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", 0, 0, 0, 0, 0, 0, List.of()),
                PlayerStats.empty(ALICE, "Alice"));
    }

    @Test
    void theKillDeathRatioIsTheKillsWhenThereAreNoDeaths() {
        assertEquals(7.0, stats(7, 0).killDeathRatio());
        assertEquals(3.5, stats(7, 2).killDeathRatio());
        assertEquals(0.0, stats(0, 0).killDeathRatio());
    }

    @Test
    void plusAddsAFinishedGameUnderTheCurrentName() {
        PlayerStats before = new PlayerStats(1, ALICE, "OldName", 7, 3, 11, 5, 4, 2, List.of("Legolas"));

        PlayerStats after = before.plus(participant(3, 1, MatchRecord.WON));

        assertEquals(new PlayerStats(1, ALICE, "Alice", 10, 4, 12, 6, 4, 2, List.of("Legolas")), after);
    }

    @ParameterizedTest(name = "{0}: won +{1}, lost +{2}")
    @CsvSource({"won, 1, 0", "lost, 0, 1", "draw, 0, 0", "left, 0, 0"})
    void everyResultCountsAsPlayedAndOnlyWonOrLostCountMore(String result, int won, int lost) {
        PlayerStats after = PlayerStats.empty(ALICE, "Alice").plus(participant(0, 0, result));

        assertEquals(1, after.gamesPlayed());
        assertEquals(won, after.gamesWon());
        assertEquals(lost, after.gamesLost());
    }

    @Test
    void plusSpectatedCountsOneWatchedGameAndNothingElse() {
        PlayerStats after = stats(7, 3).plusSpectated("NewName");

        assertEquals(1, after.gamesSpectated());
        assertEquals("NewName", after.name());
        assertEquals(7, after.kills());
        assertEquals(3, after.deaths());
        assertEquals(0, after.gamesPlayed());
    }

    @Test
    void playersKilledIsAnImmutableCopy() {
        List<String> victims = new ArrayList<>(List.of("Legolas"));
        PlayerStats stats = new PlayerStats(1, ALICE, "Alice", 1, 0, 1, 0, 0, 0, victims);

        victims.add("Gimli");

        assertEquals(List.of("Legolas"), stats.playersKilled());
        assertThrows(UnsupportedOperationException.class, () -> stats.playersKilled().add("Boromir"));
    }

    private static PlayerStats stats(int kills, int deaths) {
        return new PlayerStats(PlayerStats.SCHEMA_VERSION, ALICE, "Alice", kills, deaths, 0, 0, 0, 0, List.of());
    }

    private static MatchRecord.Participant participant(int kills, int deaths, String result) {
        return new MatchRecord.Participant(ALICE.toString(), "Alice", "red", kills, deaths, kills, result);
    }
}
