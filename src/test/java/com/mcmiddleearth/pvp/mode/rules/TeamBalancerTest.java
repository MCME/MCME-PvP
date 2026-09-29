package com.mcmiddleearth.pvp.mode.rules;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamBalancerTest {

    private static TeamBalancer.Unit solo(double skill) {
        return new TeamBalancer.Unit(1, skill);
    }

    @Test
    void aPartyStaysTogetherAndTheOthersEvenTheTeamsOut() {
        int[] teams = TeamBalancer.assign(List.of(new TeamBalancer.Unit(3, 1.0), solo(1.0), solo(1.0), solo(1.0)), 2);

        assertArrayEquals(new int[]{0, 1, 1, 1}, teams);
    }

    @Test
    void largerUnitsArePlacedFirst() {
        int[] teams = TeamBalancer.assign(List.of(solo(5.0), new TeamBalancer.Unit(2, 0.0)), 2);

        assertArrayEquals(new int[]{1, 0}, teams);
    }

    @Test
    void playersSpreadEvenlyOverThreeTeams() {
        int[] teams = TeamBalancer.assign(List.of(solo(1), solo(1), solo(1), solo(1), solo(1), solo(1)), 3);

        assertEquals(2, Arrays.stream(teams).filter(team -> team == 0).count());
        assertEquals(2, Arrays.stream(teams).filter(team -> team == 1).count());
        assertEquals(2, Arrays.stream(teams).filter(team -> team == 2).count());
    }

    @Test
    void skillIsSpreadOverTheTeams() {
        // Highest skill first: 4 -> team 0, 3 -> team 1, 2 -> team 1 (lower total), 1 -> team 0. Totals 5 and 5.
        int[] teams = TeamBalancer.assign(List.of(solo(1.0), solo(2.0), solo(3.0), solo(4.0)), 2);

        assertArrayEquals(new int[]{0, 1, 1, 0}, teams);
    }

    @Test
    void fewerPlayersWinsOverLowerSkillTotal() {
        // Comparing skill first would send every solo(1.0) to team 1 (the lower skill total), giving {0, 1, 1, 1}.
        int[] teams = TeamBalancer.assign(List.of(solo(10.0), solo(1.0), solo(1.0), solo(1.0)), 2);

        assertArrayEquals(new int[]{0, 1, 1, 0}, teams);
    }

    @Test
    void aUnitAddsSizeTimesSkillToItsTeamsTotal() {
        // Without * unit.size(), team 0's total would stay 4.0 (not 8.0) and the last solo would go there too,
        // giving {0, 1, 1, 0}.
        int[] teams = TeamBalancer.assign(
                List.of(new TeamBalancer.Unit(2, 4.0), solo(3.0), solo(3.0), solo(1.0)), 2);

        assertArrayEquals(new int[]{0, 1, 1, 1}, teams);
    }

    @Test
    void completeTiesGoToTheLowerTeamIndexInUnitOrder() {
        assertArrayEquals(new int[]{0, 1}, TeamBalancer.assign(List.of(solo(1.0), solo(1.0)), 2));
        assertArrayEquals(new int[]{0}, TeamBalancer.assign(List.of(solo(1.0)), 3));
    }

    @Test
    void roundRobinPutsUnitIOnTeamIModuloTheCount() {
        List<TeamBalancer.Unit> units = List.of(solo(0), solo(0), solo(0), solo(0), solo(0));

        assertArrayEquals(new int[]{0, 1, 0, 1, 0}, TeamBalancer.roundRobin(units, 2));
        assertArrayEquals(new int[]{0, 1, 2, 0, 1}, TeamBalancer.roundRobin(units, 3));
    }

    @Test
    void smallestPicksTheFirstOfTheSmallest() {
        assertEquals(1, TeamBalancer.smallest(List.of(3, 1, 1)));
        assertEquals(0, TeamBalancer.smallest(List.of(2, 2, 2)));
        assertThrows(IllegalArgumentException.class, () -> TeamBalancer.smallest(List.of()));
    }

    @Test
    void aTeamCountBelowOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> TeamBalancer.assign(List.of(solo(1)), 0));
        assertThrows(IllegalArgumentException.class, () -> TeamBalancer.roundRobin(List.of(solo(1)), 0));
    }
}
