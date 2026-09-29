package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.ScoreTracker;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The chat summary of a finished game, in 1.1.2's order, plus a most-kills list for modes without teams: the
 * result, the best K/D and the most deaths, each a top 3, plus the most kills where teams don't already decide it.
 * Also the colored names the announcer uses.
 */
final class SummaryLines {

    private static final int TOP = 3;

    private SummaryLines() {
    }

    static List<Component> of(Messages messages, Game game, Outcome outcome) {
        List<Component> lines = new ArrayList<>();
        lines.add(messages.get(Msg.SUMMARY_HEADER, Map.of(
                "mode", messages.plain(Msg.modeName(game.mode().id())), "arena", game.arena().id())));
        lines.add(result(messages, game, outcome));
        ScoreTracker scores = game.scores();
        top(messages, Msg.SUMMARY_BEST_KD, scores.bestKillDeath(TOP), s -> killDeath(s.kills(), s.deaths()))
                .ifPresent(lines::add);
        if (!game.rules().teams().hasTeams()) {
            top(messages, Msg.SUMMARY_MOST_KILLS, scores.mostKills(TOP), s -> String.valueOf(s.kills()))
                    .ifPresent(lines::add);
        }
        top(messages, Msg.SUMMARY_MOST_DEATHS, scores.mostDeaths(TOP), s -> String.valueOf(s.deaths()))
                .ifPresent(lines::add);
        return lines;
    }

    /** 1.1.2's K/D text: the kills when there are no deaths, else kills / deaths rounded to 2 decimals ("0.33"). */
    static String killDeath(int kills, int deaths) {
        double ratio = deaths == 0 ? kills : Math.round(kills * 100.0 / deaths) / 100.0;
        return String.valueOf(ratio);
    }

    /** A player's name in their team or personal color (white without one). */
    static Component name(PlayerSession session) {
        return Component.text(session.name(), session.color().map(TeamSpec::color).orElse(NamedTextColor.WHITE));
    }

    /** A team's name in its color, e.g. "Red" in red. */
    static Component teamName(TeamSpec team) {
        return Component.text(team.name(), team.color());
    }

    private static Component result(Messages messages, Game game, Outcome outcome) {
        String arena = game.arena().id();
        switch (outcome.reason()) {
            case STOPPED -> {
                return messages.get(Msg.GAME_STOPPED, Map.of("arena", arena));
            }
            case SHUTDOWN -> {
                return messages.get(Msg.GAME_SHUTDOWN, Map.of("arena", arena));
            }
            case ABANDONED -> {
                return messages.get(Msg.GAME_ABANDONED, Map.of("arena", arena));
            }
            default -> {
                if (outcome.winningTeam() != null) {
                    Optional<Component> team = game.team(outcome.winningTeam()).map(t -> teamName(t.spec()));
                    if (team.isPresent()) {
                        return messages.get(Msg.SUMMARY_TEAM_WON, Placeholder.component("team", team.get()));
                    }
                }
                if (outcome.winningPlayer() != null) {
                    Optional<PlayerSession> winner = game.session(outcome.winningPlayer());
                    if (winner.isPresent()) {
                        int score = winner.get().score();
                        return messages.get(Msg.plural(Msg.SUMMARY_PLAYER_WON, Msg.SUMMARY_PLAYER_WON_ONE, score),
                                Placeholder.component("player", name(winner.get())),
                                Placeholder.unparsed("score", String.valueOf(score)));
                    }
                }
                return messages.get(Msg.SUMMARY_DRAW);
            }
        }
    }

    private static Optional<Component> top(Messages messages, String key, List<PlayerSession> sessions,
                                           Function<PlayerSession, String> value) {
        if (sessions.isEmpty()) {
            return Optional.empty();
        }
        List<Component> entries = sessions.stream()
                .map(s -> messages.get(Msg.SUMMARY_ENTRY, Placeholder.component("player", name(s)),
                        Placeholder.unparsed("value", value.apply(s))))
                .toList();
        Component joined = Component.join(JoinConfiguration.separator(messages.get(Msg.SUMMARY_SEPARATOR)), entries);
        return Optional.of(messages.get(key, Placeholder.component("entries", joined)));
    }
}
