package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.text.Msg;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The phase of one game and what happens when it changes: the countdown, the start, the
 * time limit, the win check, the ending and closing. {@link Game} delegates its lifecycle methods here.
 */
final class Lifecycle {

    private final Game game;
    private final Roster roster;
    private final Joins joins;
    private final PvpConfig config;
    private final Consumer<Game> onClosed;
    private TaskHandle heartbeat;
    private Phase phase = Phase.WAITING;
    private int secondsLeft;
    private Instant startedAt;
    private Instant endedAt;
    private Outcome outcome;
    private EndStandings endStandings;
    private boolean holdScoreEvents;

    Lifecycle(Game game, Roster roster, Joins joins, PvpConfig config, Consumer<Game> onClosed) {
        this.game = game;
        this.roster = roster;
        this.joins = joins;
        this.config = config;
        this.onClosed = onClosed;
    }

    Phase phase() {
        return phase;
    }

    int secondsLeft() {
        return secondsLeft;
    }

    Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    Optional<Instant> endedAt() {
        return Optional.ofNullable(endedAt);
    }

    Optional<Outcome> outcome() {
        return Optional.ofNullable(outcome);
    }

    /** The standings frozen at {@link #end}, once the game has reached ENDING. */
    Optional<EndStandings> endStandings() {
        return Optional.ofNullable(endStandings);
    }

    /** The heartbeat that drives the countdown, the time limit and the ending. */
    void begin() {
        heartbeat = game.context().tasks().every(20, this::tick);
    }

    /**
     * Once a player is out: a countdown without enough players, or with an empty team, stops, and an
     * empty running game ends.
     */
    void afterLeave() {
        if (phase == Phase.COUNTDOWN && (game.playerCount() < minPlayers() || aTeamIsEmpty())) {
            joins.clearPlacement();
            phase = Phase.WAITING;
            secondsLeft = 0;
            game.listener().phaseChanged(game, Phase.COUNTDOWN, Phase.WAITING);
        } else if (phase == Phase.RUNNING) {
            checkWin();
            if (phase == Phase.RUNNING && game.players().isEmpty()) {
                end(Outcome.draw(EndReason.ABANDONED));
            }
        }
    }

    /**
     * The countdown lasts exactly its seconds, not up to one second less. Never called from {@code end}:
     * that also runs during plugin disable, when Paper refuses new tasks.
     */
    private void restartHeartbeat() {
        heartbeat.cancel();
        begin();
    }

    /**
     * Places the waiting players on teams and spawns, and starts the countdown; refused unless WAITING with enough
     * players.
     */
    void startCountdown() {
        if (phase != Phase.WAITING) {
            throw new PvpException(Msg.ERROR_COUNTDOWN_PHASE);
        }
        if (game.playerCount() < minPlayers()) {
            throw new PvpException(Msg.ERROR_NOT_ENOUGH_PLAYERS,
                    Map.of("min", String.valueOf(minPlayers()), "count", String.valueOf(game.playerCount())));
        }
        joins.placeForCountdown();
        restartHeartbeat();
        secondsLeft = config.countdownSeconds();
        phase = Phase.COUNTDOWN;
        game.listener().phaseChanged(game, Phase.WAITING, Phase.COUNTDOWN);
    }

    /** Once a second: the countdown, the time limit and the ending's seconds. */
    void tick() {
        switch (phase) {
            case WAITING, CLOSED -> {
            }
            case COUNTDOWN -> {
                if (--secondsLeft <= 0) {
                    start();
                }
            }
            case RUNNING -> runSecond();
            case ENDING -> {
                if (--secondsLeft <= 0) {
                    close();
                }
            }
        }
        if (phase != Phase.CLOSED) {
            game.listener().secondPassed(game);
        }
    }

    /**
     * Ends the game (ignored once ENDING or CLOSED): the outcome and match record, the result for endingSeconds, then
     * closing (at once when that is 0).
     */
    void end(Outcome result) {
        if (phase == Phase.ENDING || phase == Phase.CLOSED) {
            return;
        }
        Phase from = phase;
        phase = Phase.ENDING;
        endedAt = game.context().clock().instant();
        outcome = result;
        endStandings = captureStandings();
        roster.all().forEach(PlayerSession::clearProtection);
        secondsLeft = config.endingSeconds();
        MatchRecord record = MatchRecords.create(game, result);
        game.listener().phaseChanged(game, from, Phase.ENDING);
        game.listener().gameEnded(game, result, record);
        if (config.endingSeconds() == 0) {
            close();
        }
    }

    /**
     * Everyone present back to the lobby, the owned entities removed, the tasks cancelled and the context disposed;
     * then the listeners hear that the game closed. Every step runs even when an earlier one throws, and the first
     * failure is rethrown at the end. The manager forgets the game in any case, so it never keeps its arena or its
     * players.
     */
    void close() {
        close(LeaveReason.GAME_CLOSED);
    }

    /**
     * As {@link #close()}, but every present session leaves with {@code reason}. This is how a game closed by
     * {@code closeAll} at shutdown reads differently from one a staff member stopped, for a game that never ran
     * and so never had an outcome of its own.
     */
    void close(LeaveReason reason) {
        if (phase == Phase.CLOSED) {
            return;
        }
        try {
            closeSteps(reason);
        } finally {
            onClosed.accept(game);
        }
    }

    private void closeSteps(LeaveReason reason) {
        Phase from = phase;
        phase = Phase.CLOSED;
        PlayerActions players = game.context().players();
        RuntimeException failure = null;
        for (PlayerSession session : roster.present()) {
            failure = Steps.attempt(failure, () -> session.leave(reason));
            failure = Steps.attempt(failure, () -> players.setSpectator(session.id(), false));
            failure = Steps.attempt(failure, () -> players.leaveScoreboard(session.id()));
            failure = Steps.attempt(failure, () -> players.sendToLobby(session.id()));
        }
        failure = Steps.attempt(failure, () -> game.entities().removeAll(game.context().world()));
        failure = Steps.attempt(failure, () -> game.context().tasks().cancelAll());
        failure = Steps.attempt(failure, () -> game.context().dispose());
        failure = Steps.attempt(failure, () -> game.listener().phaseChanged(game, from, Phase.CLOSED));
        failure = Steps.attempt(failure, () -> game.listener().gameClosed(game));
        if (failure != null) {
            throw failure;
        }
    }

    /** The win check, only while RUNNING. */
    void checkWin() {
        if (phase == Phase.RUNNING) {
            game.rules().win().evaluate(game).ifPresent(this::end);
        }
    }

    /** After every score change ({@link ScoreTracker}): scoreChanged fires, then the win check. */
    void scoreChanged() {
        if (!holdScoreEvents) {
            game.listener().scoreChanged(game);
            checkWin();
        }
    }

    /** Runs {@code change} without scoreChanged events or win checks; the caller fires them afterwards. */
    void holdingScoreEvents(Runnable change) {
        boolean outer = holdScoreEvents;
        holdScoreEvents = true;
        try {
            change.run();
        } finally {
            holdScoreEvents = outer;
        }
    }

    private void start() {
        phase = Phase.RUNNING;
        // Players may have joined or left during the countdown: take each team's starting size again now.
        game.teams().forEach(team -> team.startingSize(team.members().size()));
        startedAt = game.context().clock().instant();
        secondsLeft = game.timeLimitSeconds();
        game.logic().onStart(game);
        game.listener().phaseChanged(game, Phase.COUNTDOWN, Phase.RUNNING);
    }

    private void runSecond() {
        roster.present().forEach(PlayerSession::tickProtection);
        if (game.timeLimitSeconds() > 0 && --secondsLeft <= 0) {
            end(game.rules().win().timeUp(game));
            return;
        }
        game.logic().tick(game);
        checkWin();
    }

    /**
     * The standings right now, for {@link #end}: each team with the players it has left, or, in modes without
     * teams, the present participants by score, best first. Taken before anyone can leave, so it stays correct
     * however long the result stays up.
     */
    private EndStandings captureStandings() {
        int count = game.playerCount();
        if (!game.teams().isEmpty()) {
            List<EndStandings.TeamStanding> teams = game.teams().stream()
                    .map(team -> new EndStandings.TeamStanding(team.spec(), team.aliveCount()))
                    .toList();
            return new EndStandings(count, teams, List.of());
        }
        List<EndStandings.PlayerStanding> players = game.scores().ranking().stream()
                .filter(session -> session.present() && session.role() == Role.PLAYER)
                .map(session -> new EndStandings.PlayerStanding(session.name(), session.score()))
                .toList();
        return new EndStandings(count, List.of(), players);
    }

    private int minPlayers() {
        return game.rules().minPlayers(game.rules().teamCount(game.settings()));
    }

    /** A team without players: starting would hand the other teams the win. Always false without teams. */
    private boolean aTeamIsEmpty() {
        return game.teams().stream().anyMatch(team -> team.members().isEmpty());
    }
}
