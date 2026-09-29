package com.mcmiddleearth.pvp.stats;

import java.util.List;
import java.util.UUID;

/**
 * A player's lifetime PvP stats, stored as {@code players/<uuid>.json}.
 * {@code playersKilled} is 1.1.2's list of distinct victim names: imported and kept, but 3.0 no longer adds to it.
 */
public record PlayerStats(int schemaVersion, UUID id, String name, int kills, int deaths, int gamesPlayed,
                          int gamesWon, int gamesLost, int gamesSpectated, List<String> playersKilled) {

    public static final int SCHEMA_VERSION = 1;

    public PlayerStats {
        playersKilled = List.copyOf(playersKilled);
    }

    public static PlayerStats empty(UUID id, String name) {
        return new PlayerStats(SCHEMA_VERSION, id, name, 0, 0, 0, 0, 0, 0, List.of());
    }

    /** Kills per death, as 1.1.2 showed it: the kills themselves when there are no deaths. */
    public double killDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    /** These stats plus one finished game. */
    public PlayerStats plus(MatchRecord.Participant game) {
        return new PlayerStats(SCHEMA_VERSION, id, game.name(), kills + game.kills(), deaths + game.deaths(),
                gamesPlayed + 1,
                gamesWon + (MatchRecord.WON.equals(game.result()) ? 1 : 0),
                gamesLost + (MatchRecord.LOST.equals(game.result()) ? 1 : 0),
                gamesSpectated, playersKilled);
    }

    /** These stats plus one game watched as a spectator. */
    public PlayerStats plusSpectated(String currentName) {
        return new PlayerStats(SCHEMA_VERSION, id, currentName, kills, deaths, gamesPlayed, gamesWon, gamesLost,
                gamesSpectated + 1, playersKilled);
    }
}
