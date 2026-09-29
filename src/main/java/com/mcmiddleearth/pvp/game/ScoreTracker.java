package com.mcmiddleearth.pvp.game;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The scores of one game's players and teams, and the rankings the end summary shows. Mode hooks change scores
 * here; the declared win condition then decides whether the game is over.
 */
public final class ScoreTracker {

    private static final Comparator<PlayerSession> BY_NAME =
            Comparator.comparing(PlayerSession::name, String.CASE_INSENSITIVE_ORDER);

    private final Game game;
    private final Lifecycle lifecycle;

    ScoreTracker(Game game, Lifecycle lifecycle) {
        this.game = game;
        this.lifecycle = lifecycle;
    }

    /** Adds points to the player and to their team, fires scoreChanged and checks the win condition. */
    public void addScore(PlayerSession player, int points) {
        player.addScore(points);
        player.team().ifPresent(team -> team.addScore(points));
        lifecycle.scoreChanged();
    }

    /** Adds points to the team alone, fires scoreChanged and checks the win condition. */
    public void addTeamScore(GameTeam team, int points) {
        team.addScore(points);
        lifecycle.scoreChanged();
    }

    /** Participants: score desc, kills desc, deaths asc, then name. */
    public List<PlayerSession> ranking() {
        return participants()
                .sorted(Comparator.comparingInt(PlayerSession::score).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerSession::kills).reversed())
                        .thenComparingInt(PlayerSession::deaths)
                        .thenComparing(BY_NAME))
                .toList();
    }

    /** Teams by score, highest first; equal scores keep palette order. */
    public List<GameTeam> teamRanking() {
        return game.teams().stream().sorted(Comparator.comparingInt(GameTeam::score).reversed()).toList();
    }

    /** Participants with the most kills; ties by name. */
    public List<PlayerSession> mostKills(int limit) {
        return participants()
                .sorted(Comparator.comparingInt(PlayerSession::kills).reversed().thenComparing(BY_NAME))
                .limit(limit)
                .toList();
    }

    /** Participants with the best kills per death (kills when there are no deaths); ties by name. */
    public List<PlayerSession> bestKillDeath(int limit) {
        return participants()
                .sorted(Comparator.comparingDouble(PlayerSession::killDeathRatio).reversed().thenComparing(BY_NAME))
                .limit(limit)
                .toList();
    }

    /** Participants with the most deaths; ties by name. */
    public List<PlayerSession> mostDeaths(int limit) {
        return participants()
                .sorted(Comparator.comparingInt(PlayerSession::deaths).reversed().thenComparing(BY_NAME))
                .limit(limit)
                .toList();
    }

    private Stream<PlayerSession> participants() {
        return game.sessions().stream().filter(PlayerSession::participated);
    }
}
