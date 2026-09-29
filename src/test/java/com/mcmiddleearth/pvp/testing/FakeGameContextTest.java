package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.GameContextFactory;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.game.TaskHandle;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.team.Palette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeGameContextTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final FakeGameContext context = new FakeGameContext();

    @Test
    void theClockStartsAtNoonAndMovesFiftyMillisecondsPerTick() {
        assertEquals(Instant.parse("2026-09-24T12:00:00Z"), context.clock().instant());

        context.advanceTicks(1);
        assertEquals(Instant.parse("2026-09-24T12:00:00.050Z"), context.clock().instant());

        context.advanceSeconds(2);
        assertEquals(Instant.parse("2026-09-24T12:00:02.050Z"), context.clock().instant());
    }

    @Test
    void theCurrentTickCountsTheSimulatedTicks() {
        assertEquals(0, context.tasks().currentTick());

        context.advanceTicks(3);
        context.advanceSeconds(1);

        assertEquals(23, context.tasks().currentTick());
    }

    @Test
    void aRepeatingTaskFirstRunsOnePeriodFromNow() {
        AtomicInteger runs = new AtomicInteger();
        context.tasks().every(20, runs::incrementAndGet);

        context.advanceTicks(19);
        assertEquals(0, runs.get());
        context.advanceTicks(1);
        assertEquals(1, runs.get());
        context.advanceTicks(40);
        assertEquals(3, runs.get());
    }

    @Test
    void aOneShotTaskRunsOnceAndThenNoLongerCounts() {
        AtomicInteger runs = new AtomicInteger();
        context.tasks().after(5, runs::incrementAndGet);
        assertEquals(1, context.tasks().activeCount());

        context.advanceTicks(4);
        assertEquals(0, runs.get());
        context.advanceTicks(1);
        assertEquals(1, runs.get());
        context.advanceTicks(20);
        assertEquals(1, runs.get());
        assertEquals(0, context.tasks().activeCount());
    }

    @Test
    void cancellingAOneShotTaskThatRanChangesNothing() {
        TaskHandle handle = context.tasks().after(1, () -> { });
        context.advanceTicks(1);

        handle.cancel();

        assertFalse(handle.cancelled());
    }

    @Test
    void aCancelledTaskNeitherRunsNorCounts() {
        AtomicInteger runs = new AtomicInteger();
        TaskHandle repeating = context.tasks().every(1, runs::incrementAndGet);
        context.tasks().after(10, runs::incrementAndGet);
        assertEquals(2, context.tasks().activeCount());

        repeating.cancel();
        context.advanceTicks(5);

        assertTrue(repeating.cancelled());
        assertEquals(0, runs.get());
        assertEquals(1, context.tasks().activeCount());
    }

    @Test
    void cancelAllStopsEveryTask() {
        AtomicInteger runs = new AtomicInteger();
        TaskHandle first = context.tasks().every(20, runs::incrementAndGet);
        TaskHandle second = context.tasks().after(3, runs::incrementAndGet);

        context.tasks().cancelAll();
        context.advanceSeconds(2);

        assertEquals(0, runs.get());
        assertEquals(0, context.tasks().activeCount());
        assertTrue(first.cancelled());
        assertTrue(second.cancelled());
    }

    @Test
    void aTaskCanCancelEveryTaskWhileTheyRun() {
        AtomicInteger later = new AtomicInteger();
        context.tasks().every(1, () -> context.tasks().cancelAll());
        context.tasks().every(1, later::incrementAndGet);

        context.advanceTicks(3);

        assertEquals(0, later.get());
        assertEquals(0, context.tasks().activeCount());
    }

    @Test
    void aTaskCanScheduleAnotherTask() {
        AtomicInteger runs = new AtomicInteger();
        context.tasks().after(2, () -> context.tasks().after(1, runs::incrementAndGet));

        context.advanceTicks(2);
        assertEquals(0, runs.get());
        context.advanceTicks(1);
        assertEquals(1, runs.get());
    }

    @Test
    void schedulingIsLogged() {
        context.tasks().every(20, () -> { });
        context.tasks().after(5, () -> { });
        context.tasks().cancelAll();

        assertEquals(List.of("every 20", "after 5", "cancelAll"), context.log());
    }

    @Test
    void playerActionsAreRecorded() {
        NamedPoint point = NamedPoint.of("world", 1.5, 64, 2.5);
        Kit kit = new Kit("test", List.of());

        context.players().sendToLobby(PLAYER);
        context.players().teleport(PLAYER, point);
        context.players().giveKit(PLAYER, kit, Palette.RED);
        context.players().joinScoreboard(PLAYER, Palette.RED);
        context.players().setSpectator(PLAYER, true);

        assertEquals(point, context.positions().get(PLAYER));
        assertFalse(context.inLobby().contains(PLAYER));
        assertSame(kit, context.kits().get(PLAYER));
        assertEquals(Palette.RED, context.colors().get(PLAYER));
        assertTrue(context.onScoreboard().contains(PLAYER));
        assertTrue(context.spectating().contains(PLAYER));
        assertEquals(List.of("lobby " + PLAYER, "teleport " + PLAYER + " world 1.5 64.0 2.5",
                "kit " + PLAYER + " test red", "scoreboard " + PLAYER + " red", "spectator " + PLAYER + " true"),
                context.log());
    }

    @Test
    void theLobbyAndLeavingTheScoreboardUndoTheGameState() {
        context.players().joinScoreboard(PLAYER, Palette.BLUE);
        context.players().setSpectator(PLAYER, true);

        context.players().leaveScoreboard(PLAYER);
        context.players().sendToLobby(PLAYER);

        assertFalse(context.onScoreboard().contains(PLAYER));
        assertFalse(context.colors().containsKey(PLAYER));
        assertFalse(context.spectating().contains(PLAYER));
        assertTrue(context.inLobby().contains(PLAYER));
    }

    @Test
    void spectatingEndsWithSetSpectatorFalseOrAReset() {
        context.players().setSpectator(PLAYER, true);
        context.players().setSpectator(PLAYER, false);
        assertFalse(context.spectating().contains(PLAYER));

        context.players().setSpectator(PLAYER, true);
        context.players().resetForPlay(PLAYER);
        assertFalse(context.spectating().contains(PLAYER));
    }

    @Test
    void joiningTheScoreboardWithoutAColorClearsTheColor() {
        context.players().joinScoreboard(PLAYER, Palette.RED);
        context.players().joinScoreboard(PLAYER, null);

        assertTrue(context.onScoreboard().contains(PLAYER));
        assertFalse(context.colors().containsKey(PLAYER));
        assertEquals("scoreboard " + PLAYER + " none", context.log().get(1));
    }

    @Test
    void spawnedEntitiesAreTrackedUntilRemoved() {
        UUID first = context.world().spawnEntity("ZOMBIE", NamedPoint.of("world", 0, 64, 0));
        UUID second = context.world().spawnEntity("ZOMBIE", NamedPoint.of("world", 0, 64, 0));

        context.world().removeEntity(first);

        assertNotEquals(first, second);
        assertEquals(List.of(second), List.copyOf(context.spawnedEntities()));
        assertEquals(List.of("spawn ZOMBIE " + first, "spawn ZOMBIE " + second, "remove " + first), context.log());
    }

    @Test
    void disposeIsRecorded() {
        assertFalse(context.disposed());

        context.dispose();

        assertTrue(context.disposed());
        assertEquals(List.of("dispose"), context.log());
    }

    @Test
    void disposingWithTasksStillActiveIsRefused() {
        context.tasks().every(20, () -> { });

        assertThrows(IllegalStateException.class, context::dispose);

        context.tasks().cancelAll();
        context.dispose();
        assertTrue(context.disposed());
    }

    @Test
    void theFactoryAddsEveryContextItCreates() {
        List<FakeGameContext> created = new ArrayList<>();
        GameContextFactory factory = FakeGameContext.factory(created);

        // The fake ignores the arena.
        var first = factory.create(GameId.random(), null);
        var second = factory.create(GameId.random(), null);

        assertEquals(List.of(first, second), created);
    }

    @Test
    void aZeroDelayStillWaitsOneTick() {
        AtomicInteger runs = new AtomicInteger();
        context.tasks().after(0, runs::incrementAndGet);

        assertEquals(0, runs.get());
        context.advanceTicks(1);
        assertEquals(1, runs.get());
        context.advanceTicks(5);
        assertEquals(1, runs.get());
        assertEquals(0, context.tasks().activeCount());
    }

    @Test
    void aRepeatingTaskNeedsAPeriodOfAtLeastOneTick() {
        assertThrows(IllegalArgumentException.class, () -> context.tasks().every(0, () -> { }));
    }

    @Test
    void resettingOrSendingToTheLobbyEmptiesTheKit() {
        Kit kit = new Kit("test", List.of());
        context.players().giveKit(PLAYER, kit, Palette.RED);
        context.players().resetForPlay(PLAYER);
        assertFalse(context.kits().containsKey(PLAYER));

        context.players().giveKit(PLAYER, kit, Palette.RED);
        context.players().sendToLobby(PLAYER);
        assertFalse(context.kits().containsKey(PLAYER));
    }
}
