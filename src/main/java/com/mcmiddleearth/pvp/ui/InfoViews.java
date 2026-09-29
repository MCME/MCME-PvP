package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.EndStandings;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameTeam;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Read-only notices: the rules of every mode or of one mode, a player's stats, and the teams and scores of a game. */
public final class InfoViews {

    private final PvpServices services;
    private final Messages messages;

    public InfoViews(PvpServices services) {
        this.services = services;
        this.messages = services.messages();
    }

    /** Every registered mode with its rules text, so a new mode appears here without UI code. */
    public NoticeScreen rulesList() {
        List<Component> body = new ArrayList<>();
        for (GameMode mode : services.modes().all()) {
            body.add(messages.get(Msg.RULES_HEADER, Map.of("mode", UiText.modeName(messages, mode))));
            body.add(messages.get(Msg.modeRules(mode.id())));
        }
        return new NoticeScreen(messages.get(Msg.UI_RULES_TITLE), body, UiText.close(messages));
    }

    public NoticeScreen rules(GameMode mode) {
        return new NoticeScreen(messages.get(Msg.RULES_HEADER, Map.of("mode", UiText.modeName(messages, mode))),
                List.of(messages.get(Msg.modeRules(mode.id()))), UiText.close(messages));
    }

    /**
     * A player's stats under {@code name}: the player's current name, because stats imported from 1.1.2 before the
     * player's first 3.0 game carry no name.
     */
    public NoticeScreen stats(PlayerStats stats, String name) {
        return new NoticeScreen(messages.get(Msg.UI_STATS_TITLE), List.of(
                messages.get(Msg.STATS_HEADER, Map.of("player", name)),
                messages.get(Msg.STATS_KILLS, Map.of(
                        "kills", String.valueOf(stats.kills()),
                        "deaths", String.valueOf(stats.deaths()),
                        "kd", String.format(Locale.ROOT, "%.2f", stats.killDeathRatio()))),
                messages.get(Msg.STATS_GAMES, Map.of(
                        "played", String.valueOf(stats.gamesPlayed()),
                        "won", String.valueOf(stats.gamesWon()),
                        "lost", String.valueOf(stats.gamesLost()),
                        "spectated", String.valueOf(stats.gamesSpectated())))),
                UiText.close(messages));
    }

    /** The viewer's own stats, or a notice that there are none. */
    public NoticeScreen statsOf(Viewer viewer) {
        return services.stats().find(viewer.id())
                .map(found -> stats(found, viewer.name()))
                .orElseGet(() -> new NoticeScreen(messages.get(Msg.UI_STATS_TITLE),
                        List.of(messages.get(Msg.STATS_NONE, Map.of("player", viewer.name()))),
                        UiText.close(messages)));
    }

    /**
     * The game's line, then: while waiting, who is waiting; while playing, each team with the players it has left,
     * or (without teams) the players by score; once the game has ended, the result first, then the standings as
     * they were at that moment, unaffected by a player moving on to another game.
     */
    public NoticeScreen scores(Game game) {
        List<Component> body = new ArrayList<>();
        if (game.phase() == Phase.ENDING) {
            endedLines(game, body);
        } else {
            body.add(UiText.gameLine(messages, game));
            if (game.phase() == Phase.WAITING) {
                waitingLine(game).ifPresent(body::add);
            } else {
                liveLines(game, body);
            }
        }
        return new NoticeScreen(messages.get(Msg.UI_SCORES_TITLE), body, UiText.close(messages));
    }

    /** Empty when nobody is waiting yet, so a spectator of an empty waiting game reads no bare "Waiting:" line. */
    private Optional<Component> waitingLine(Game game) {
        List<PlayerSession> players = game.players();
        if (players.isEmpty()) {
            return Optional.empty();
        }
        String names = String.join(", ", players.stream().map(PlayerSession::name).toList());
        return Optional.of(messages.get(Msg.UI_SCORES_WAITING, Map.of("players", names)));
    }

    /** Teams are filled when the countdown starts; before that every team would read "0 left". */
    private void liveLines(Game game, List<Component> body) {
        if (game.teams().isEmpty()) {
            for (PlayerSession player : game.scores().ranking()) {
                if (player.present()) {
                    addPlayerLine(body, player.name(), player.score());
                }
            }
        } else {
            for (GameTeam team : game.teams()) {
                addTeamLine(body, UiText.team(team.spec()), team.aliveCount());
            }
        }
    }

    /** The game line, the result, and the standings, all frozen at the moment the game ended. */
    private void endedLines(Game game, List<Component> body) {
        Optional<EndStandings> standings = game.endStandings();
        int count = standings.map(EndStandings::playerCount).orElseGet(game::playerCount);
        body.add(UiText.gameLine(messages, game, count));
        game.outcome().ifPresent(outcome -> body.add(resultLine(game, outcome)));
        standings.ifPresent(s -> {
            for (EndStandings.TeamStanding team : s.teams()) {
                addTeamLine(body, UiText.team(team.spec()), team.aliveCount());
            }
            for (EndStandings.PlayerStanding player : s.players()) {
                addPlayerLine(body, player.name(), player.score());
            }
        });
    }

    /**
     * The reason first, when it isn't a fought result (the same texts the chat summary and end-of-game announcement
     * use for a stopped, shut-down or abandoned game); otherwise who won, or that it was a draw.
     */
    private Component resultLine(Game game, Outcome outcome) {
        Map<String, String> arena = Map.of("arena", game.arena().id());
        switch (outcome.reason()) {
            case STOPPED -> {
                return messages.get(Msg.GAME_STOPPED, arena);
            }
            case SHUTDOWN -> {
                return messages.get(Msg.GAME_SHUTDOWN, arena);
            }
            case ABANDONED -> {
                return messages.get(Msg.GAME_ABANDONED, arena);
            }
            default -> {
            }
        }
        if (outcome.winningTeam() != null) {
            Optional<GameTeam> team = game.team(outcome.winningTeam());
            if (team.isPresent()) {
                Component name = UiText.team(team.get().spec());
                return messages.get(Msg.SUMMARY_TEAM_WON, Placeholder.component("team", name));
            }
        }
        if (outcome.winningPlayer() != null) {
            Optional<PlayerSession> winner = game.session(outcome.winningPlayer());
            if (winner.isPresent()) {
                int score = winner.get().score();
                return messages.get(Msg.plural(Msg.SUMMARY_PLAYER_WON, Msg.SUMMARY_PLAYER_WON_ONE, score),
                        Map.of("player", winner.get().name(), "score", String.valueOf(score)));
            }
        }
        return messages.get(Msg.SUMMARY_DRAW);
    }

    private void addTeamLine(List<Component> body, Component teamName, int alive) {
        body.add(messages.get(Msg.HUD_SIDEBAR_TEAM_ALIVE, Placeholder.component("team", teamName),
                Placeholder.unparsed("alive", String.valueOf(alive))));
    }

    private void addPlayerLine(List<Component> body, String name, int score) {
        body.add(messages.get(Msg.HUD_SIDEBAR_PLAYER, Map.of("player", name, "score", String.valueOf(score))));
    }
}
