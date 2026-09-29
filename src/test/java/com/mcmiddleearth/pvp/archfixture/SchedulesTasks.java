package com.mcmiddleearth.pvp.archfixture;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/** Uses the Bukkit scheduler on purpose, so ArchitectureTest can show that the rule fails. */
public final class SchedulesTasks {

    private SchedulesTasks() {
    }

    public static void cancelAll(BukkitScheduler scheduler, Plugin plugin) {
        scheduler.cancelTasks(plugin);
    }
}
