package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.command.Menus;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.UiNavigator;
import com.mcmiddleearth.pvp.ui.UiScreens;
import com.mcmiddleearth.pvp.ui.Viewer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * The command screens as dialogs and clickable chat. Players get the menu kit; the console, and any other
 * sender that is not a player, gets the plain text of {@code fallback}.
 */
public final class UiMenus implements Menus {

    private final UiScreens screens;
    private final UiNavigator navigator;
    private final Menus fallback;

    public UiMenus(UiScreens screens, UiNavigator navigator, Menus fallback) {
        this.screens = screens;
        this.navigator = navigator;
        this.fallback = fallback;
    }

    @Override
    public void mainMenu(Player player) {
        Viewer viewer = Viewers.of(player);
        navigator.show(viewer, screens.mainMenu(viewer));
    }

    @Override
    public void games(CommandSender sender, int page) {
        if (sender instanceof Player player) {
            Viewer viewer = Viewers.of(player);
            navigator.show(viewer, screens.games(viewer, page));
        } else {
            fallback.games(sender, page);
        }
    }

    @Override
    public void startGame(Player player, Optional<Arena> arena) {
        Viewer viewer = Viewers.of(player);
        Screen screen;
        try {
            screen = arena.isPresent() ? screens.startForm(arena.get()) : screens.startPicker();
            // A refused arena is answered here, as Replies.run would for a command; TextMenus leaves refusals
            // to the command.
        } catch (PvpException e) {
            navigator.message(viewer, screens.messages().error(e));
            return;
        }
        navigator.show(viewer, screen);
    }

    @Override
    public void rules(CommandSender sender, Optional<GameMode> mode) {
        if (sender instanceof Player player) {
            navigator.show(Viewers.of(player), screens.rules(mode));
        } else {
            fallback.rules(sender, mode);
        }
    }

    /** {@code /pvp stats} hands over the stats under the player's current name, which the notice shows. */
    @Override
    public void stats(CommandSender sender, PlayerStats stats) {
        if (sender instanceof Player player) {
            navigator.show(Viewers.of(player), screens.stats(stats, stats.name()));
        } else {
            fallback.stats(sender, stats);
        }
    }
}
