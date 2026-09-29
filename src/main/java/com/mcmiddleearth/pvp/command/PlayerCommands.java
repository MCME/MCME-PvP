package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinReplies;
import com.mcmiddleearth.pvp.game.JoinResult;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

/** What {@code /pvp} and its sub-commands do; {@link PvpCommands} parses the arguments and calls these. */
public final class PlayerCommands {

    private final PvpServices services;
    private final Menus menus;
    private final Messages messages;
    private final GameManager games;

    public PlayerCommands(PvpServices services, Menus menus) {
        this.services = services;
        this.menus = menus;
        this.messages = services.messages();
        this.games = services.games();
    }

    /** {@code /pvp}: the main menu; the console gets the command overview. */
    public void root(CommandSender sender) {
        if (sender instanceof Player player) {
            menus.mainMenu(player);
        } else {
            sender.sendMessage(messages.get(Msg.HELP));
        }
    }

    /** {@code /pvp join [arena]}: the game on that arena, or the only open game when there is exactly one. */
    public void join(CommandSender sender, Optional<String> arena) {
        Replies.run(sender, messages, () -> {
            Player player = Replies.player(sender);
            if (services.lock().keepsOut(player::hasPermission)) {
                throw new PvpException(Msg.ERROR_SERVER_LOCKED);
            }
            Game game = arena.isPresent() ? gameOn(arena.get()) : onlyOpenGame();
            String spectateReason = JoinReplies.spectateReason(game, player.getUniqueId());
            if (games.join(JoinUnit.solo(Replies.ref(player)), game) == JoinResult.SPECTATING) {
                player.sendMessage(messages.get(spectateReason));
            }
        });
    }

    /** {@code /pvp leave}: back to the lobby. */
    public void leave(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Player player = Replies.player(sender);
            if (!games.leave(player.getUniqueId(), LeaveReason.LEFT)) {
                throw new PvpException(Msg.ERROR_NOT_IN_GAME);
            }
        });
    }

    /** {@code /pvp spectate <arena>}: refused like joining while PvP is locked. */
    public void spectate(CommandSender sender, String arena) {
        Replies.run(sender, messages, () -> {
            Player player = Replies.player(sender);
            if (services.lock().keepsOut(player::hasPermission)) {
                throw new PvpException(Msg.ERROR_SERVER_LOCKED);
            }
            games.spectate(Replies.ref(player), gameOn(arena));
        });
    }

    /** {@code /pvp games [page]}. */
    public void games(CommandSender sender, int page) {
        menus.games(sender, page);
    }

    /** A bare {@code /pvp} sub-command's usage line. */
    public void usage(CommandSender sender, String usage) {
        Replies.usage(sender, messages, usage);
    }

    /** {@code /pvp rules [mode]}: one mode's rules, or the list of modes. */
    public void rules(CommandSender sender, Optional<String> mode) {
        Replies.run(sender, messages, () -> {
            Optional<GameMode> chosen = mode.map(id -> services.modes().get(id)
                    .orElseThrow(() -> new PvpException(Msg.ERROR_UNKNOWN_MODE, Map.of("mode", id))));
            menus.rules(sender, chosen);
        });
    }

    /** {@code /pvp stats [player]}: the sender's stats, or those of a player the server knows, online or not. */
    public void stats(CommandSender sender, Optional<String> name) {
        Replies.run(sender, messages, () -> {
            OfflinePlayer target = name.isPresent() ? known(name.get()) : Replies.player(sender);
            // The name the server knows now, else the one typed: stats found only in 1.1.2's files carry the uuid.
            String shown = target != null && target.getName() != null
                    ? target.getName() : name.orElse(sender.getName());
            Optional<PlayerStats> stats = target == null
                    ? Optional.empty() : services.stats().find(target.getUniqueId());
            if (stats.isPresent()) {
                menus.stats(sender, named(stats.get(), shown));
            } else {
                sender.sendMessage(messages.get(Msg.STATS_NONE, Map.of("player", shown)));
            }
        });
    }

    /** {@code /pvp lobby}: in a game this is leaving it; otherwise a teleport to the lobby. */
    public void lobby(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Player player = Replies.player(sender);
            if (games.leave(player.getUniqueId(), LeaveReason.LEFT)) {
                return;
            }
            services.lobby().send(player.getUniqueId());
            player.sendMessage(messages.get(Msg.LOBBY_TELEPORTED));
        });
    }

    private Game gameOn(String arena) {
        return games.gameOn(arena)
                .orElseThrow(() -> new PvpException(Msg.ERROR_NO_GAME_ON_ARENA, Map.of("arena", arena)));
    }

    private Game onlyOpenGame() {
        Optional<Game> only = games.soleOpenGame();
        if (only.isPresent()) {
            return only.get();
        }
        boolean several = games.games().stream().anyMatch(Game::acceptsJoins);
        throw new PvpException(several ? Msg.ERROR_CHOOSE_GAME : Msg.ERROR_NO_OPEN_GAME);
    }

    private static PlayerStats named(PlayerStats stats, String name) {
        return new PlayerStats(stats.schemaVersion(), stats.id(), name, stats.kills(), stats.deaths(),
                stats.gamesPlayed(), stats.gamesWon(), stats.gamesLost(), stats.gamesSpectated(), stats.playersKilled());
    }

    /** A player by exact name: online, or known to the server from an earlier visit; null when unknown. */
    private static OfflinePlayer known(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }
}
