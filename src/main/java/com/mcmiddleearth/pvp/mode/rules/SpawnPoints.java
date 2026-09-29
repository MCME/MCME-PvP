package com.mcmiddleearth.pvp.mode.rules;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.text.Msg;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Where a mode's players spawn in an arena: one list per team, or one numbered list for everyone. */
public sealed interface SpawnPoints permits SpawnPoints.PerTeam, SpawnPoints.Numbered {

    static SpawnPoints perTeam() {
        return new PerTeam();
    }

    static SpawnPoints numbered(String prefix) {
        return new Numbered(prefix);
    }

    /** The spawn points of {@code team} (nullable for {@link Numbered}), in the order players are placed on them. */
    List<NamedPoint> points(Arena arena, TeamSpec team);

    /** How many teams the arena can hold with these spawn points. */
    int supportedTeams(Arena arena);

    /** Refuses a game the arena cannot hold. */
    void validate(Arena arena, int teamCount);

    /** One spawn list per palette team, from the arena's team spawns ({@code red}, {@code blue}, …). */
    record PerTeam() implements SpawnPoints {

        @Override
        public List<NamedPoint> points(Arena arena, TeamSpec team) {
            return team == null ? List.of() : arena.teamSpawns().getOrDefault(team.id(), List.of());
        }

        /** Palette teams with at least one spawn, counted from the first team up to the first one without. */
        @Override
        public int supportedTeams(Arena arena) {
            int count = 0;
            for (TeamSpec spec : Palette.TEAMS) {
                if (arena.teamSpawns().getOrDefault(spec.id(), List.of()).isEmpty()) {
                    break;
                }
                count++;
            }
            return count;
        }

        @Override
        public void validate(Arena arena, int teamCount) {
            int supported = supportedTeams(arena);
            if (supported < teamCount) {
                throw new PvpException(Msg.plural(Msg.ERROR_ARENA_TEAMS, Msg.ERROR_ARENA_TEAMS_ONE, supported),
                        Map.of("arena", arena.id(), "max", String.valueOf(supported)));
            }
        }
    }

    /**
     * The arena points named {@code prefix} or {@code prefix<digits>} (ignoring case): the unnumbered one first, then
     * by number. Gaps are fine, which 1.1.2 did not allow. Everyone shares the list, whatever their team.
     */
    record Numbered(String prefix) implements SpawnPoints {

        public Numbered {
            Objects.requireNonNull(prefix, "prefix");
        }

        @Override
        public List<NamedPoint> points(Arena arena, TeamSpec team) {
            record Candidate(BigInteger number, String name, NamedPoint point) {
            }
            Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "(\\d*)", Pattern.CASE_INSENSITIVE);
            return arena.points().entrySet().stream()
                    .map(entry -> {
                        Matcher matcher = pattern.matcher(entry.getKey());
                        if (!matcher.matches()) {
                            return null;
                        }
                        String digits = matcher.group(1);
                        BigInteger number = digits.isEmpty() ? BigInteger.ONE.negate() : new BigInteger(digits);
                        return new Candidate(number, entry.getKey(), entry.getValue());
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Candidate::number).thenComparing(Candidate::name))
                    .map(Candidate::point)
                    .toList();
        }

        @Override
        public int supportedTeams(Arena arena) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void validate(Arena arena, int teamCount) {
            if (points(arena, null).isEmpty()) {
                throw new PvpException(Msg.ERROR_ARENA_SPAWNS, Map.of("arena", arena.id(), "points", prefix));
            }
        }
    }
}
