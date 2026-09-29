package com.mcmiddleearth.pvp.mode.rules;

import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameTeam;
import com.mcmiddleearth.pvp.game.IntSetting;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.game.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/** The win conditions modes are built from. */
public final class WinConditions {

    private WinConditions() {
    }

    /**
     * The last team with alive players wins; a draw only when no team has players left (everyone left). Deaths
     * count one at a time, so when the last players of two teams die in the same tick, the first death handled
     * decides. No time limit.
     */
    public static WinCondition lastTeamStanding() {
        return new LastTeamStanding();
    }

    /**
     * At the time limit, the single highest score above 0 wins (a team, or a player in modes without teams).
     * Only {@code Setting.TIME_LIMIT} is supported.
     */
    public static WinCondition highestAt(IntSetting timeLimit) {
        if (!timeLimit.equals(Setting.TIME_LIMIT)) {
            throw new IllegalArgumentException(
                    "highestAt only supports the time limit setting (" + Setting.TIME_LIMIT.id() + ")");
        }
        return new HighestAt(timeLimit);
    }

    /** The first team (or player) to reach the goal wins at once; a goal of 0 is off. */
    public static WinCondition firstTo(IntSetting goal) {
        return new FirstTo(goal);
    }

    static WinCondition either(WinCondition first, WinCondition second) {
        return new Either(first, second);
    }

    private record LastTeamStanding() implements WinCondition {

        @Override
        public Optional<Outcome> evaluate(Game game) {
            List<GameTeam> standing = game.teams().stream().filter(team -> team.aliveCount() > 0).toList();
            if (standing.size() == 1) {
                return Optional.of(Outcome.team(standing.get(0).id(), EndReason.WIN_CONDITION));
            }
            return standing.isEmpty() ? Optional.of(Outcome.draw(EndReason.WIN_CONDITION)) : Optional.empty();
        }
    }

    private record HighestAt(IntSetting timeLimit) implements WinCondition {

        @Override
        public Optional<Outcome> evaluate(Game game) {
            return Optional.empty();
        }

        @Override
        public boolean usesTimeLimit() {
            return true;
        }

        /** Teams in team modes, else the present players; a tie or a top score of 0 is a draw. */
        @Override
        public Outcome timeUp(Game game) {
            if (!game.teams().isEmpty()) {
                return soleLeader(game.teams(), GameTeam::score)
                        .map(team -> Outcome.team(team.id(), EndReason.TIME_LIMIT))
                        .orElse(Outcome.draw(EndReason.TIME_LIMIT));
            }
            return soleLeader(game.players(), PlayerSession::score)
                    .map(player -> Outcome.player(player.id(), EndReason.TIME_LIMIT))
                    .orElse(Outcome.draw(EndReason.TIME_LIMIT));
        }

        @Override
        public List<Setting> settings() {
            return List.of(timeLimit);
        }

        /** The only one with the highest score, when that score is above 0. */
        private static <T> Optional<T> soleLeader(List<T> all, ToIntFunction<T> score) {
            int top = all.stream().mapToInt(score).max().orElse(0);
            List<T> leaders = all.stream().filter(item -> score.applyAsInt(item) == top).toList();
            return top > 0 && leaders.size() == 1 ? Optional.of(leaders.get(0)) : Optional.empty();
        }
    }

    private record FirstTo(IntSetting goal) implements WinCondition {

        @Override
        public Optional<Outcome> evaluate(Game game) {
            int target = game.settings().has(goal) ? game.settings().get(goal) : 0;
            if (target <= 0) {
                return Optional.empty();
            }
            if (!game.teams().isEmpty()) {
                return game.scores().teamRanking().stream()
                        .filter(team -> team.score() >= target)
                        .findFirst()
                        .map(team -> Outcome.team(team.id(), EndReason.WIN_CONDITION));
            }
            return game.scores().ranking().stream()
                    .filter(session -> session.present() && session.role() == Role.PLAYER)
                    .filter(session -> session.score() >= target)
                    .findFirst()
                    .map(session -> Outcome.player(session.id(), EndReason.WIN_CONDITION));
        }

        @Override
        public List<Setting> settings() {
            return List.of(goal);
        }
    }

    private record Either(WinCondition first, WinCondition second) implements WinCondition {

        @Override
        public Optional<Outcome> evaluate(Game game) {
            return first.evaluate(game).or(() -> second.evaluate(game));
        }

        @Override
        public boolean usesTimeLimit() {
            return first.usesTimeLimit() || second.usesTimeLimit();
        }

        @Override
        public Outcome timeUp(Game game) {
            return first.usesTimeLimit() ? first.timeUp(game) : second.timeUp(game);
        }

        /** The union, in {@link Setting#ALL} order (settings outside that list follow, in their own order). */
        @Override
        public List<Setting> settings() {
            List<Setting> union = new ArrayList<>(Setting.ALL.stream()
                    .filter(s -> first.settings().contains(s) || second.settings().contains(s))
                    .toList());
            Stream.concat(first.settings().stream(), second.settings().stream())
                    .filter(s -> !union.contains(s))
                    .forEach(union::add);
            return List.copyOf(union);
        }
    }
}
