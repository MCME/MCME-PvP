package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;

import java.util.List;

/** Destructive buttons ask first: Yes re-checks the action's permission and state, then runs once; No only closes. */
public final class ConfirmView {

    private final Messages messages;
    private final UiNavigator navigator;

    public ConfirmView(Messages messages, UiNavigator navigator) {
        this.messages = messages;
        this.navigator = navigator;
    }

    public ConfirmScreen confirm(Component title, Component body, UiAction destructive) {
        return new ConfirmScreen(title, List.of(body),
                new Button(messages.get(Msg.UI_CONFIRM_YES), Component.empty(), destructive.once(), ButtonStyle.DANGER),
                new Button(messages.get(Msg.UI_CONFIRM_NO), Component.empty(), UiAction.none(), ButtonStyle.NORMAL));
    }

    /**
     * The action of a button that asks before {@code destructive}. It carries the same permission and still-valid
     * check, so a stale button is refused before it even asks.
     */
    public UiAction asking(Component title, Component body, UiAction destructive) {
        return new UiAction(destructive.permission(), destructive.check(),
                (viewer, values) -> navigator.show(viewer, confirm(title, body, destructive)), false);
    }
}
