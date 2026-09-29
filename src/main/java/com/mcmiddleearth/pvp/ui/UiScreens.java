package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.text.Messages;

import java.util.Optional;

/**
 * The screens of the menu kit, built fresh from the services for every opening. {@code PvpPlugin}
 * creates one; {@code UiMenus} and {@code UiAnnouncer} ask it for screens and hand them to a {@link UiNavigator},
 * which the buttons also use to open the next screen. Call it on the server thread: the screens read the live
 * games.
 */
public final class UiScreens {

    private final Messages messages;
    private final MainMenuView main;
    private final StartGameView start;
    private final GamesListView games;
    private final SummaryView summary;
    private final InfoViews info;

    public UiScreens(PvpServices services, UiNavigator navigator) {
        this.messages = services.messages();
        this.info = new InfoViews(services);
        ConfirmView confirm = new ConfirmView(messages, navigator);
        this.start = new StartGameView(services, navigator);
        this.games = new GamesListView(services, navigator, confirm, info);
        this.summary = new SummaryView(messages, navigator, games, info);
        this.main = new MainMenuView(services, navigator, start, confirm, info);
    }

    /** {@code /pvp}. */
    public Screen mainMenu(Viewer viewer) {
        return main.build(viewer);
    }

    /** {@code /pvpadmin start} without an arena: the arena picker. */
    public Screen startPicker() {
        return start.picker();
    }

    /**
     * {@code /pvpadmin start <arena>} without settings: the form. Throws {@code PvpException} when the arena is
     * busy or cannot be played.
     */
    public Screen startForm(Arena arena) {
        return start.form(arena);
    }

    /** {@code /pvp games [page]}. */
    public ChatMenu games(Viewer viewer, int page) {
        return games.page(viewer, page);
    }

    /** {@code /pvp rules [mode]}: one mode's rules, or all of them. */
    public Screen rules(Optional<GameMode> mode) {
        return mode.<Screen>map(info::rules).orElseGet(info::rulesList);
    }

    /** {@code /pvp stats [player]}, shown under the player's current {@code name}. */
    public Screen stats(PlayerStats stats, String name) {
        return info.stats(stats, name);
    }

    /** The buttons under the "game opened" announcement. */
    public ChatMenu opened(Game game) {
        return games.opened(game);
    }

    /** The buttons under the end-of-game summary. */
    public ChatMenu summary() {
        return summary.buttons();
    }

    /** The notice for a player arriving on the server; empty when no game accepts joins. */
    public Optional<ChatMenu> arrival() {
        return games.arrival();
    }

    /** The texts, for a caller that answers a refusal itself ({@code UiMenus.startGame}). */
    public Messages messages() {
        return messages;
    }
}
