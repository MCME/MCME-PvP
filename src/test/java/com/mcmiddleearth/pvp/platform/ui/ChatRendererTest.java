package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ButtonStyle;
import com.mcmiddleearth.pvp.ui.ChatLine;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.ChatPart;
import com.mcmiddleearth.pvp.ui.ConfirmScreen;
import com.mcmiddleearth.pvp.ui.FormScreen;
import com.mcmiddleearth.pvp.ui.FormValues;
import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.MenuScreen;
import com.mcmiddleearth.pvp.ui.NoticeScreen;
import com.mcmiddleearth.pvp.ui.Slider;
import com.mcmiddleearth.pvp.ui.Toggle;
import com.mcmiddleearth.pvp.ui.UiAction;
import com.mcmiddleearth.pvp.ui.Viewer;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRendererTest {

    private final RecordingCallbacks callbacks = new RecordingCallbacks();
    private final ChatRenderer renderer = new ChatRenderer(callbacks, Messages.bundled());
    private final Viewer alice = new Viewer(UUID.randomUUID(), "Alice", node -> true);
    private final List<String> ran = new ArrayList<>();

    private UiAction recording(String name) {
        return UiAction.of((viewer, values) -> ran.add(name + " " + values.values()));
    }

    @Test
    void everyButtonGetsItsOwnCallbackAndAHoverText() {
        UiAction join = recording("join");
        UiAction spectate = recording("spectate");
        ChatMenu menu = ChatMenu.of(text("PvP games"), List.of(ChatLine.of(
                ChatPart.text(text("AS-TDM")),
                ChatPart.link(new Button(text("Join"), text("Join AS-TDM"), join, ButtonStyle.PRIMARY)),
                ChatPart.link(Button.of(text("Spectate"), spectate)))));

        List<Component> lines = renderer.render(alice, menu);

        assertEquals(List.of("PvP games", "AS-TDM [Join] [Spectate]"), plain(lines));
        List<Component> links = links(lines.get(1));
        assertEquals(List.of(ClickEvent.runCommand("/ui-click 0"), ClickEvent.runCommand("/ui-click 1")),
                links.stream().map(Component::clickEvent).toList());
        assertEquals(HoverEvent.showText(text("Join AS-TDM")), links.get(0).hoverEvent());
        assertEquals(HoverEvent.showText(text("Spectate")), links.get(1).hoverEvent());
        assertEquals(List.of(join, spectate), callbacks.actions);
        assertEquals(List.of(alice.id(), alice.id()), callbacks.viewers);
    }

    @Test
    void buttonsWearTheThemeColorOfTheirStyle() {
        ChatMenu menu = ChatMenu.of(null, List.of(ChatLine.of(
                ChatPart.link(new Button(text("Join"), Component.empty(), recording("a"), ButtonStyle.PRIMARY)),
                ChatPart.link(Button.of(text("Spectate"), recording("b"))),
                ChatPart.link(new Button(text("Stop"), Component.empty(), recording("c"), ButtonStyle.DANGER)))));
        List<String> legacy = links(renderer.render(alice, menu).get(0)).stream()
                .map(link -> LegacyComponentSerializer.legacySection().serialize(link)).toList();
        assertEquals(List.of("§a[Join]", "§b[Spectate]", "§c[Stop]"), legacy);
    }

    @Test
    void aLongListGetsAPagingLine() {
        UiAction previous = recording("previous");
        UiAction next = recording("next");
        ChatMenu middle = new ChatMenu(text("PvP games"), List.of(ChatLine.of(ChatPart.text(text("a")))), 2, 3,
                previous, next);
        List<Component> lines = renderer.render(alice, middle);
        assertEquals("[‹ Prev] Page 2/3 [Next ›]", plain(lines.get(lines.size() - 1)));
        assertEquals(List.of(previous, next), callbacks.actions);

        ChatMenu first = new ChatMenu(text("PvP games"), List.of(), 1, 2, null, next);
        List<Component> firstLines = renderer.render(alice, first);
        assertEquals(List.of("PvP games", "Page 1/2 [Next ›]"), plain(firstLines));
    }

    @Test
    void aSinglePageWithoutHeaderIsJustItsLines() {
        ChatMenu row = ChatMenu.of(null, List.of(ChatLine.of(ChatPart.link(Button.of(text("Games"), recording("g"))))));
        assertEquals(List.of("[Games]"), plain(renderer.render(alice, row)));
    }

    @Test
    void aMenuScreenAsChatListsItsButtonsButNotItsCloseButton() {
        MenuScreen screen = new MenuScreen(text("PvP"), List.of(text("Pick a game.")),
                List.of(new Button(text("Join AS-TDM"), Component.empty(), recording("join"), ButtonStyle.PRIMARY),
                        Button.of(text("Rules"), recording("rules"))),
                1, Button.of(text("Close"), UiAction.none()));
        List<Component> lines = renderer.render(alice, screen);
        assertEquals(List.of("PvP", "Pick a game.", "[Join AS-TDM]", "[Rules]"), plain(lines));
        assertEquals(2, callbacks.actions.size());
    }

    @Test
    void aMenusBackButtonIsShown() {
        MenuScreen screen = new MenuScreen(text("PvP"), List.of(text("Pick a game.")),
                List.of(Button.of(text("Rules"), recording("rules"))),
                1, Button.of(text("Back"), UiAction.of((viewer, values) -> { })));
        List<Component> lines = renderer.render(alice, screen);
        assertEquals(List.of("PvP", "Pick a game.", "[Rules]", "[Back]"), plain(lines));
    }

    @Test
    void aFormAsChatShowsItsStartingValuesAndSubmitsThem() {
        List<Input> inputs = List.of(new Slider("timeLimit", text("Time limit"), 1, 60, 1, 10),
                new Toggle("midgameJoin", text("Late joins"), true));
        FormScreen form = new FormScreen(text("Free For All on AS-FFA"), List.of(text("Teams: 2")), inputs,
                new Button(text("Start"), Component.empty(), recording("start"), ButtonStyle.PRIMARY),
                Button.of(text("Back"), recording("back")));
        List<Component> lines = renderer.render(alice, form);
        assertEquals(List.of("Free For All on AS-FFA", "Teams: 2", "Time limit: 10", "Late joins: true",
                "[Start] [Back]"), plain(lines));
        assertTrue(callbacks.actions.get(0).attempt(alice, FormValues.empty()).isEmpty());
        assertEquals(List.of("start {timeLimit=10, midgameJoin=true}"), ran);
    }

    @Test
    void noticesAndQuestionsLeaveOutButtonsThatOnlyClose() {
        NoticeScreen notice = new NoticeScreen(text("Rules"), List.of(text("No respawns.")),
                Button.of(text("Close"), UiAction.none()));
        assertEquals(List.of("Rules", "No respawns."), plain(renderer.render(alice, notice)));
        ConfirmScreen question = new ConfirmScreen(text("Stop?"), List.of(text("Everyone leaves.")),
                new Button(text("Yes"), Component.empty(), recording("stop"), ButtonStyle.DANGER),
                Button.of(text("No"), UiAction.none()));
        assertEquals(List.of("Stop?", "Everyone leaves.", "[Yes]"), plain(renderer.render(alice, question)));
    }

    /** The components that carry a click event, outermost first. */
    private static List<Component> links(Component line) {
        List<Component> found = new ArrayList<>();
        collect(line, found);
        return found;
    }

    private static void collect(Component component, List<Component> found) {
        if (component.clickEvent() != null) {
            found.add(component);
            return;
        }
        component.children().forEach(child -> collect(child, found));
    }

    /** Gives each chat button "/ui-click <n>" and remembers its action, so the test can click it. */
    private static final class RecordingCallbacks implements UiCallbacks {

        private final List<UiAction> actions = new ArrayList<>();
        private final List<UUID> viewers = new ArrayList<>();

        @Override
        public ClickEvent<?> chat(Viewer viewer, UiAction action) {
            actions.add(action);
            viewers.add(viewer.id());
            return ClickEvent.runCommand("/ui-click " + (actions.size() - 1));
        }

        @Override
        public DialogAction dialog(Viewer viewer, UiAction action, List<Input> inputs) {
            throw new UnsupportedOperationException("the chat renderer never builds dialog buttons");
        }
    }
}
