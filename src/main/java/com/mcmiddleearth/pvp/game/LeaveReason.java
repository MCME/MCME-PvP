package com.mcmiddleearth.pvp.game;

/**
 * Why a session ended before or when the game closed. {@code GAME_CLOSED} is also the reason of a player who
 * moved on to another game while theirs showed its result. {@code SHUTDOWN} is {@code GAME_CLOSED}'s counterpart
 * for a game that closed because the server is stopping, so the two can be told apart in chat.
 */
public enum LeaveReason {
    LEFT, QUIT, KICKED, GAME_CLOSED, SHUTDOWN;

    /** The game ended the session, not the player. */
    public boolean byGame() {
        return this == GAME_CLOSED || this == SHUTDOWN;
    }
}
