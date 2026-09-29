package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

/**
 * A form field. {@code key} names its value in {@link FormValues} (in the Start game form: a setting id such as
 * "teamCount"). Keys are letters, digits and underscores, the characters dialog inputs accept.
 */
public sealed interface Input permits Slider, Toggle, Choice {

    String key();

    Component label();

    /** Returns {@code key}, or throws when dialogs would not accept it; the input records call it. */
    static String checkKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid input key: " + key);
        }
        return key;
    }
}
