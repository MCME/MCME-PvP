package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

/** Inputs plus a submit button, whose action receives the submitted {@link FormValues}, and a back button. */
public record FormScreen(Component title, List<Component> body, List<Input> inputs, Button submit, Button back)
        implements Screen {

    public FormScreen {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(submit, "submit");
        Objects.requireNonNull(back, "back");
        body = List.copyOf(body);
        inputs = List.copyOf(inputs);
        if (inputs.stream().map(Input::key).distinct().count() != inputs.size()) {
            throw new IllegalArgumentException("duplicate input keys");
        }
    }
}
