package com.mcmiddleearth.pvp.archfixture;

import org.bukkit.Bukkit;

/** Uses the static Bukkit server on purpose, so ArchitectureTest can show that the rule fails. */
public final class CallsTheServer {

    private CallsTheServer() {
    }

    public static String serverName() {
        return Bukkit.getServer().getName();
    }
}
