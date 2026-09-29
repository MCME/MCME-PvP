package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.stats.MatchRecord;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Forwards every game notification to the listeners added, in the order they were added. A RuntimeException from
 * one listener is logged at SEVERE (with the event name) and the next listeners still run. Per listener and event,
 * the first failure is logged with its stack trace, then only every 100th one, as a count without a stack trace, so
 * a listener that breaks once a second (secondPassed) can't flood the log.
 */
public final class GameListeners implements GameListener {

    private static final int LOG_EVERY = 100;

    private final Logger logger;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<GameListener, Map<String, Integer>> failureCounts = new IdentityHashMap<>();

    public GameListeners(Logger logger) {
        this.logger = logger;
    }

    public void add(GameListener listener) {
        listeners.add(listener);
    }

    @Override
    public void gameOpened(Game game, String openedBy) {
        each("gameOpened", listener -> listener.gameOpened(game, openedBy));
    }

    @Override
    public void phaseChanged(Game game, Phase from, Phase to) {
        each("phaseChanged", listener -> listener.phaseChanged(game, from, to));
    }

    @Override
    public void playerJoined(Game game, PlayerSession session, JoinResult result) {
        each("playerJoined", listener -> listener.playerJoined(game, session, result));
    }

    @Override
    public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
        each("playerLeft", listener -> listener.playerLeft(game, session, reason));
    }

    @Override
    public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        each("playerDied", listener -> listener.playerDied(game, victim, killer));
    }

    /** Each listener hears it as a flee, or as a death when it does not tell the two apart. */
    @Override
    public void playerFled(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        each("playerFled", listener -> listener.playerFled(game, victim, killer));
    }

    @Override
    public void scoreChanged(Game game) {
        each("scoreChanged", listener -> listener.scoreChanged(game));
    }

    @Override
    public void secondPassed(Game game) {
        each("secondPassed", listener -> listener.secondPassed(game));
    }

    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        each("gameEnded", listener -> listener.gameEnded(game, outcome, record));
    }

    @Override
    public void gameClosed(Game game) {
        each("gameClosed", listener -> listener.gameClosed(game));
    }

    private void each(String event, Consumer<GameListener> call) {
        for (GameListener listener : listeners) {
            try {
                call.accept(listener);
            } catch (RuntimeException e) {
                onFailure(listener, event, e);
            }
        }
    }

    /**
     * The first failure for this listener and event is logged in full; after that, only every {@value #LOG_EVERY}th
     * one, as a count without a stack trace, so a listener that breaks on every {@code secondPassed} can't flood
     * the log.
     */
    private void onFailure(GameListener listener, String event, RuntimeException e) {
        int count = failureCounts.computeIfAbsent(listener, l -> new HashMap<>()).merge(event, 1, Integer::sum);
        String name = listener.getClass().getName();
        if (count == 1) {
            logger.log(Level.SEVERE, "Game listener " + name + " failed in " + event, e);
        } else if (count % LOG_EVERY == 0) {
            logger.log(Level.SEVERE,
                    "Game listener " + name + " failed " + count + " times in " + event + "; last: " + e.getMessage());
        }
    }
}
