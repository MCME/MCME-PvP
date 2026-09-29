package com.mcmiddleearth.pvp.ui;

import java.util.List;

/** One chat line: text and buttons, which the renderer joins with spaces. */
public record ChatLine(List<ChatPart> parts) {

    public ChatLine {
        parts = List.copyOf(parts);
    }

    public static ChatLine of(ChatPart... parts) {
        return new ChatLine(List.of(parts));
    }
}
