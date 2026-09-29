package com.mcmiddleearth.pvp.game;

import java.time.Clock;

/**
 * Everything a game may use from the server. The Paper version is in {@code platform}; tests use a fake. Every call
 * happens on the server thread, and every task runs there.
 */
public interface GameContext {

    PlayerActions players();

    GameTasks tasks();

    WorldActions world();

    Clock clock();

    /** Releases what the server holds for this game (for example its scoreboard). Called once, at close, after tasks are cancelled. */
    void dispose();
}
