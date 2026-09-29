package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;

import java.util.Map;

/** Texts and buttons that several views share, so a game reads the same in every screen. */
final class UiText {

    private UiText() {
    }

    static String modeName(Messages messages, GameMode mode) {
        return messages.plain(Msg.modeName(mode.id()));
    }

    /** The placeholders of a game line: arena, mode, count, max and status. */
    static Map<String, String> gameArgs(Messages messages, Game game) {
        return gameArgs(messages, game, game.playerCount());
    }

    /** As {@link #gameArgs(Messages, Game)}, with an explicit count in place of the game's current one. */
    static Map<String, String> gameArgs(Messages messages, Game game, int count) {
        return Map.of(
                "arena", game.arena().id(),
                "mode", modeName(messages, game.mode()),
                "count", String.valueOf(count),
                "max", String.valueOf(game.maxPlayers()),
                "status", messages.plain(Msg.phase(game.phase().name())));
    }

    /** "AS-TDM · Team Deathmatch · 3/16 · waiting". */
    static Component gameLine(Messages messages, Game game) {
        return messages.get(Msg.GAMES_ENTRY, gameArgs(messages, game));
    }

    /** As {@link #gameLine(Messages, Game)}, with an explicit count (the standings at the moment a game ended). */
    static Component gameLine(Messages messages, Game game, int count) {
        return messages.get(Msg.GAMES_ENTRY, gameArgs(messages, game, count));
    }

    static Component team(TeamSpec spec) {
        return Component.text(spec.name(), spec.color());
    }

    static Button button(Messages messages, String key, UiAction action) {
        return Button.of(messages.get(key), action);
    }

    /** The button that only closes a screen. */
    static Button close(Messages messages) {
        return button(messages, Msg.UI_CLOSE, UiAction.none());
    }
}
