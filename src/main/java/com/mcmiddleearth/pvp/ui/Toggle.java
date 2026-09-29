package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.Objects;

/** An on/off switch. */
public record Toggle(String key, Component label, boolean initial) implements Input {

    public Toggle {
        Input.checkKey(key);
        Objects.requireNonNull(label, "label");
    }
}
