package com.mcmiddleearth.pvp.mode.rules;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Places join units (one player, or a party that must stay together) on teams. 1.1.2 alternated players sorted by
 * lifetime K/D; {@link #assign} keeps that skill spread and also keeps parties whole.
 */
public final class TeamBalancer {

    /** A group placed together; skill is the average lifetime K/D of its members. */
    public record Unit(int size, double skill) {
    }

    private TeamBalancer() {
    }

    /**
     * Team index per unit (in the given order): largest units first, then higher skill first; each goes to the team
     * with the fewest players, ties to the lower skill total (the sum of its members' skill), then the lower index.
     */
    public static int[] assign(List<Unit> units, int teamCount) {
        requireTeams(teamCount);
        int[] sizes = new int[teamCount];
        double[] skill = new double[teamCount];
        int[] result = new int[units.size()];
        List<Integer> order = IntStream.range(0, units.size()).boxed()
                .sorted(Comparator.<Integer>comparingInt(i -> units.get(i).size()).reversed()
                        .thenComparing(Comparator.<Integer>comparingDouble(i -> units.get(i).skill()).reversed())
                        .thenComparingInt(i -> i))
                .toList();
        for (int index : order) {
            Unit unit = units.get(index);
            int best = 0;
            for (int team = 1; team < teamCount; team++) {
                if (sizes[team] < sizes[best] || (sizes[team] == sizes[best] && skill[team] < skill[best])) {
                    best = team;
                }
            }
            result[index] = best;
            sizes[best] += unit.size();
            skill[best] += unit.skill() * unit.size();
        }
        return result;
    }

    /** Team index per unit: unit i goes to team i % teamCount. */
    public static int[] roundRobin(List<Unit> units, int teamCount) {
        requireTeams(teamCount);
        return IntStream.range(0, units.size()).map(i -> i % teamCount).toArray();
    }

    /** The index of the smallest size (ties: the lowest index). */
    public static int smallest(List<Integer> sizes) {
        if (sizes.isEmpty()) {
            throw new IllegalArgumentException("no sizes to compare");
        }
        int best = 0;
        for (int i = 1; i < sizes.size(); i++) {
            if (sizes.get(i) < sizes.get(best)) {
                best = i;
            }
        }
        return best;
    }

    private static void requireTeams(int teamCount) {
        if (teamCount < 1) {
            throw new IllegalArgumentException("team count " + teamCount);
        }
    }
}
