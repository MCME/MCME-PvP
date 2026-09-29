package com.mcmiddleearth.pvp.archfixture;

import io.papermc.paper.registry.RegistryKey;

/** Uses a Paper registry type on purpose, so ArchitectureTest can show that the rule fails. */
public final class ReadsRegistries {

    private ReadsRegistries() {
    }

    public static String describe(RegistryKey<?> key) {
        return key.toString();
    }
}
