package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.JoinResult;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.stats.MatchRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Records game notifications as short lines ("opened AS-TDM", "phase AS-TDM WAITING->COUNTDOWN", "joined Alice
 * PLAYING", "left Alice LEFT", "died Bob by Alice", "ended AS-TDM red WIN_CONDITION", "closed AS-TDM"). The frequent
 * scoreChanged and secondPassed are only counted, so the event list stays readable.
 */
public final class RecordingListener implements GameListener {

    private final List<String> events = new ArrayList<>();
    private final List<MatchRecord> records = new ArrayList<>();
    private int scoreChanges;
    private int secondsPassed;

    public List<String> events() {
        return Collections.unmodifiableList(events);
    }

    /** The events that start with {@code prefix}, e.g. "phase ". */
    public List<String> events(String prefix) {
        return events.stream().filter(event -> event.startsWith(prefix)).toList();
    }

    /** Every match record passed to gameEnded, in order. */
    public List<MatchRecord> records() {
        return Collections.unmodifiableList(records);
    }

    /** Counted across every game this listener hears, not per game. */
    public int scoreChanges() {
        return scoreChanges;
    }

    /** Counted across every game this listener hears, not per game. */
    public int secondsPassed() {
        return secondsPassed;
    }

    @Override
    public void gameOpened(Game game, String openedBy) {
        events.add("opened " + game.arena().id());
    }

    @Override
    public void phaseChanged(Game game, Phase from, Phase to) {
        events.add("phase " + game.arena().id() + " " + from + "->" + to);
    }

    @Override
    public void playerJoined(Game game, PlayerSession session, JoinResult result) {
        events.add("joined " + session.name() + " " + result);
    }

    @Override
    public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
        events.add("left " + session.name() + " " + reason);
    }

    @Override
    public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        events.add("died " + victim.name() + killer.map(k -> " by " + k.name()).orElse(""));
    }

    @Override
    public void scoreChanged(Game game) {
        scoreChanges++;
    }

    @Override
    public void secondPassed(Game game) {
        secondsPassed++;
    }

    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        String winner = "draw";
        if (outcome.winningTeam() != null) {
            winner = outcome.winningTeam();
        } else if (outcome.winningPlayer() != null) {
            winner = game.session(outcome.winningPlayer()).map(PlayerSession::name)
                    .orElse(outcome.winningPlayer().toString());
        }
        events.add("ended " + game.arena().id() + " " + winner + " " + outcome.reason());
        records.add(record);
    }

    @Override
    public void gameClosed(Game game) {
        events.add("closed " + game.arena().id());
    }
}
