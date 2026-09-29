package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/** Shared command plumbing: every refusal is a {@link PvpException} that becomes one chat line for the sender. */
final class Replies {

    private Replies() {
    }

    /** Runs a command; a refusal is answered with its message instead of an error. */
    static void run(CommandSender sender, Messages messages, Runnable command) {
        try {
            command.run();
        } catch (PvpException refusal) {
            sender.sendMessage(messages.error(refusal));
        }
    }

    /** The sender as a player; the console is refused. */
    static Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw new PvpException(Msg.ERROR_PLAYERS_ONLY);
    }

    /** Refuses a sender without the permission (Brigadier's {@code requires} also hides the command from them). */
    static void require(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw new PvpException(Msg.ERROR_NO_PERMISSION);
        }
    }

    static PlayerRef ref(Player player) {
        return new PlayerRef(player.getUniqueId(), player.getName());
    }

    /** A bare sub-command's usage line: "Usage: <usage>". */
    static void usage(CommandSender sender, Messages messages, String usage) {
        sender.sendMessage(messages.get(Msg.USAGE, Map.of("usage", usage)));
    }
}
