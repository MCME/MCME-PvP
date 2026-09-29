package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.game.GameTasks;
import com.mcmiddleearth.pvp.game.TaskHandle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The tasks of one game, as synchronous Bukkit tasks on the server thread (so game code needs no locking).
 * The game cancels them all when it closes.
 */
public final class PaperGameTasks implements GameTasks {

    private final Plugin plugin;
    private final List<Handle> handles = new ArrayList<>();

    public PaperGameTasks(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle every(long periodTicks, Runnable task) {
        if (periodTicks < 1) {
            throw new IllegalArgumentException("period " + periodTicks);
        }
        Handle handle = new Handle();
        handle.task = plugin.getServer().getScheduler().runTaskTimer(plugin, task, periodTicks, periodTicks);
        return track(handle);
    }

    @Override
    public TaskHandle after(long delayTicks, Runnable task) {
        Handle handle = new Handle();
        // Paper runs a 0-tick task scheduled from inside a running task in the same tick; GameTasks promises it
        // never runs in the tick that scheduled it.
        handle.task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            handle.finished = true;
            task.run();
        }, Math.max(1, delayTicks));
        return track(handle);
    }

    @Override
    public void cancelAll() {
        List.copyOf(handles).forEach(Handle::cancel);
        handles.clear();
    }

    @Override
    public int activeCount() {
        prune();
        return handles.size();
    }

    /** The server's tick counter, which the scheduler runs these tasks by. */
    @Override
    public long currentTick() {
        return plugin.getServer().getCurrentTick();
    }

    private TaskHandle track(Handle handle) {
        // Finished one-shot tasks (one per respawn) would otherwise pile up over a long game.
        prune();
        handles.add(handle);
        return handle;
    }

    private void prune() {
        handles.removeIf(handle -> handle.cancelled || handle.finished);
    }

    /** A game task; {@code finished} is set when a one-shot task has run. */
    private static final class Handle implements TaskHandle {

        private BukkitTask task;
        private boolean cancelled;
        private boolean finished;

        @Override
        public void cancel() {
            if (finished) {
                return;
            }
            cancelled = true;
            task.cancel();
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }
    }
}
