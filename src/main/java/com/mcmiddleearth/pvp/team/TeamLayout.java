package com.mcmiddleearth.pvp.team;

/** How a mode splits players into teams. */
public sealed interface TeamLayout {

    /** No teams: everyone for themselves (players get personal colors). */
    record None() implements TeamLayout {
    }

    /** Always exactly {@code count} teams. */
    record Fixed(int count) implements TeamLayout {
        public Fixed {
            if (count < 2 || count > Palette.TEAMS.size()) {
                throw new IllegalArgumentException("team count " + count);
            }
        }
    }

    /** Staff choose between {@code min} and {@code max} teams with the {@code teamCount} setting. */
    record Configurable(int min, int max) implements TeamLayout {
        public Configurable {
            if (min < 2 || max < min || max > Palette.TEAMS.size()) {
                throw new IllegalArgumentException("team range " + min + ".." + max);
            }
        }
    }

    static TeamLayout none() {
        return new None();
    }

    static TeamLayout fixed(int count) {
        return new Fixed(count);
    }

    static TeamLayout configurable(int min, int max) {
        return new Configurable(min, max);
    }

    default boolean hasTeams() {
        return !(this instanceof None);
    }
}
