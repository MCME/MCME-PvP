package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.Objects;

/** A whole-number slider from {@code min} to {@code max} in steps of {@code step}, starting at {@code initial}. */
public record Slider(String key, Component label, int min, int max, int step, int initial) implements Input {

    public Slider {
        Input.checkKey(key);
        Objects.requireNonNull(label, "label");
        if (min >= max || step < 1 || initial < min || initial > max) {
            throw new IllegalArgumentException("slider " + key + ": " + min + ".." + max + " step " + step
                    + " initial " + initial);
        }
    }
}
