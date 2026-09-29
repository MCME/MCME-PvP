package com.mcmiddleearth.pvp.ui;

import net.kyori.adventure.text.Component;

/**
 * How the views reach a player. The Paper implementation ({@code platform.ui.UiService}) shows screens as dialogs
 * and menus in chat; tests record what was shown.
 */
public interface UiNavigator {

    /** Shows {@code screen} as a dialog. */
    void show(Viewer viewer, Screen screen);

    /** Sends {@code menu} to the viewer's chat. */
    void show(Viewer viewer, ChatMenu menu);

    void message(Viewer viewer, Component message);

    /**
     * Runs a command as the viewer, for a staff action that exists only as a command (Kick all sends players to
     * another server, which needs the server's player list). The command checks its permission again.
     *
     * <p>{@code command} must be a constant line, never built from game data: the Paper implementation runs it
     * with {@code performCommand}, which splits it on spaces, so a value such as an arena id could inject extra
     * arguments. To act on game data, call the service instead of building a command string.
     */
    void perform(Viewer viewer, String command);

    /**
     * Writes {@code message} to the server log, for a staff action taken from a menu that runs through the
     * service directly instead of a command (a command run with {@link #perform} is already logged there).
     */
    void log(String message);
}
