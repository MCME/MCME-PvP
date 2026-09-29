package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.stats.MatchRecord;

import java.time.Instant;
import java.util.List;

/** Builds the match record of a game that is ending. */
final class MatchRecords {

    private MatchRecords() {
    }

    static MatchRecord create(Game game, Outcome outcome) {
        Instant endedAt = game.endedAt().orElseThrow();
        Instant startedAt = game.startedAt().orElse(endedAt);
        List<PlayerSession> participants = game.sessions().stream().filter(PlayerSession::participated).toList();
        List<MatchRecord.TeamEntry> teams = game.teams().stream()
                .map(team -> new MatchRecord.TeamEntry(team.id(), participants.stream()
                        .filter(session -> session.team().filter(team::equals).isPresent())
                        .map(session -> session.id().toString())
                        .toList()))
                .toList();
        List<MatchRecord.Participant> entries = participants.stream()
                .map(session -> new MatchRecord.Participant(session.id().toString(), session.name(),
                        session.team().map(GameTeam::id).orElse(null), session.kills(), session.deaths(),
                        session.score(), result(session, outcome)))
                .toList();
        String winner = outcome.winningTeam() != null ? outcome.winningTeam()
                : outcome.winningPlayer() != null ? outcome.winningPlayer().toString() : null;
        return new MatchRecord(MatchRecord.SCHEMA_VERSION, game.id().toString(), game.mode().id(), game.arena().id(),
                startedAt.toString(), endedAt.toString(), game.settings().asMap(), teams, entries, winner,
                outcome.reason().recordName());
    }

    /**
     * An eliminated participant was out of the fight anyway, so they share their team's result whether they stayed,
     * left, quit or came back to watch. Anyone else who left is {@link MatchRecord#LEFT}, also after coming back
     * only to watch: watching does not undo leaving.
     */
    private static String result(PlayerSession session, Outcome outcome) {
        boolean left = !session.eliminated() && (session.present()
                ? session.role() != Role.PLAYER
                : session.leftReason().filter(reason -> !reason.byGame()).isPresent());
        if (left) {
            return MatchRecord.LEFT;
        }
        if (outcome.draw()) {
            return MatchRecord.DRAW;
        }
        boolean won = outcome.winningTeam() != null
                ? session.team().map(team -> team.id().equals(outcome.winningTeam())).orElse(false)
                : session.id().equals(outcome.winningPlayer());
        return won ? MatchRecord.WON : MatchRecord.LOST;
    }
}
