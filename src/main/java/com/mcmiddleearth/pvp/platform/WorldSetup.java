package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.config.PvpConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Prepares the worlds of config.yml at startup: loads them, and switches immediate respawn on in each. */
public final class WorldSetup {

    private WorldSetup() {
    }

    /** Loads every configured world that is not loaded yet and sets {@code immediate_respawn} in all of them. */
    public static void apply(PvpConfig config, Logger logger) {
        apply(config, logger, name -> Bukkit.createWorld(new WorldCreator(name)));
    }

    /** {@link #apply(PvpConfig, Logger)} with the world loader replaced, for tests. */
    static void apply(PvpConfig config, Logger logger, Function<String, World> loader) {
        for (String name : config.worlds()) {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                world = load(name, logger, loader);
            }
            if (world == null) {
                logger.warning("World " + name + " from config.yml (worlds) could not be loaded;"
                        + " its arenas cannot be played");
                continue;
            }
            // 1.1.2 forced immediate respawn: the dead go straight back (TDM: to spectate, FFA: to the next spawn).
            world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        }
    }

    private static World load(String name, Logger logger, Function<String, World> loader) {
        try {
            World world = loader.apply(name);
            if (world != null) {
                logger.info("Loaded world " + name);
            }
            return world;
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Loading world " + name + " failed: " + e, e);
            return null;
        }
    }
}
