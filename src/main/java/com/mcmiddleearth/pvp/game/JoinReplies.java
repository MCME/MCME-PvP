package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.text.Msg;

import java.util.UUID;

/**
 * Picks the message key for a join to {@code game} that ends up spectating instead of playing, shared by the
 * command and the menus.
 */
public final class JoinReplies {

    private JoinReplies() {
    }

    /**
     * Which text explains it: watching out a past elimination from this same game, the game being full, or the
     * mode only letting a mid-game joiner watch. Read the game's state before the join itself runs: joining
     * already changes {@code playerCount()}.
     */
    public static String spectateReason(Game game, UUID playerId) {
        boolean eliminated = game.session(playerId).filter(PlayerSession::eliminated).isPresent();
        boolean full = game.playerCount() + 1 > game.maxPlayers();
        return eliminated ? Msg.GAME_REJOIN_ELIMINATED : full ? Msg.JOIN_FULL_SPECTATING : Msg.JOIN_MIDGAME_SPECTATING;
    }
}
