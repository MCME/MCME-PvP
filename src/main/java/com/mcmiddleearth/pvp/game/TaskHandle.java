package com.mcmiddleearth.pvp.game;

/** A scheduled task owned by one game. */
public interface TaskHandle {

    /** Stops the task. Calling it again, or after a one-shot task has run, does nothing. */
    void cancel();

    boolean cancelled();
}
