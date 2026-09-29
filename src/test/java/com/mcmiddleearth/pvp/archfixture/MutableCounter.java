package com.mcmiddleearth.pvp.archfixture;

/** Breaks the no-mutable-static rule on purpose, so ArchitectureTest can show that the rule fails. */
public final class MutableCounter {

    public static int count;

    private MutableCounter() {
    }
}
