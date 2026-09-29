package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.game.TaskHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperGameTasksTest {

    private BukkitSchedulerMock scheduler;
    private PaperGameTasks tasks;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        scheduler = server.getScheduler();
        tasks = new PaperGameTasks(MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void everyFirstRunsOnePeriodFromNow() {
        List<Long> runs = new ArrayList<>();
        tasks.every(20, () -> runs.add(scheduler.getCurrentTick()));

        scheduler.performTicks(19);
        assertEquals(List.of(), runs);
        scheduler.performTicks(21);
        assertEquals(List.of(20L, 40L), runs);
    }

    @Test
    void aRepeatingTaskNeedsAPeriodOfAtLeastOneTick() {
        assertThrows(IllegalArgumentException.class, () -> tasks.every(0, () -> { }));
    }

    @Test
    void aOneShotTaskStopsCountingOnceItRan() {
        AtomicInteger runs = new AtomicInteger();
        TaskHandle handle = tasks.after(10, runs::incrementAndGet);
        assertEquals(1, tasks.activeCount());

        scheduler.performTicks(9);
        assertEquals(0, runs.get());

        scheduler.performTicks(1);
        assertEquals(1, runs.get());
        assertEquals(0, tasks.activeCount());
        assertFalse(handle.cancelled());
    }

    @Test
    void cancellingAOneShotTaskThatRanChangesNothing() {
        TaskHandle handle = tasks.after(1, () -> { });
        scheduler.performTicks(1);

        handle.cancel();

        assertFalse(handle.cancelled());
    }

    @Test
    void cancelStopsOneTask() {
        AtomicInteger runs = new AtomicInteger();
        TaskHandle handle = tasks.every(5, runs::incrementAndGet);
        scheduler.performTicks(5);

        handle.cancel();
        scheduler.performTicks(20);

        assertTrue(handle.cancelled());
        assertEquals(1, runs.get());
        assertEquals(0, tasks.activeCount());
    }

    @Test
    void cancelAllLeavesNothingScheduled() {
        int before = scheduler.getPendingTasks().size();
        AtomicInteger runs = new AtomicInteger();
        tasks.every(20, runs::incrementAndGet);
        tasks.after(100, runs::incrementAndGet);
        assertEquals(2, tasks.activeCount());

        tasks.cancelAll();
        scheduler.performTicks(200);

        assertEquals(0, tasks.activeCount());
        assertEquals(before, scheduler.getPendingTasks().size());
        assertEquals(0, runs.get());
    }

    @Test
    void theCurrentTickIsTheServersTick() {
        scheduler.performTicks(3);
        long before = tasks.currentTick();

        scheduler.performTicks(5);

        assertEquals(3, before);
        assertEquals(before + 5, tasks.currentTick());
    }
}
