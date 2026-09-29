package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.game.GameContext;
import com.mcmiddleearth.pvp.game.GameTasks;
import com.mcmiddleearth.pvp.game.PlayerActions;
import com.mcmiddleearth.pvp.game.WorldActions;

import java.time.Clock;

/** Everything one game uses from Paper; created by {@link PaperGameContextFactory}. */
public final class PaperGameContext implements GameContext {

    private final PlayerActions players;
    private final GameTasks tasks;
    private final WorldActions world;
    private final Runnable onDispose;

    PaperGameContext(PlayerActions players, GameTasks tasks, WorldActions world, Runnable onDispose) {
        this.players = players;
        this.tasks = tasks;
        this.world = world;
        this.onDispose = onDispose;
    }

    @Override
    public PlayerActions players() {
        return players;
    }

    @Override
    public GameTasks tasks() {
        return tasks;
    }

    @Override
    public WorldActions world() {
        return world;
    }

    @Override
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Also cancels any task the game still has, so a missed cancel can't leave a heartbeat running. */
    @Override
    public void dispose() {
        tasks.cancelAll();
        onDispose.run();
    }
}
