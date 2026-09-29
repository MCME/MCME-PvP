package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.GameTeam;
import com.mcmiddleearth.pvp.game.JoinResult;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The plain-chat side of every game event: 1.1.2's messages, sent only to the game's players and spectators
 * (the "game opened" notice also to the players in the lobby, and to the network through the proxy).
 */
public final class TextAnnouncer implements GameListener {

    private final PvpServices services;
    private final Messages messages;

    public TextAnnouncer(PvpServices services) {
        this.services = services;
        this.messages = services.messages();
    }

    @Override
    public void gameOpened(Game game, String openedBy) {
        TagResolver[] args = {
                Placeholder.unparsed("player", openedBy),
                Placeholder.unparsed("mode", modeName(game)),
                Placeholder.unparsed("arena", game.arena().id()),
                Placeholder.unparsed("max", String.valueOf(game.maxPlayers()))};
        Component notice = messages.get(Msg.GAME_CREATED, args);
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean outside = services.games().gameOf(player.getUniqueId()).isEmpty();
            if (outside && !services.lock().keepsOut(player::hasPermission)) {
                player.sendMessage(notice);
            }
        }
        services.proxy().broadcast(messages.legacy(Msg.GAME_PROXY_ANNOUNCEMENT, args));
    }

    @Override
    public void playerJoined(Game game, PlayerSession session, JoinResult result) {
        if (result == JoinResult.SPECTATING) {
            send(session.id(), messages.get(Msg.JOIN_SPECTATING, Map.of("arena", game.arena().id())));
            return;
        }
        send(session.id(), messages.get(Msg.JOIN_JOINED, Map.of("arena", game.arena().id(), "mode", modeName(game))));
        Optional<GameTeam> team = session.team();
        if (team.isPresent()) {
            Component teamName = SummaryLines.teamName(team.get().spec());
            send(session.id(), messages.get(Msg.JOIN_TEAM, Placeholder.component("team", teamName)));
            Component notice = messages.get(Msg.GAME_PLAYER_JOINED_TEAM,
                    Placeholder.component("player", SummaryLines.name(session)), Placeholder.component("team", teamName));
            sendAudience(game, notice, session.id());
        } else {
            sendAudience(game, messages.get(Msg.GAME_PLAYER_JOINED, Map.of("player", session.name(),
                    "count", String.valueOf(game.playerCount()), "max", String.valueOf(game.maxPlayers()))),
                    session.id());
        }
    }

    @Override
    public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
        switch (reason) {
            case LEFT -> send(session.id(), messages.get(Msg.LEAVE_LEFT));
            case KICKED -> send(session.id(), messages.get(Msg.LEAVE_KICKED));
            default -> {
            }
        }
        if (!reason.byGame() && game.phase() != Phase.ENDING && session.role() == Role.PLAYER) {
            sendAudience(game, messages.get(Msg.GAME_PLAYER_LEFT, Map.of("player", session.name())), session.id());
        }
    }

    @Override
    public void phaseChanged(Game game, Phase from, Phase to) {
        switch (to) {
            case COUNTDOWN -> {
                int seconds = game.secondsLeft();
                sendAudience(game, messages.get(
                        Msg.plural(Msg.GAME_COUNTDOWN_STARTED, Msg.GAME_COUNTDOWN_STARTED_ONE, seconds),
                        Map.of("seconds", String.valueOf(seconds))));
                // Teams are made when the countdown starts; 1.1.2 told each player theirs at this point.
                for (PlayerSession player : game.players()) {
                    player.team().ifPresent(team -> send(player.id(), messages.get(Msg.JOIN_TEAM,
                            Placeholder.component("team", SummaryLines.teamName(team.spec())))));
                }
            }
            case WAITING -> sendAudience(game, messages.get(Msg.GAME_COUNTDOWN_CANCELLED));
            case RUNNING -> sendAudience(game, messages.get(Msg.GAME_STARTED));
            case CLOSED -> {
                // A game closed before it ran has no summary; tell the players it had when it closed, either
                // that staff stopped it or that the server is stopping.
                if (from == Phase.WAITING || from == Phase.COUNTDOWN) {
                    Component stopped = messages.get(Msg.GAME_STOPPED, Map.of("arena", game.arena().id()));
                    Component shutdown = messages.get(Msg.GAME_SHUTDOWN, Map.of("arena", game.arena().id()));
                    for (PlayerSession session : game.sessions()) {
                        session.leftReason().ifPresent(reason -> {
                            if (reason == LeaveReason.GAME_CLOSED) {
                                send(session.id(), stopped);
                            } else if (reason == LeaveReason.SHUTDOWN) {
                                send(session.id(), shutdown);
                            }
                        });
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void secondPassed(Game game) {
        int left = game.secondsLeft();
        if (game.phase() == Phase.COUNTDOWN && left >= 1 && left <= 5) {
            sendAudience(game, messages.get(Msg.GAME_COUNTDOWN_TICK, Map.of("seconds", String.valueOf(left))));
        } else if (game.phase() == Phase.RUNNING && game.timeLimitSeconds() > 0
                && (left == 30 || (left >= 1 && left <= 10))) {
            String time = messages.plain(Msg.plural(Msg.TIME_SECONDS, Msg.TIME_SECOND, left),
                    Placeholder.unparsed("seconds", String.valueOf(left)));
            sendAudience(game, messages.get(Msg.GAME_TIME_WARNING, Map.of("time", time)));
        }
    }

    @Override
    public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        killer.filter(k -> !k.id().equals(victim.id())).ifPresent(k -> sendAudience(game, messages.get(Msg.GAME_DEATH,
                Placeholder.component("victim", SummaryLines.name(victim)),
                Placeholder.component("killer", SummaryLines.name(k)))));
        if (victim.role() == Role.PLAYER && !victim.alive()) {
            send(victim.id(), messages.get(Msg.GAME_ELIMINATED));
        }
    }

    /**
     * Leaving under fire has no vanilla death text, so the game hears who it counts as their killer, if anyone. The
     * player is on their way out, so they are not told that they spectate.
     */
    @Override
    public void playerFled(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        TagResolver player = Placeholder.component("player", SummaryLines.name(victim));
        sendAudience(game, killer
                .map(k -> messages.get(Msg.GAME_FLED, player, Placeholder.component("killer", SummaryLines.name(k))))
                .orElseGet(() -> messages.get(Msg.GAME_FLED_ALONE, player)));
    }

    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        SummaryLines.of(messages, game, outcome).forEach(line -> sendAudience(game, line));
    }

    private String modeName(Game game) {
        return messages.plain(Msg.modeName(game.mode().id()));
    }

    private void sendAudience(Game game, Component message, UUID... except) {
        for (UUID id : game.audience()) {
            if (!isAmong(id, except)) {
                send(id, message);
            }
        }
    }

    private static boolean isAmong(UUID id, UUID[] ids) {
        for (UUID other : ids) {
            if (other.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static void send(UUID id, Component message) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            player.sendMessage(message);
        }
    }
}
