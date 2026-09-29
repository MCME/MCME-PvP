package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code /pvp} main menu, generated from the open games. Outside a game: a Join button per game the
 * viewer could play in ("Join AS-TDM · Team Deathmatch · 6/16 · waiting"), then Spectate, Rules, My stats and
 * Lobby. Inside a game: Leave, the rules of its mode, and its teams and scores. A game that has ended counts as
 * inside until it closes; joining another game from the games list works then too. Staff also get Start a game
 * and Kick all behind a confirmation, both needing {@code pvp.staff.run}.
 */
public final class MainMenuView {

    /** Kick all needs the server's player list and the proxy, which only the command has. */
    static final String KICK_ALL_COMMAND = "pvpadmin kickall";

    private final PvpServices services;
    private final Messages messages;
    private final UiNavigator navigator;
    private final GameActions actions;
    private final StartGameView start;
    private final ConfirmView confirm;
    private final InfoViews info;

    public MainMenuView(PvpServices services, UiNavigator navigator, StartGameView start, ConfirmView confirm,
                        InfoViews info) {
        this.services = services;
        this.messages = services.messages();
        this.navigator = navigator;
        this.actions = new GameActions(services, navigator);
        this.start = start;
        this.confirm = confirm;
        this.info = info;
    }

    public MenuScreen build(Viewer viewer) {
        return services.games().gameOf(viewer.id()).map(game -> inGame(viewer, game)).orElseGet(() -> outside(viewer));
    }

    private MenuScreen outside(Viewer viewer) {
        List<Game> games = services.games().games();
        List<Button> buttons = new ArrayList<>();
        for (Game game : games) {
            if (actions.offersJoin(game)) {
                buttons.add(new Button(messages.get(Msg.UI_MAIN_JOIN, UiText.gameArgs(messages, game)),
                        messages.get(Msg.UI_TOOLTIP_JOIN, Map.of("arena", game.arena().id())),
                        actions.join(game), ButtonStyle.PRIMARY));
            }
        }
        buttons.add(UiText.button(messages, Msg.UI_MAIN_SPECTATE,
                UiAction.of((clicker, values) -> navigator.show(clicker, spectateList()))));
        buttons.add(UiText.button(messages, Msg.UI_MAIN_RULES,
                UiAction.of((clicker, values) -> navigator.show(clicker, info.rulesList()))));
        buttons.add(UiText.button(messages, Msg.UI_MAIN_STATS,
                UiAction.of((clicker, values) -> navigator.show(clicker, info.statsOf(clicker)))));
        buttons.add(UiText.button(messages, Msg.UI_MAIN_LOBBY, lobby(viewer)));
        buttons.addAll(staffButtons(viewer));
        Component body = messages.get(mainBodyKey(games));
        return new MenuScreen(messages.get(Msg.UI_MAIN_TITLE), List.of(body), buttons, 1, UiText.close(messages));
    }

    /** Nothing to spectate, something to join, or something to watch but nothing that takes players. */
    private String mainBodyKey(List<Game> games) {
        if (games.stream().noneMatch(actions::offersSpectate)) {
            return Msg.UI_MAIN_BODY_EMPTY;
        }
        return games.stream().anyMatch(actions::offersJoin) ? Msg.UI_MAIN_BODY : Msg.UI_MAIN_BODY_WATCH;
    }

    private MenuScreen inGame(Viewer viewer, Game game) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(UiText.button(messages, Msg.UI_MAIN_LEAVE, actions.leave(viewer, game)));
        buttons.add(UiText.button(messages, Msg.UI_MAIN_RULES,
                UiAction.of((clicker, values) -> navigator.show(clicker, info.rules(game.mode())))));
        buttons.add(UiText.button(messages, Msg.UI_MAIN_SCORES,
                UiAction.of((clicker, values) -> navigator.show(clicker, info.scores(game)))
                        .validWhile(() -> actions.stillOpen(game))));
        buttons.addAll(staffButtons(viewer));
        Component title = messages.get(Msg.UI_INGAME_TITLE,
                Map.of("mode", UiText.modeName(messages, game.mode()), "arena", game.arena().id()));
        return new MenuScreen(title, List.of(UiText.gameLine(messages, game)), buttons, 1, UiText.close(messages));
    }

    /** The games that take spectators, as buttons; a notice when there is none. Back returns to the main menu. */
    Screen spectateList() {
        Button back = UiText.button(messages, Msg.UI_BACK,
                UiAction.of((clicker, values) -> navigator.show(clicker, build(clicker))));
        List<Button> buttons = new ArrayList<>();
        for (Game game : services.games().games()) {
            if (actions.offersSpectate(game)) {
                int count = game.playerCount();
                buttons.add(new Button(
                        messages.get(Msg.plural(Msg.UI_SPECTATE_ENTRY, Msg.UI_SPECTATE_ENTRY_ONE, count), Map.of(
                                "arena", game.arena().id(),
                                "mode", UiText.modeName(messages, game.mode()),
                                "count", String.valueOf(count))),
                        messages.get(Msg.UI_TOOLTIP_SPECTATE, Map.of("arena", game.arena().id())),
                        actions.spectate(game), ButtonStyle.NORMAL));
            }
        }
        Component title = messages.get(Msg.UI_SPECTATE_TITLE);
        return buttons.isEmpty()
                ? new NoticeScreen(title, List.of(messages.get(Msg.UI_MAIN_BODY_EMPTY)), back)
                : new MenuScreen(title, List.of(), buttons, 1, back);
    }

    private List<Button> staffButtons(Viewer viewer) {
        List<Button> buttons = new ArrayList<>();
        if (viewer.has(Permissions.STAFF_RUN)) {
            buttons.add(UiText.button(messages, Msg.UI_MAIN_START,
                    UiAction.of((clicker, values) -> navigator.show(clicker, start.picker()))
                            .requires(Permissions.STAFF_RUN)));
        }
        if (viewer.has(Permissions.STAFF_RUN)) {
            UiAction kickAll = UiAction.of((clicker, values) -> navigator.perform(clicker, KICK_ALL_COMMAND))
                    .requires(Permissions.STAFF_RUN);
            buttons.add(new Button(messages.get(Msg.UI_MAIN_KICKALL), Component.empty(),
                    confirm.asking(messages.get(Msg.UI_CONFIRM_KICKALL_TITLE),
                            messages.get(Msg.UI_CONFIRM_KICKALL_BODY), kickAll),
                    ButtonStyle.DANGER));
        }
        return buttons;
    }

    /**
     * Lobby, for players outside games: the same service and message as {@code /pvp lobby}. A viewer who joined a
     * game after the menu was drawn is refused: the button was made for someone outside games, and
     * {@code /pvp lobby} or Leave is the way out of a game.
     */
    private UiAction lobby(Viewer viewer) {
        return UiAction.of((clicker, values) -> {
                    services.lobby().send(viewer.id());
                    navigator.message(viewer, messages.get(Msg.LOBBY_TELEPORTED));
                })
                .requires(Permissions.PLAY)
                .validWhile(() -> services.games().gameOf(viewer.id()).isPresent()
                        ? Optional.of(new PvpException(Msg.ERROR_ALREADY_IN_GAME, Map.of("player", viewer.name())))
                        : Optional.empty());
    }
}
