package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.Region;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * What {@code /pvpadmin} does. Typed commands act at once (typing one is already deliberate); the menus'
 * confirmation screens are for clicks. Every action checks its permission again, so a caller cannot skip it.
 */
public final class AdminCommands {

    private final PvpServices services;
    private final Menus menus;
    private final Messages messages;
    private final Logger logger;

    public AdminCommands(PvpServices services, Menus menus, Logger logger) {
        this.services = services;
        this.menus = menus;
        this.messages = services.messages();
        this.logger = logger;
    }

    /** {@code /pvpadmin} alone: every sub-command's syntax. */
    public void help(CommandSender sender) {
        sender.sendMessage(messages.get(Msg.ADMIN_HELP));
    }

    /** A bare admin sub-command's usage line. */
    public void usage(CommandSender sender, String usage) {
        Replies.usage(sender, messages, usage);
    }

    /**
     * {@code /pvpadmin start [arena] [settings]}. A player without settings gets the start screen (the arena picker
     * without an arena); with settings, or from the console, the game opens at once with the defaults for the rest.
     */
    public void start(CommandSender sender, Optional<String> arena, Optional<String> settings) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            if (sender instanceof Player player && settings.isEmpty()) {
                menus.startGame(player, arena.map(this::arena));
                return;
            }
            if (arena.isEmpty()) {
                arenaList(sender);
                return;
            }
            String openedBy = sender instanceof Player ? sender.getName() : "Console";
            Game game = services.games().open(arena.get(), SettingsArgument.parse(settings.orElse("")), openedBy);
            sender.sendMessage(messages.get(Msg.ADMIN_OPENED,
                    Map.of("mode", modeName(game.mode().id()), "arena", game.arena().id())));
        });
    }

    /** {@code /pvpadmin countdown <arena>}. */
    public void countdown(CommandSender sender, String arena) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            Game game = gameOn(arena);
            services.games().startCountdown(game);
            sender.sendMessage(messages.get(Msg.ADMIN_COUNTDOWN, Map.of("arena", game.arena().id())));
        });
    }

    /** {@code /pvpadmin stop <arena>}: a running game shows its result first, any other closes at once. */
    public void stop(CommandSender sender, String arena) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            Game game = gameOn(arena);
            services.games().stop(game);
            sender.sendMessage(messages.get(Msg.ADMIN_STOPPED, Map.of("arena", game.arena().id())));
        });
    }

    /** {@code /pvpadmin kick <player>}: out of their game and off to the main server. */
    public void kick(CommandSender sender, String name) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_KICK);
            Player target = Bukkit.getPlayerExact(name);
            if (target == null) {
                throw new PvpException(Msg.ERROR_UNKNOWN_PLAYER, Map.of("player", name));
            }
            if (!removeFromGame(target)) {
                target.sendMessage(messages.get(Msg.LEAVE_KICKED));
            }
            List<String> unready = new ArrayList<>();
            moveToMainServer(target, unready);
            warnUnready(unready);
            sender.sendMessage(messages.get(Msg.ADMIN_KICKED, Map.of("player", target.getName())));
        });
    }

    /** {@code /pvpadmin kickall}: everyone without {@code pvp.join.locked} goes to the main server. */
    public void kickAll(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            int count = sendAway(sender, Msg.ADMIN_KICKED_ALL_NOTICE);
            sender.sendMessage(messages.get(Msg.plural(Msg.ADMIN_KICKED_ALL, Msg.ADMIN_KICKED_ALL_ONE, count),
                    Map.of("count", String.valueOf(count))));
        });
    }

    /** {@code /pvpadmin lock}: locks PvP and sends everyone without {@code pvp.join.locked} away. */
    public void lock(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            services.lock().lock();
            sendAway(sender, Msg.ADMIN_LOCKED_NOTICE);
            sender.sendMessage(messages.get(Msg.ADMIN_LOCKED));
        });
    }

    /** {@code /pvpadmin unlock}. */
    public void unlock(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_RUN);
            services.lock().unlock();
            sender.sendMessage(messages.get(Msg.ADMIN_UNLOCKED));
        });
    }

    /** {@code /pvpadmin arena list}: every arena, playable or not (with the reason). */
    public void arenaList(CommandSender sender) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_ARENA);
            List<Arena> all = services.arenas().all();
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_LIST_HEADER, Map.of("count", String.valueOf(all.size()))));
            for (Arena arena : all) {
                sender.sendMessage(arena.playable()
                        ? messages.get(Msg.ADMIN_ARENA_LIST_ENTRY,
                                Map.of("arena", arena.id(), "mode", modeNameOf(arena)))
                        : messages.get(Msg.ADMIN_ARENA_LIST_UNASSIGNED, Map.of("arena", arena.id(), "reason",
                                Objects.requireNonNullElse(arena.unassignedReason(), "no reason recorded"))));
            }
        });
    }

    /** {@code /pvpadmin arena info <id>}: a short block, one label per line. */
    public void arenaInfo(CommandSender sender, String id) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_ARENA);
            Arena arena = arena(id);
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_INFO_TITLE, Map.of(
                    "arena", arena.id(),
                    "title", Objects.requireNonNullElse(arena.title(), arena.id()))));
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_INFO_MODE, Map.of(
                    "mode", arena.playable() ? modeNameOf(arena) : "unassigned",
                    "max", String.valueOf(arena.maxPlayers()),
                    "world", arena.world())));
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_INFO_SPAWNS, Map.of("spawns", describeSpawns(arena))));
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_INFO_REGION, Map.of("region", describe(arena.region()))));
            if (!arena.playable()) {
                sender.sendMessage(messages.get(Msg.ADMIN_ARENA_INFO_UNASSIGNED, Map.of("reason",
                        Objects.requireNonNullElse(arena.unassignedReason(), "no reason recorded"))));
            }
        });
    }

    /**
     * {@code /pvpadmin arena import <id>}: imports {@code maps/<id>} again over the arena file. A map that cannot be
     * read or saved is logged and reported with the importer's reason; the arena stays as it was.
     */
    public void arenaImport(CommandSender sender, String id) {
        Replies.run(sender, messages, () -> {
            Replies.require(sender, Permissions.STAFF_ARENA);
            Arena arena;
            try {
                arena = services.arenas().reimport(id);
            } catch (UncheckedIOException e) {
                logger.log(Level.WARNING, "Re-importing arena " + id + " failed", e);
                throw new PvpException(Msg.ERROR_ARENA_UNASSIGNED, Map.of("arena", id, "reason", e.getMessage()));
            }
            String file = arena.imported() == null ? arena.id() : arena.imported().sourceFile();
            sender.sendMessage(messages.get(Msg.ADMIN_ARENA_IMPORTED, Map.of("arena", arena.id(), "file", file)));
        });
    }

    /**
     * Sends every online player without {@code pvp.join.locked} to the main server, except {@code sender} (a
     * moderator group can grant the kick or run permission without also granting the stay-locked one, and running
     * this from a menu click always names the clicking staff member as sender); returns how many.
     * <p>
     * First stops, as a draw, every RUNNING game that would otherwise lose a participating player to the sweep:
     * removing players one by one instead would let a win condition fire on whatever order the online players
     * happen to be in, handing some team a win it did not earn (1.1.2 refused this during a running game). Only
     * once every such game has already ended are the players actually removed, notified and sent away.
     */
    private int sendAway(CommandSender sender, String noticeKey) {
        List<Player> targets = new ArrayList<>();
        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            if (player.hasPermission(Permissions.JOIN_LOCKED) || sender.equals(player)) {
                continue;
            }
            targets.add(player);
        }
        Set<Game> runningGames = new LinkedHashSet<>();
        for (Player player : targets) {
            services.games().gameOf(player.getUniqueId())
                    .filter(game -> game.phase() == Phase.RUNNING)
                    .filter(game -> game.session(player.getUniqueId()).map(PlayerSession::role)
                            .filter(Role.PLAYER::equals).isPresent())
                    .ifPresent(runningGames::add);
        }
        runningGames.forEach(services.games()::stop);

        List<String> unready = new ArrayList<>();
        for (Player player : targets) {
            removeFromGame(player);
            player.sendMessage(messages.get(noticeKey));
            moveToMainServer(player, unready);
        }
        warnUnready(unready);
        return targets.size();
    }

    /**
     * Takes the player out of their game; a failure only logs a WARNING naming the player and the exception, so
     * the caller still sends them away instead of leaving them stuck on a locked server. Returns whether they
     * were actually in a game to leave.
     */
    private boolean removeFromGame(Player player) {
        try {
            return services.games().leave(player.getUniqueId(), LeaveReason.KICKED);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "Could not finish removing " + player.getName() + " from their game; sending them away anyway", e);
            return true;
        }
    }

    /** Sends the player to the main server; adds their name to {@code unready} when their proxy channel is not. */
    private void moveToMainServer(Player player, List<String> unready) {
        if (!services.proxy().ready(player)) {
            unready.add(player.getName());
        }
        services.proxy().sendToMainServer(player);
    }

    /** Logs one WARNING naming every player {@code unready} collected, if any. */
    private void warnUnready(List<String> unready) {
        if (!unready.isEmpty()) {
            logger.warning(String.join(", ", unready) + " could not be sent to the main server: their proxy "
                    + "channel isn't ready yet. " + ProxyMessenger.CHANNEL_HINT);
        }
    }

    private Arena arena(String id) {
        return services.arenas().get(id)
                .orElseThrow(() -> new PvpException(Msg.ERROR_UNKNOWN_ARENA, Map.of("arena", id)));
    }

    private Game gameOn(String arena) {
        return services.games().gameOn(arena)
                .orElseThrow(() -> new PvpException(Msg.ERROR_NO_GAME_ON_ARENA, Map.of("arena", arena)));
    }

    private String modeName(String modeId) {
        return messages.plain(Msg.modeName(modeId));
    }

    /** The mode's name; an arena file may spell its mode id in another case ("TDM"), which has no message key. */
    private String modeNameOf(Arena arena) {
        return modeName(services.modes().get(arena.modeId()).map(GameMode::id).orElse(arena.modeId()));
    }

    private static String describe(Region region) {
        if (region == null) {
            return "none (the whole world)";
        }
        Region.Bounds bounds = region.bounds();
        return region.vertices().size() + " corners, x " + bounds.minX() + ".." + bounds.maxX()
                + ", z " + bounds.minZ() + ".." + bounds.maxZ() + ", y " + region.minY() + ".." + region.maxY();
    }

    /**
     * "blue 1, red 1" from the arena's team spawns (already sorted by team id); with none, the numbered
     * {@code PlayerSpawn} points as a count; with neither, "none".
     */
    private static String describeSpawns(Arena arena) {
        if (!arena.teamSpawns().isEmpty()) {
            return arena.teamSpawns().entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue().size())
                    .collect(Collectors.joining(", "));
        }
        int numbered = SpawnPoints.numbered("PlayerSpawn").points(arena, null).size();
        if (numbered > 0) {
            return numbered + " player spawn" + (numbered == 1 ? "" : "s");
        }
        return "none";
    }
}
