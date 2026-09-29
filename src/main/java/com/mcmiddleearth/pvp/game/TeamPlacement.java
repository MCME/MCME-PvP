package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.mode.rules.TeamBalancer;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/** Puts players on teams, or gives them personal colors in modes without teams. */
final class TeamPlacement {

    private final List<GameTeam> teams;
    private final boolean balanced;
    private final ToDoubleFunction<UUID> skillOf;
    private int nextColor;

    TeamPlacement(List<GameTeam> teams, boolean balanced, ToDoubleFunction<UUID> skillOf) {
        this.teams = teams;
        this.balanced = balanced;
        this.skillOf = skillOf;
    }

    /**
     * At the countdown: every unit on one team (balanced by size and skill, or round-robin), then each team's
     * starting size. Without teams, each player gets the next personal color, in join order.
     */
    void placeAll(List<List<PlayerSession>> units, List<PlayerSession> players) {
        if (teams.isEmpty()) {
            players.forEach(session -> session.personalColor(nextPersonalColor()));
            return;
        }
        List<TeamBalancer.Unit> weights = units.stream()
                .map(unit -> new TeamBalancer.Unit(unit.size(),
                        unit.stream().mapToDouble(session -> skillOf.applyAsDouble(session.id())).average().orElse(0)))
                .toList();
        int[] placed = balanced
                ? TeamBalancer.assign(weights, teams.size())
                : TeamBalancer.roundRobin(weights, teams.size());
        for (int i = 0; i < units.size(); i++) {
            GameTeam team = teams.get(placed[i]);
            units.get(i).forEach(session -> session.joinTeam(team));
        }
        teams.forEach(team -> team.startingSize(team.members().size()));
    }

    /** The team a unit joining mid-game plays on: the fewest alive players, ties in palette order. */
    Optional<GameTeam> midgameTeam() {
        if (teams.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(teams.get(TeamBalancer.smallest(teams.stream().map(GameTeam::aliveCount).toList())));
    }

    /** 1.1.2's Free For All rotation of personal colors. */
    TeamSpec nextPersonalColor() {
        TeamSpec color = Palette.PERSONAL.get(nextColor % Palette.PERSONAL.size());
        nextColor++;
        return color;
    }

    /** Back to WAITING: nobody has a team, a color or took part yet, and the color rotation starts over. */
    void clear(List<PlayerSession> sessions) {
        sessions.forEach(session -> {
            session.clearPlacement();
            session.clearParticipation();
        });
        teams.forEach(team -> team.startingSize(0));
        nextColor = 0;
    }
}
