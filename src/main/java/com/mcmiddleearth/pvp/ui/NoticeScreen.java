package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

/** Text with a single button, for rules, stats and scores. */
public record NoticeScreen(Component title, List<Component> body, Button ok) implements Screen {

    public NoticeScreen {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(ok, "ok");
        body = List.copyOf(body);
    }
}
