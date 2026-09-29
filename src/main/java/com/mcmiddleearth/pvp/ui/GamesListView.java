package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The clickable game lists: the paged {@code /pvp games} list, the button row under the "game opened"
 * announcement, and the arrival notice. On {@code /pvp games}, a game line offers [Join] only when the viewer is
 * free to join it: the join policy would let them play (the same rule as joining itself), they are not eliminated
 * from that game already, and they are not currently committed to another game of their own; [Spectate] shows
 * whenever the game takes spectators and the viewer is not busy elsewhere, even for a game they were eliminated
 * from. A viewer whose own game has ended still gets both, since it no longer holds them. Staff also get
 * [Countdown] and [Stop]; Stop asks first.
 */
public final class GamesListView {

    /** Games per page of {@code /pvp games}. */
    static final int PER_PAGE = 8;

    /** Games shown in the arrival notice before it points to the full list instead. */
    static final int ARRIVAL_MAX = 8;

    private final PvpServices services;
    private final Messages messages;
    private final UiNavigator navigator;
    private final GameActions actions;
    private final ConfirmView confirm;
    private final InfoViews info;

    public GamesListView(PvpServices services, UiNavigator navigator, ConfirmView confirm, InfoViews info) {
        this.services = services;
        this.messages = services.messages();
        this.navigator = navigator;
        this.actions = new GameActions(services, navigator);
        this.confirm = confirm;
        this.info = info;
    }

    /** One page of the open games; {@code page} is clamped to the pages there are. */
    public ChatMenu page(Viewer viewer, int page) {
        List<Game> games = services.games().games();
        int pages = Math.max(1, (games.size() + PER_PAGE - 1) / PER_PAGE);
        int current = Math.max(1, Math.min(page, pages));
        List<ChatLine> lines = new ArrayList<>();
        if (games.isEmpty()) {
            lines.add(ChatLine.of(ChatPart.text(messages.get(Msg.GAMES_NONE))));
        }
        for (Game game : games.subList((current - 1) * PER_PAGE, Math.min(games.size(), current * PER_PAGE))) {
            List<ChatPart> parts = new ArrayList<>();
            parts.add(ChatPart.text(UiText.gameLine(messages, game)));
            parts.addAll(playerButtons(game, viewer));
            if (viewer.has(Permissions.STAFF_RUN)) {
                parts.addAll(staffButtons(game));
            }
            lines.add(new ChatLine(parts));
        }
        return new ChatMenu(messages.get(Msg.GAMES_HEADER), lines, current, pages,
                current > 1 ? turnTo(current - 1) : null, current < pages ? turnTo(current + 1) : null);
    }

    /** [Join] [Spectate] [Rules] under the "game opened" text. */
    public ChatMenu opened(Game game) {
        List<ChatPart> parts = new ArrayList<>(playerButtons(game));
        parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_RULES),
                messages.get(Msg.UI_TOOLTIP_RULES, Map.of("mode", UiText.modeName(messages, game.mode()))),
                UiAction.of((viewer, values) -> navigator.show(viewer, info.rules(game.mode()))), ButtonStyle.NORMAL)));
        return ChatMenu.of(null, List.of(new ChatLine(parts)));
    }

    /**
     * The notice for a player arriving on the server: every game that accepts joins (a join may still end in
     * spectating), at most {@link #ARRIVAL_MAX} of them, then a link to the full list when there are more; empty
     * when there is no open game at all.
     */
    public Optional<ChatMenu> arrival() {
        List<Game> open = services.games().games().stream().filter(actions::offersSpectate).toList();
        if (open.isEmpty()) {
            return Optional.empty();
        }
        List<ChatLine> lines = new ArrayList<>();
        for (Game game : open.subList(0, Math.min(open.size(), ARRIVAL_MAX))) {
            List<ChatPart> parts = new ArrayList<>();
            parts.add(ChatPart.text(UiText.gameLine(messages, game)));
            parts.addAll(playerButtons(game));
            lines.add(new ChatLine(parts));
        }
        if (open.size() > ARRIVAL_MAX) {
            lines.add(ChatLine.of(ChatPart.link(moreGamesButton())));
        }
        return Optional.of(ChatMenu.of(messages.get(Msg.UI_ARRIVAL), lines));
    }

    /** Shows the full list directly (as {@code turnTo}'s first page does), rather than running it as a command. */
    private Button moreGamesButton() {
        return Button.of(messages.get(Msg.UI_ARRIVAL_MORE), turnTo(1));
    }

    /** The [Join] and [Spectate] parts of one row of the "game opened" announcement and the arrival notice. */
    private List<ChatPart> playerButtons(Game game) {
        List<ChatPart> parts = new ArrayList<>();
        Map<String, String> arena = Map.of("arena", game.arena().id());
        if (actions.offersJoin(game)) {
            parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_JOIN), messages.get(Msg.UI_TOOLTIP_JOIN, arena),
                    actions.join(game), ButtonStyle.PRIMARY)));
        }
        if (actions.offersSpectate(game)) {
            parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_SPECTATE),
                    messages.get(Msg.UI_TOOLTIP_SPECTATE, arena), actions.spectate(game), ButtonStyle.NORMAL)));
        }
        return parts;
    }

    /**
     * The [Join] and [Spectate] parts of one row of {@code /pvp games}, for {@code viewer}: neither while the
     * viewer is committed to another game of their own that has not ended, and never [Join] for a game the viewer
     * was eliminated from (they may still watch it).
     */
    private List<ChatPart> playerButtons(Game game, Viewer viewer) {
        if (isBusy(viewer)) {
            return List.of();
        }
        boolean eliminatedHere = game.session(viewer.id()).filter(PlayerSession::eliminated).isPresent();
        List<ChatPart> parts = new ArrayList<>();
        Map<String, String> arena = Map.of("arena", game.arena().id());
        if (!eliminatedHere && actions.offersJoin(game)) {
            parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_JOIN), messages.get(Msg.UI_TOOLTIP_JOIN, arena),
                    actions.join(game), ButtonStyle.PRIMARY)));
        }
        if (actions.offersSpectate(game)) {
            parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_SPECTATE),
                    messages.get(Msg.UI_TOOLTIP_SPECTATE, arena), actions.spectate(game), ButtonStyle.NORMAL)));
        }
        return parts;
    }

    /** Playing or spectating a game of their own that has not ended: leaving it is the way to act on any list row. */
    private boolean isBusy(Viewer viewer) {
        return services.games().gameOf(viewer.id()).filter(current -> current.phase() != Phase.ENDING).isPresent();
    }

    private List<ChatPart> staffButtons(Game game) {
        List<ChatPart> parts = new ArrayList<>();
        Map<String, String> arena = Map.of("arena", game.arena().id());
        if (game.phase() == Phase.WAITING) {
            parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_COUNTDOWN),
                    messages.get(Msg.UI_TOOLTIP_COUNTDOWN, arena), actions.countdown(game), ButtonStyle.NORMAL)));
        }
        Component question = messages.get(Msg.UI_CONFIRM_STOP_TITLE, arena);
        parts.add(ChatPart.link(new Button(messages.get(Msg.UI_CHAT_STOP), messages.get(Msg.UI_TOOLTIP_STOP, arena),
                confirm.asking(question, messages.get(Msg.UI_CONFIRM_STOP_BODY), actions.stop(game)),
                ButtonStyle.DANGER)));
        return parts;
    }

    private UiAction turnTo(int page) {
        return UiAction.of((viewer, values) -> navigator.show(viewer, page(viewer, page)));
    }
}
