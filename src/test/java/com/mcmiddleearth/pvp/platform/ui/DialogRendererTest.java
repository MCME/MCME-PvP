package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ButtonStyle;
import com.mcmiddleearth.pvp.ui.UiAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * {@link DialogRenderer} itself cannot build a dialog in tests (no dialog provider there; see {@code supported()}),
 * so only its label coloring is checked here directly; it needs no callbacks.
 */
class DialogRendererTest {

    private final DialogRenderer renderer = new DialogRenderer(null, Messages.bundled());

    @Test
    void aDangerButtonsLabelIsColoredWithTheChatButtonsDangerColor() {
        Button danger = new Button(Component.text("Yes"), Component.empty(), UiAction.none(), ButtonStyle.DANGER);

        assertEquals(NamedTextColor.RED, renderer.label(danger).color());
    }

    @Test
    void aNormalOrPrimaryButtonsLabelIsUnchanged() {
        Button normal = Button.of(Component.text("No"), UiAction.none());
        Button primary = new Button(Component.text("Start"), Component.empty(), UiAction.none(), ButtonStyle.PRIMARY);

        assertEquals(normal.label(), renderer.label(normal));
        assertNotEquals(NamedTextColor.RED, renderer.label(primary).color());
        assertEquals(primary.label(), renderer.label(primary));
    }
}
