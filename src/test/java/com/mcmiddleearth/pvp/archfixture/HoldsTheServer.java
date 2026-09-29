package com.mcmiddleearth.pvp.archfixture;

import org.bukkit.Server;

/** Holds an org.bukkit.Server field on purpose, so ArchitectureTest can show that the rule fails. */
public final class HoldsTheServer {

    private final Server server;

    public HoldsTheServer(Server server) {
        this.server = server;
    }
}
