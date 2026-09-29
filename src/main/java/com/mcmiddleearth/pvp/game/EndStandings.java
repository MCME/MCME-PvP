package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.List;

/**
 * The standings as they were the moment a game ended: how many players it had, then each team with the players it
 * still had, or, in modes without teams, each participant by score, best first. {@link Game#endStandings()} keeps
 * this fixed once a game is ENDING, so a player who moves on to another game does not change what already happened.
 * Exactly one of {@link #teams()} and {@link #players()} is non-empty, matching whether the game's mode has teams.
 */
public record EndStandings(int playerCount, List<TeamStanding> teams, List<PlayerStanding> players) {

    public EndStandings {
        teams = List.copyOf(teams);
        players = List.copyOf(players);
    }

    /** A team and the players it had left when the game ended. */
    public record TeamStanding(TeamSpec spec, int aliveCount) {
    }

    /** A participant and their score when the game ended. */
    public record PlayerStanding(String name, int score) {
    }
}
