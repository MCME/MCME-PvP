package com.mcmiddleearth.pvp.stats;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.PlayerSession;

/**
 * Records every finished game: the match record goes to the history, participants get their kills, deaths and
 * result, and spectators who did not take part get a spectated game. Fixes 1.1.2, which never counted Team
 * Deathmatch losses or any Free For All result.
 */
public final class StatsRecorder implements GameListener {

    private final StatsService stats;
    private final MatchHistoryStore history;

    public StatsRecorder(StatsService stats, MatchHistoryStore history) {
        this.stats = stats;
        this.history = history;
    }

    /**
     * Records the history and the stats independently, so a failure in one never costs the other. The first failure
     * is rethrown afterwards, with a second one added as suppressed, so {@link com.mcmiddleearth.pvp.game.GameListeners}
     * logs it.
     */
    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        RuntimeException failure = null;
        try {
            history.append(record);
        } catch (RuntimeException e) {
            failure = e;
        }
        try {
            stats.recordMatch(record);
            for (PlayerSession spectator : game.spectators()) {
                // A participant who came back to watch already counts as played (result left).
                if (!spectator.participated()) {
                    stats.recordSpectated(spectator.id(), spectator.name());
                }
            }
        } catch (RuntimeException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
