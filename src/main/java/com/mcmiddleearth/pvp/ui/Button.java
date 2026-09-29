package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.Objects;

/** A clickable button. An empty {@code tooltip} ({@link Component#empty()}) means none; chat then shows the label. */
public record Button(Component label, Component tooltip, UiAction action, ButtonStyle style) {

    public Button {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(style, "style");
    }

    /** A plain button without a tooltip. */
    public static Button of(Component label, UiAction action) {
        return new Button(label, Component.empty(), action, ButtonStyle.NORMAL);
    }

    public boolean hasTooltip() {
        return !Component.empty().equals(tooltip);
    }
}
