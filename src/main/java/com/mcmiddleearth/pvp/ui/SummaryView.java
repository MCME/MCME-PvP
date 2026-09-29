package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.List;

/** The [Games] [My stats] row under the end-of-game summary. */
public final class SummaryView {

    private final Messages messages;
    private final UiNavigator navigator;
    private final GamesListView games;
    private final InfoViews info;

    public SummaryView(Messages messages, UiNavigator navigator, GamesListView games, InfoViews info) {
        this.messages = messages;
        this.navigator = navigator;
        this.games = games;
        this.info = info;
    }

    public ChatMenu buttons() {
        Button list = new Button(messages.get(Msg.UI_CHAT_GAMES), messages.get(Msg.UI_TOOLTIP_GAMES),
                UiAction.of((viewer, values) -> navigator.show(viewer, games.page(viewer, 1))), ButtonStyle.NORMAL);
        Button stats = new Button(messages.get(Msg.UI_CHAT_STATS), messages.get(Msg.UI_TOOLTIP_STATS),
                UiAction.of((viewer, values) -> navigator.show(viewer, info.statsOf(viewer))), ButtonStyle.NORMAL);
        return ChatMenu.of(null, List.of(ChatLine.of(ChatPart.link(list), ChatPart.link(stats))));
    }
}
