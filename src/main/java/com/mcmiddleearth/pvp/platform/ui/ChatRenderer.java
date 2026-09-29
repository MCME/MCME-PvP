package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ChatLine;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.ChatPart;
import com.mcmiddleearth.pvp.ui.Choice;
import com.mcmiddleearth.pvp.ui.ConfirmScreen;
import com.mcmiddleearth.pvp.ui.FormScreen;
import com.mcmiddleearth.pvp.ui.FormValues;
import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.MenuScreen;
import com.mcmiddleearth.pvp.ui.NoticeScreen;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.Slider;
import com.mcmiddleearth.pvp.ui.Toggle;
import com.mcmiddleearth.pvp.ui.Viewer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chat menus with buttons, for lists, notifications and quick actions. Every button becomes a "[label]" in the
 * theme color of its style ({@code ui.button}, {@code ui.button-primary}, {@code ui.button-danger}), with a click
 * callback from {@link UiCallbacks} and a hover text (the tooltip, else the label). It also writes a dialog screen as
 * chat, for a server where dialogs are missing; a form then offers to submit its starting values.
 */
public final class ChatRenderer {

    private final UiCallbacks callbacks;
    private final Messages messages;

    public ChatRenderer(UiCallbacks callbacks, Messages messages) {
        this.callbacks = callbacks;
        this.messages = messages;
    }

    /** The header (if any), each line, and a paging line when there is more than one page. */
    public List<Component> render(Viewer viewer, ChatMenu menu) {
        List<Component> lines = new ArrayList<>();
        if (menu.header() != null) {
            lines.add(menu.header());
        }
        for (ChatLine line : menu.lines()) {
            lines.add(Component.join(JoinConfiguration.spaces(), line.parts().stream().map(part -> switch (part) {
                case ChatPart.Text text -> text.text();
                case ChatPart.Link linked -> link(viewer, linked.button());
            }).toList()));
        }
        if (menu.pages() > 1) {
            lines.add(pager(viewer, menu));
        }
        return lines;
    }

    /**
     * A dialog screen as chat: its title, its text, then its buttons. The bottom row (a menu's exit, a form's
     * Start and Back, Yes and No, OK) leaves out a button that only closes, since chat has nothing to close; a
     * menu's own buttons are all shown.
     */
    public List<Component> render(Viewer viewer, Screen screen) {
        List<Component> lines = new ArrayList<>();
        lines.add(messages.get(Msg.UI_HEADER, Placeholder.component("title", screen.title())));
        lines.addAll(screen.body());
        switch (screen) {
            case MenuScreen menu -> {
                menu.buttons().forEach(button -> lines.add(link(viewer, button)));
                row(viewer, List.of(menu.exit())).ifPresent(lines::add);
            }
            case FormScreen form -> {
                Map<String, Object> start = new LinkedHashMap<>();
                for (Input input : form.inputs()) {
                    Object value = startingValue(input);
                    start.put(input.key(), value);
                    lines.add(messages.get(Msg.UI_FORM_FIXED, Placeholder.component("setting", input.label()),
                            Placeholder.unparsed("value", String.valueOf(value))));
                }
                Button submit = form.submit();
                Button submitStart = new Button(submit.label(), submit.tooltip(),
                        submit.action().withValues(new FormValues(start)), submit.style());
                row(viewer, List.of(submitStart, form.back())).ifPresent(lines::add);
            }
            case ConfirmScreen confirm -> row(viewer, List.of(confirm.yes(), confirm.no())).ifPresent(lines::add);
            case NoticeScreen notice -> row(viewer, List.of(notice.ok())).ifPresent(lines::add);
        }
        return lines;
    }

    /** One button: "[label]" framed by its style, a callback for {@code viewer}, and a hover text. */
    private Component link(Viewer viewer, Button button) {
        String frame = switch (button.style()) {
            case NORMAL -> Msg.UI_BUTTON;
            case PRIMARY -> Msg.UI_BUTTON_PRIMARY;
            case DANGER -> Msg.UI_BUTTON_DANGER;
        };
        return messages.get(frame, Placeholder.component("label", button.label()))
                .clickEvent(callbacks.chat(viewer, button.action()))
                .hoverEvent(HoverEvent.showText(button.hasTooltip() ? button.tooltip() : button.label()));
    }

    private Optional<Component> row(Viewer viewer, List<Button> buttons) {
        List<Component> links = buttons.stream().filter(button -> !button.action().isNone())
                .map(button -> link(viewer, button)).toList();
        return links.isEmpty() ? Optional.empty() : Optional.of(Component.join(JoinConfiguration.spaces(), links));
    }

    private Component pager(Viewer viewer, ChatMenu menu) {
        List<Component> parts = new ArrayList<>();
        if (menu.previous() != null) {
            parts.add(link(viewer, Button.of(messages.get(Msg.UI_CHAT_PREV), menu.previous())));
        }
        parts.add(messages.get(Msg.UI_CHAT_PAGE,
                Map.of("page", String.valueOf(menu.page()), "pages", String.valueOf(menu.pages()))));
        if (menu.next() != null) {
            parts.add(link(viewer, Button.of(messages.get(Msg.UI_CHAT_NEXT), menu.next())));
        }
        return Component.join(JoinConfiguration.spaces(), parts);
    }

    private static Object startingValue(Input input) {
        return switch (input) {
            case Slider slider -> slider.initial();
            case Toggle toggle -> toggle.initial();
            case Choice choice -> choice.initial();
        };
    }
}
