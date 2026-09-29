package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Chat lines with inline buttons: one page of a list, a notification or a row of quick actions. {@code header} is
 * null for a bare row of buttons; {@code previous} and {@code next} turn the page and are null on the first and on
 * the last page.
 */
public record ChatMenu(Component header, List<ChatLine> lines, int page, int pages, UiAction previous, UiAction next) {

    public ChatMenu {
        lines = List.copyOf(lines);
        if (pages < 1 || page < 1 || page > pages) {
            throw new IllegalArgumentException("page " + page + " of " + pages);
        }
    }

    /** A single page without paging buttons; {@code header} may be null. */
    public static ChatMenu of(Component header, List<ChatLine> lines) {
        return new ChatMenu(header, lines, 1, 1, null, null);
    }
}
