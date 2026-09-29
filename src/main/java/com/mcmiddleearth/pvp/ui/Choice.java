package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

/** One option out of a list; the submitted value is the id of the chosen option. */
public record Choice(String key, Component label, List<Option> options, String initial) implements Input {

    /** An option: {@code id} is what the form submits, {@code label} what the player sees. */
    public record Option(String id, Component label) {

        public Option {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
        }
    }

    public Choice {
        Input.checkKey(key);
        Objects.requireNonNull(label, "label");
        options = List.copyOf(options);
        if (options.stream().map(Option::id).distinct().count() != options.size()) {
            throw new IllegalArgumentException("choice " + key + ": duplicate option ids");
        }
        if (options.stream().noneMatch(option -> option.id().equals(initial))) {
            throw new IllegalArgumentException("choice " + key + ": initial " + initial + " is not an option");
        }
    }
}
