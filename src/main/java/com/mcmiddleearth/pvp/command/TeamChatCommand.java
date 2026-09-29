package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.GameTeam;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** {@code /t <message>}: chat to the members of your own team in your game, the sender included. */
public final class TeamChatCommand {

    private final PvpServices services;
    private final Messages messages;

    public TeamChatCommand(PvpServices services) {
        this.services = services;
        this.messages = services.messages();
    }

    /** {@code /t} alone: its usage line. */
    public void usage(CommandSender sender) {
        Replies.usage(sender, messages, "/t <message>");
    }

    public void send(CommandSender sender, String message) {
        Replies.run(sender, messages, () -> {
            Player player = Replies.player(sender);
            UUID id = player.getUniqueId();
            GameTeam team = services.games().gameOf(id)
                    .flatMap(game -> game.session(id))
                    .filter(PlayerSession::alive)
                    .flatMap(PlayerSession::team)
                    .orElseThrow(() -> new PvpException(Msg.ERROR_NO_TEAM));
            // The message is inserted unparsed, so players cannot use formatting tags in it.
            Component line = messages.get(Msg.TEAM_CHAT_FORMAT,
                    Placeholder.styling("color", team.spec().color()),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("message", message));
            for (PlayerSession member : team.members()) {
                if (!member.alive()) {
                    // 1.1.2 removed the dead from their team: an eliminated player neither sends nor hears it.
                    continue;
                }
                Player online = Bukkit.getPlayer(member.id());
                if (online != null) {
                    online.sendMessage(line);
                }
            }
        });
    }
}
