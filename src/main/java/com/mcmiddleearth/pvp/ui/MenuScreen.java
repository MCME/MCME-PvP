package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

/**
 * Buttons in a grid of {@code columns}. {@code exit} sits below them and also answers Escape: Close, or Back to the
 * previous screen. A dialog of this kind needs at least one button besides the exit.
 */
public record MenuScreen(Component title, List<Component> body, List<Button> buttons, int columns, Button exit)
        implements Screen {

    public MenuScreen {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(exit, "exit");
        body = List.copyOf(body);
        buttons = List.copyOf(buttons);
        if (buttons.isEmpty()) {
            throw new IllegalArgumentException("a menu needs at least one button");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("columns " + columns);
        }
    }
}
