package com.mcmiddleearth.pvp.game;

/** The scheduler of one game. Every task is cancelled when the game closes. 20 ticks = 1 second. */
public interface GameTasks {

    /**
     * Runs {@code task} every {@code periodTicks}; the first run is one period from now.
     *
     * @throws IllegalArgumentException when {@code periodTicks} is below 1
     */
    TaskHandle every(long periodTicks, Runnable task);

    /**
     * Runs {@code task} once, {@code delayTicks} from now. A delay below 1 counts as 1, so a task never runs in the
     * tick that scheduled it.
     */
    TaskHandle after(long delayTicks, Runnable task);

    void cancelAll();

    /** Tasks not yet cancelled and not yet finished (a finished one-shot task no longer counts). */
    int activeCount();

    /**
     * The tick these tasks run on now. It goes up by one every tick, so two readings tell how many ticks lie
     * between them.
     */
    long currentTick();
}
