package com.mcmiddleearth.pvp.archfixture;

import org.bukkit.scheduler.BukkitRunnable;

/** Extends BukkitRunnable on purpose, so ArchitectureTest can show that the rule fails. */
public final class ExtendsBukkitRunnable extends BukkitRunnable {

    @Override
    public void run() {
    }
}
