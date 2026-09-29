package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

/** A yes/no question before a destructive action (see {@link ConfirmView}). */
public record ConfirmScreen(Component title, List<Component> body, Button yes, Button no) implements Screen {

    public ConfirmScreen {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(yes, "yes");
        Objects.requireNonNull(no, "no");
        body = List.copyOf(body);
    }
}
