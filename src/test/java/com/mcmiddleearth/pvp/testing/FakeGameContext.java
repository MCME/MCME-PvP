package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.GameContext;
import com.mcmiddleearth.pvp.game.GameContextFactory;
import com.mcmiddleearth.pvp.game.GameTasks;
import com.mcmiddleearth.pvp.game.PlayerActions;
import com.mcmiddleearth.pvp.game.TaskHandle;
import com.mcmiddleearth.pvp.game.WorldActions;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A {@link GameContext} without a server: it records what the game asks for, runs tasks on a simulated tick clock
 * ({@link #advanceTicks}), and starts its clock at {@link #START}. One instance per game.
 */
public final class FakeGameContext implements GameContext {

    /** Where every fake clock starts. */
    public static final Instant START = Instant.parse("2026-09-24T12:00:00Z");

    private final List<String> log = new ArrayList<>();
    private final Map<UUID, NamedPoint> positions = new HashMap<>();
    private final Set<UUID> inLobby = new LinkedHashSet<>();
    private final Set<UUID> spectating = new LinkedHashSet<>();
    private final Map<UUID, Kit> kits = new HashMap<>();
    private final Map<UUID, TeamSpec> colors = new HashMap<>();
    private final Set<UUID> onScoreboard = new LinkedHashSet<>();
    private final Set<UUID> spawnedEntities = new LinkedHashSet<>();
    private final List<FakeTask> tasks = new ArrayList<>();
    private final TickClock clock = new TickClock();
    private final PlayerActions players = new Players();
    private final WorldActions world = new World();
    private final GameTasks gameTasks = new Tasks();
    private long tick;
    private boolean disposed;

    /** A factory for {@code GameManager} that adds every context it creates to {@code created}. */
    public static GameContextFactory factory(List<FakeGameContext> created) {
        return (id, arena) -> {
            FakeGameContext context = new FakeGameContext();
            created.add(context);
            return context;
        };
    }

    @Override
    public PlayerActions players() {
        return players;
    }

    @Override
    public GameTasks tasks() {
        return gameTasks;
    }

    @Override
    public WorldActions world() {
        return world;
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public void dispose() {
        int active = tasks().activeCount();
        if (active > 0) {
            throw new IllegalStateException("dispose() with " + active + " tasks still active; cancel them first");
        }
        disposed = true;
        log.add("dispose");
    }

    /** Runs due tasks tick by tick, in the order they were scheduled; the clock moves 50 ms per tick. */
    public void advanceTicks(long ticks) {
        for (long i = 0; i < ticks; i++) {
            tick++;
            clock.now = clock.now.plusMillis(50);
            for (FakeTask task : List.copyOf(tasks)) {
                if (!task.cancelled && !task.finished && task.nextTick == tick) {
                    task.run();
                }
            }
            tasks.removeIf(task -> task.cancelled || task.finished);
        }
    }

    public void advanceSeconds(int seconds) {
        advanceTicks(seconds * 20L);
    }

    /** Last teleport per player. */
    public Map<UUID, NamedPoint> positions() {
        return Collections.unmodifiableMap(positions);
    }

    /** Sent to the lobby and not teleported since. */
    public Set<UUID> inLobby() {
        return Collections.unmodifiableSet(inLobby);
    }

    /** setSpectator(true) and not set back (the lobby sets it back too). */
    public Set<UUID> spectating() {
        return Collections.unmodifiableSet(spectating);
    }

    /** The kit each player holds now: set by giveKit, cleared by resetForPlay and sendToLobby (both empty the inventory). */
    public Map<UUID, Kit> kits() {
        return Collections.unmodifiableMap(kits);
    }

    /** Last joinScoreboard color per player on the scoreboard (absent: none). */
    public Map<UUID, TeamSpec> colors() {
        return Collections.unmodifiableMap(colors);
    }

    /** Joined the game's scoreboard and not left. */
    public Set<UUID> onScoreboard() {
        return Collections.unmodifiableSet(onScoreboard);
    }

    /** Spawned and not removed. */
    public Set<UUID> spawnedEntities() {
        return Collections.unmodifiableSet(spawnedEntities);
    }

    public boolean disposed() {
        return disposed;
    }

    /** Every call, e.g. "teleport <uuid> <world> <x> <y> <z>", "kit <uuid> standard red", "every 20". */
    public List<String> log() {
        return Collections.unmodifiableList(log);
    }

    /** The log lines that start with {@code prefix}, e.g. "kit ". */
    public List<String> log(String prefix) {
        return log.stream().filter(line -> line.startsWith(prefix)).toList();
    }

    private static String id(TeamSpec color) {
        return color == null ? "none" : color.id();
    }

    private final class Players implements PlayerActions {

        @Override
        public void teleport(UUID player, NamedPoint point) {
            positions.put(player, point);
            inLobby.remove(player);
            log.add("teleport " + player + " " + point.world() + " " + point.x() + " " + point.y() + " " + point.z());
        }

        @Override
        public void resetForPlay(UUID player) {
            spectating.remove(player);
            kits.remove(player);
            log.add("reset " + player);
        }

        @Override
        public void giveKit(UUID player, Kit kit, TeamSpec color) {
            kits.put(player, kit);
            log.add("kit " + player + " " + kit.id() + " " + id(color));
        }

        @Override
        public void setSpectator(UUID player, boolean spectator) {
            if (spectator) {
                spectating.add(player);
            } else {
                spectating.remove(player);
            }
            log.add("spectator " + player + " " + spectator);
        }

        @Override
        public void joinScoreboard(UUID player, TeamSpec color) {
            onScoreboard.add(player);
            if (color == null) {
                colors.remove(player);
            } else {
                colors.put(player, color);
            }
            log.add("scoreboard " + player + " " + id(color));
        }

        @Override
        public void leaveScoreboard(UUID player) {
            onScoreboard.remove(player);
            colors.remove(player);
            log.add("unscoreboard " + player);
        }

        @Override
        public void sendToLobby(UUID player) {
            inLobby.add(player);
            spectating.remove(player);
            kits.remove(player);
            log.add("lobby " + player);
        }
    }

    private final class World implements WorldActions {

        @Override
        public UUID spawnEntity(String entityType, NamedPoint at) {
            UUID entity = UUID.randomUUID();
            spawnedEntities.add(entity);
            log.add("spawn " + entityType + " " + entity);
            return entity;
        }

        @Override
        public void removeEntity(UUID entity) {
            spawnedEntities.remove(entity);
            log.add("remove " + entity);
        }
    }

    private final class Tasks implements GameTasks {

        @Override
        public TaskHandle every(long periodTicks, Runnable task) {
            if (periodTicks < 1) {
                throw new IllegalArgumentException("period " + periodTicks);
            }
            log.add("every " + periodTicks);
            return schedule(periodTicks, periodTicks, task);
        }

        @Override
        public TaskHandle after(long delayTicks, Runnable task) {
            log.add("after " + delayTicks);
            return schedule(Math.max(1, delayTicks), 0, task);
        }

        @Override
        public void cancelAll() {
            log.add("cancelAll");
            tasks.forEach(FakeTask::cancel);
        }

        @Override
        public int activeCount() {
            return (int) tasks.stream().filter(task -> !task.cancelled && !task.finished).count();
        }

        @Override
        public long currentTick() {
            return tick;
        }

        private TaskHandle schedule(long delay, long period, Runnable body) {
            FakeTask task = new FakeTask(body, tick + delay, period);
            tasks.add(task);
            return task;
        }
    }

    private static final class FakeTask implements TaskHandle {

        private final Runnable body;
        private final long period;
        private long nextTick;
        private boolean cancelled;
        private boolean finished;

        private FakeTask(Runnable body, long nextTick, long period) {
            this.body = body;
            this.nextTick = nextTick;
            this.period = period;
        }

        private void run() {
            if (period > 0) {
                nextTick += period;
            } else {
                finished = true;
            }
            body.run();
        }

        @Override
        public void cancel() {
            if (finished) {
                return;
            }
            cancelled = true;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }
    }

    private static final class TickClock extends Clock {

        private Instant now = START;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
