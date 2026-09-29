package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.stats.MatchRecord;

import java.util.Optional;

/**
 * Notifications about games, on the server thread. Chat, HUD, menus and stats listen here, so the engine never
 * formats text. Every method has an empty default, except {@link #playerFled}, which by default is a death.
 *
 * <p>Listeners must not change the engine from inside a notification (join, leave, start, stop or close a game):
 * the engine is between steps then. Schedule such a change for the next tick.
 */
public interface GameListener {

    /** A game was opened; {@code openedBy} is the staff member's name, or "Console". */
    default void gameOpened(Game game, String openedBy) {
    }

    default void phaseChanged(Game game, Phase from, Phase to) {
    }

    default void playerJoined(Game game, PlayerSession session, JoinResult result) {
    }

    default void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
    }

    default void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
    }

    /**
     * A player left or quit a running game soon after another player hurt them, which counts as a death, and as a
     * kill for {@code killer} when that player earns it. It is counted before the player leaves, in place of
     * {@link #playerDied}; a listener that does not tell the two apart hears it as {@code playerDied}.
     */
    default void playerFled(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        playerDied(game, victim, killer);
    }

    /** Kills, deaths, scores or alive counts changed. */
    default void scoreChanged(Game game) {
    }

    /** Once a second while the game is open, after the game's own work for that second. */
    default void secondPassed(Game game) {
    }

    /** The game entered ENDING; {@code record} is the match record that is being written. */
    default void gameEnded(Game game, Outcome outcome, MatchRecord record) {
    }

    /**
     * The game is CLOSED. The manager forgets it right after this call, so lookups made from inside
     * still find it; reopen its arena later, not from here.
     */
    default void gameClosed(Game game) {
    }
}
