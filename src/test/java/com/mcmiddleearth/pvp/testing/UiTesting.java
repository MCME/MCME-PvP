package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ChatLine;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.ChatPart;
import com.mcmiddleearth.pvp.ui.ConfirmScreen;
import com.mcmiddleearth.pvp.ui.FormScreen;
import com.mcmiddleearth.pvp.ui.FormValues;
import com.mcmiddleearth.pvp.ui.MenuScreen;
import com.mcmiddleearth.pvp.ui.NoticeScreen;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.Viewer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reading and clicking screens and chat menus in tests, the way a player would. */
public final class UiTesting {

    private UiTesting() {
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static List<String> plain(List<Component> components) {
        return components.stream().map(UiTesting::plain).toList();
    }

    /** Every button of a screen: a menu's buttons then its exit; submit and back; yes and no; ok. */
    public static List<Button> buttons(Screen screen) {
        return switch (screen) {
            case MenuScreen menu -> {
                List<Button> all = new ArrayList<>(menu.buttons());
                all.add(menu.exit());
                yield all;
            }
            case FormScreen form -> List.of(form.submit(), form.back());
            case ConfirmScreen confirm -> List.of(confirm.yes(), confirm.no());
            case NoticeScreen notice -> List.of(notice.ok());
        };
    }

    public static List<String> labels(Screen screen) {
        return buttons(screen).stream().map(button -> plain(button.label())).toList();
    }

    /** The only button of the screen whose label starts with {@code start}. */
    public static Button button(Screen screen, String start) {
        return only(buttons(screen), start, labels(screen));
    }

    /** Every button in a chat menu, line by line. */
    public static List<Button> links(ChatMenu menu) {
        return menu.lines().stream().flatMap(line -> links(line).stream()).toList();
    }

    public static List<Button> links(ChatLine line) {
        return line.parts().stream().filter(ChatPart.Link.class::isInstance)
                .map(part -> ((ChatPart.Link) part).button()).toList();
    }

    public static List<String> linkLabels(ChatLine line) {
        return links(line).stream().map(button -> plain(button.label())).toList();
    }

    /** The only button of one chat line whose label starts with {@code start}. */
    public static Button link(ChatLine line, String start) {
        return only(links(line), start, linkLabels(line));
    }

    /** A chat line as a player reads it: text as is, buttons as "[label]". */
    public static String text(ChatLine line) {
        return String.join(" ", line.parts().stream().map(part -> switch (part) {
            case ChatPart.Text text -> plain(text.text());
            case ChatPart.Link link -> "[" + plain(link.button().label()) + "]";
        }).toList());
    }

    /** The click event of the first button in a rendered chat line, searched depth first. */
    public static Optional<ClickEvent<?>> firstClick(Component line) {
        if (line.clickEvent() != null) {
            return Optional.of(line.clickEvent());
        }
        for (Component child : line.children()) {
            Optional<ClickEvent<?>> found = firstClick(child);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Clicks a button without form values; returns the refusal, if any. */
    public static Optional<PvpException> click(Button button, Viewer viewer) {
        return button.action().attempt(viewer, FormValues.empty());
    }

    /** Submits a form with the given values; returns the refusal, if any. */
    public static Optional<PvpException> submit(FormScreen form, Viewer viewer, Map<String, ?> values) {
        return form.submit().action().attempt(viewer, new FormValues(new LinkedHashMap<String, Object>(values)));
    }

    private static Button only(List<Button> buttons, String start, List<String> labels) {
        List<Button> matches = buttons.stream().filter(button -> plain(button.label()).startsWith(start)).toList();
        if (matches.size() != 1) {
            throw new AssertionError(matches.size() + " buttons start with \"" + start + "\" in " + labels);
        }
        return matches.get(0);
    }
}
