package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.Objects;

/** A piece of a chat line: plain text, or a button. */
public sealed interface ChatPart {

    record Text(Component text) implements ChatPart {

        public Text {
            Objects.requireNonNull(text, "text");
        }
    }

    record Link(Button button) implements ChatPart {

        public Link {
            Objects.requireNonNull(button, "button");
        }
    }

    static ChatPart text(Component text) {
        return new Text(text);
    }

    static ChatPart link(Button button) {
        return new Link(button);
    }
}
