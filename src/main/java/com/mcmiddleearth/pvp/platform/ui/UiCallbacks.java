package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.UiAction;
import com.mcmiddleearth.pvp.ui.Viewer;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.List;

/**
 * Turns UI actions into server-side click callbacks. Buttons are callbacks, not hidden commands, so nothing
 * a client sends can name another game or setting. {@link PaperUiCallbacks} is the real one; the renderer tests
 * use a recording fake.
 */
public interface UiCallbacks {

    /** The click event of a chat button made for {@code viewer}. */
    ClickEvent<?> chat(Viewer viewer, UiAction action);

    /** The action of a dialog button; the action receives the values of {@code inputs} when it is clicked. */
    DialogAction dialog(Viewer viewer, UiAction action, List<Input> inputs);
}
