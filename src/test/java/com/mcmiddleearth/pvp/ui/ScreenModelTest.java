package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenModelTest {

    private static final Component LABEL = Component.text("label");
    private static final Button CLOSE = Button.of(Component.text("Close"), UiAction.none());

    @Test
    void aMenuNeedsAButtonAndAtLeastOneColumn() {
        Button join = Button.of(Component.text("Join"), UiAction.none());
        assertThrows(IllegalArgumentException.class,
                () -> new MenuScreen(LABEL, List.of(), List.of(), 1, CLOSE));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuScreen(LABEL, List.of(), List.of(join), 0, CLOSE));
        assertDoesNotThrow(() -> new MenuScreen(LABEL, List.of(), List.of(join), 2, CLOSE));
    }

    @Test
    void aSliderNeedsARangeAStepAndAnInitialValueInside() {
        assertDoesNotThrow(() -> new Slider("teamCount", LABEL, 2, 4, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 2, 2, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 4, 2, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 2, 4, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 2, 4, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 2, 4, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Slider("teamCount", LABEL, 2, 4, 1, 5));
    }

    @Test
    void inputKeysAreLettersDigitsAndUnderscores() {
        assertDoesNotThrow(() -> new Toggle("midgame_join2", LABEL, true));
        assertThrows(IllegalArgumentException.class, () -> new Toggle("midgame join", LABEL, true));
        assertThrows(IllegalArgumentException.class, () -> new Toggle("", LABEL, true));
    }

    @Test
    void aChoiceStartsOnOneOfItsOptions() {
        List<Choice.Option> options = List.of(new Choice.Option("a", LABEL), new Choice.Option("b", LABEL));
        assertEquals("b", new Choice("map", LABEL, options, "b").initial());
        assertThrows(IllegalArgumentException.class, () -> new Choice("map", LABEL, options, "c"));
        assertThrows(IllegalArgumentException.class, () -> new Choice("map", LABEL, List.of(), "a"));
        assertThrows(IllegalArgumentException.class, () -> new Choice("map", LABEL,
                List.of(new Choice.Option("a", LABEL), new Choice.Option("a", LABEL)), "a"));
    }

    @Test
    void aFormNeedsDistinctInputKeys() {
        Toggle first = new Toggle("midgameJoin", LABEL, true);
        Toggle duplicate = new Toggle("midgameJoin", LABEL, false);
        Toggle other = new Toggle("friendlyFire", LABEL, false);
        Button submit = Button.of(LABEL, UiAction.none());
        Button back = Button.of(LABEL, UiAction.none());
        assertThrows(IllegalArgumentException.class,
                () -> new FormScreen(LABEL, List.of(), List.of(first, duplicate), submit, back));
        assertDoesNotThrow(() -> new FormScreen(LABEL, List.of(), List.of(first, other), submit, back));
    }

    @Test
    void aChatMenuPageLiesWithinItsPages() {
        assertDoesNotThrow(() -> new ChatMenu(LABEL, List.of(), 2, 3, UiAction.none(), UiAction.none()));
        assertThrows(IllegalArgumentException.class, () -> new ChatMenu(LABEL, List.of(), 4, 3, null, null));
        assertThrows(IllegalArgumentException.class, () -> new ChatMenu(LABEL, List.of(), 0, 1, null, null));
        ChatMenu single = ChatMenu.of(null, List.of());
        assertEquals(1, single.pages());
    }

    @Test
    void listsAreCopiedSoAScreenCannotChangeAfterwards() {
        List<Component> body = new ArrayList<>(List.of(Component.text("line")));
        NoticeScreen notice = new NoticeScreen(LABEL, body, CLOSE);
        body.add(Component.text("added later"));
        assertEquals(1, notice.body().size());
        assertThrows(UnsupportedOperationException.class, () -> notice.body().add(LABEL));
    }

    @Test
    void anEmptyTooltipMeansNone() {
        assertFalse(Button.of(LABEL, UiAction.none()).hasTooltip());
        assertTrue(new Button(LABEL, Component.text("tip"), UiAction.none(), ButtonStyle.PRIMARY).hasTooltip());
    }
}
