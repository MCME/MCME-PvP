package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ButtonStyle;
import com.mcmiddleearth.pvp.ui.Choice;
import com.mcmiddleearth.pvp.ui.ConfirmScreen;
import com.mcmiddleearth.pvp.ui.FormScreen;
import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.MenuScreen;
import com.mcmiddleearth.pvp.ui.NoticeScreen;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.Slider;
import com.mcmiddleearth.pvp.ui.Toggle;
import com.mcmiddleearth.pvp.ui.Viewer;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Builds a Paper dialog from a screen, fresh for every opening: dialogs are not registered at bootstrap, because
 * their content is live. Every button gets a callback from {@link UiCallbacks}, the closing ones too, so the dialog
 * tracker learns whenever a PvP dialog closes. Paper's dialog builders cannot run in unit tests (no dialog provider
 * there), so this class is checked in game, and {@link #supported()} lets the service fall back to chat.
 */
public final class DialogRenderer {

    private static final int WIDE = 300;
    private static final int MEDIUM = 200;
    private static final int NARROW = 150;

    private final UiCallbacks callbacks;
    private final Messages messages;
    private final boolean supported;

    public DialogRenderer(UiCallbacks callbacks, Messages messages) {
        this.callbacks = callbacks;
        this.messages = messages;
        this.supported = probe();
    }

    /** True on a Paper server; false where the dialog API has no implementation, as in unit tests. */
    public boolean supported() {
        return supported;
    }

    private static boolean probe() {
        try {
            DialogBody.plainMessage(Component.empty());
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public Dialog render(Viewer viewer, Screen screen) {
        List<DialogInput> inputs = screen instanceof FormScreen form
                ? form.inputs().stream().map(DialogRenderer::input).toList()
                : List.of();
        DialogBase base = DialogBase.builder(screen.title())
                .body(screen.body().stream().map(DialogBody::plainMessage).toList())
                .inputs(inputs)
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .build();
        DialogType type = type(viewer, screen);
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    private DialogType type(Viewer viewer, Screen screen) {
        return switch (screen) {
            case MenuScreen menu -> DialogType.multiAction(menu.buttons().stream()
                            .map(button -> button(viewer, button, List.of(), width(menu.columns()))).toList())
                    .columns(menu.columns())
                    .exitAction(button(viewer, menu.exit(), List.of(), NARROW))
                    .build();
            // Back is the exit action, so Escape goes back as well.
            case FormScreen form -> DialogType.multiAction(List.of(button(viewer, form.submit(), form.inputs(), MEDIUM)))
                    .columns(1)
                    .exitAction(button(viewer, form.back(), List.of(), NARROW))
                    .build();
            case ConfirmScreen confirm -> DialogType.confirmation(
                    button(viewer, confirm.yes(), List.of(), NARROW), button(viewer, confirm.no(), List.of(), NARROW));
            case NoticeScreen notice -> DialogType.notice(button(viewer, notice.ok(), List.of(), NARROW));
        };
    }

    private ActionButton button(Viewer viewer, Button button, List<Input> inputs, int width) {
        ActionButton.Builder builder = ActionButton.builder(label(button))
                .width(width)
                .action(callbacks.dialog(viewer, button.action(), inputs));
        if (button.hasTooltip()) {
            builder.tooltip(button.tooltip());
        }
        return builder.build();
    }

    /**
     * {@code button}'s label component: unchanged, except a {@link ButtonStyle#DANGER} button is colored with the
     * chat buttons' own danger color ({@code theme.button-danger}), since Paper's dialog buttons otherwise ignore
     * {@link ButtonStyle} entirely.
     */
    Component label(Button button) {
        if (button.style() != ButtonStyle.DANGER) {
            return button.label();
        }
        return messages.themeColor("button-danger").map(button.label()::color).orElse(button.label());
    }

    private static DialogInput input(Input input) {
        return switch (input) {
            case Slider slider -> DialogInput.numberRange(slider.key(), slider.label(), slider.min(), slider.max())
                    .step((float) slider.step())
                    .initial((float) slider.initial())
                    .width(WIDE)
                    .build();
            case Toggle toggle -> DialogInput.bool(toggle.key(), toggle.label()).initial(toggle.initial()).build();
            case Choice choice -> DialogInput.singleOption(choice.key(), choice.label(), choice.options().stream()
                    .map(option -> SingleOptionDialogInput.OptionEntry.create(option.id(), option.label(),
                            option.id().equals(choice.initial())))
                    .toList()).build();
        };
    }

    /** Wide buttons in one column (long "Join …" labels), narrower ones side by side. */
    private static int width(int columns) {
        return columns == 1 ? WIDE : columns == 2 ? MEDIUM : NARROW;
    }
}
