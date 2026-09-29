package com.mcmiddleearth.pvp.game;

/** A sequence of steps that must all run even when one of them throws. */
final class Steps {

    private Steps() {
    }

    /** Runs one step; a failure is kept, the first one winning, instead of stopping the steps after it. */
    static RuntimeException attempt(RuntimeException failure, Runnable step) {
        try {
            step.run();
            return failure;
        } catch (RuntimeException e) {
            if (failure == null) {
                return e;
            }
            failure.addSuppressed(e);
            return failure;
        }
    }
}
