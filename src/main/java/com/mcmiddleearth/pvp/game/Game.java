package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeLogic;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.team.Palette;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/**
 * One game on one arena: its phase, sessions, teams, scores, tasks and entities. {@link GameManager} creates it and
 * is the only way in and out; the platform's event router calls the {@code handle…} methods. {@link Lifecycle},
 * {@link Joins} and {@link Combat} implement its behavior.
 */
public final class Game {

    private final GameId id;
    private final Arena arena;
    private final GameMode mode;
    private final ModeRules rules;
    private final GameSettings settings;
    private final GameContext context;
    private final GameListener listener;
    private final ModeLogic logic;
    private final List<GameTeam> teams;
    private final Roster roster = new Roster();
    private final GameEntities entities = new GameEntities();
    private final Joins joins;
    private final Lifecycle lifecycle;
    private final Combat combat;
    private final ScoreTracker scores;

    /** Creates the teams (none in modes without teams) and the mode's hooks; {@link #begin} starts the heartbeat. */
    Game(GameId id, Arena arena, GameMode mode, GameSettings settings, GameContext context, GameListener listener,
         PvpConfig config, ToDoubleFunction<UUID> skillOf, Consumer<Game> onClosed) {
        this.id = id;
        this.arena = arena;
        this.mode = mode;
        this.rules = mode.rules();
        this.settings = settings;
        this.context = context;
        this.listener = listener;
        this.logic = mode.newLogic().orElseGet(() -> new ModeLogic() {
        });
        this.teams = Palette.teams(rules.teamCount(settings)).stream().map(GameTeam::new).toList();
        SpawnCursor spawns = new SpawnCursor(arena, rules.spawnPoints());
        this.joins = new Joins(this, roster, new TeamPlacement(teams, rules.balanced(), skillOf), spawns);
        this.lifecycle = new Lifecycle(this, roster, joins, config, onClosed);
        this.combat = new Combat(this, roster, spawns, lifecycle);
        this.scores = new ScoreTracker(this, lifecycle);
    }

    public GameId id() {
        return id;
    }

    public Arena arena() {
        return arena;
    }

    public GameMode mode() {
        return mode;
    }

    /** {@code mode().rules()}. */
    public ModeRules rules() {
        return rules;
    }

    public GameSettings settings() {
        return settings;
    }

    public Phase phase() {
        return lifecycle.phase();
    }

    /** COUNTDOWN: until the start; RUNNING: until the time limit (0 without one); ENDING: until close. */
    public int secondsLeft() {
        return lifecycle.secondsLeft();
    }

    /** The TIME_LIMIT setting in seconds when the win condition uses a time limit, else 0. */
    public int timeLimitSeconds() {
        return rules.win().usesTimeLimit() && settings.has(Setting.TIME_LIMIT)
                ? settings.get(Setting.TIME_LIMIT) * 60
                : 0;
    }

    public Optional<Instant> startedAt() {
        return lifecycle.startedAt();
    }

    public Optional<Outcome> outcome() {
        return lifecycle.outcome();
    }

    /** The standings as they were the moment the game ended, once it has reached ENDING. */
    public Optional<EndStandings> endStandings() {
        return lifecycle.endStandings();
    }

    /** In palette order; empty in modes without teams. */
    public List<GameTeam> teams() {
        return teams;
    }

    public Optional<GameTeam> team(String teamId) {
        return teams.stream().filter(team -> team.id().equalsIgnoreCase(teamId)).findFirst();
    }

    /** The player's session, present or not. */
    public Optional<PlayerSession> session(UUID player) {
        return roster.session(player);
    }

    /** Everyone who ever joined, in join order. */
    public List<PlayerSession> sessions() {
        return roster.all();
    }

    /** Present sessions with role PLAYER, alive or eliminated. */
    public List<PlayerSession> players() {
        return roster.players();
    }

    /** Present sessions with role SPECTATOR. */
    public List<PlayerSession> spectators() {
        return roster.spectators();
    }

    /** Present players and spectators: who sees this game's messages and HUD. */
    public List<UUID> audience() {
        return roster.present().stream().map(PlayerSession::id).toList();
    }

    public int playerCount() {
        return players().size();
    }

    public int maxPlayers() {
        return settings.get(Setting.MAX_PLAYERS);
    }

    /** WAITING, COUNTDOWN or RUNNING. */
    public boolean acceptsJoins() {
        Phase phase = phase();
        return phase == Phase.WAITING || phase == Phase.COUNTDOWN || phase == Phase.RUNNING;
    }

    /**
     * Whether a unit of this size would play rather than spectate or be refused if it joined now, by the same
     * checks as joining, without side effects. It does not know who joins, so it leaves out that a player eliminated
     * from this game earlier only comes back to watch.
     */
    public boolean canJoinAsPlayer(int unitSize) {
        return joins.canJoinAsPlayer(unitSize);
    }

    /** During the countdown players stay where they spawned; the platform enforces it. */
    public boolean isFrozen(UUID player) {
        return phase() == Phase.COUNTDOWN
                && roster.present(player).filter(session -> session.role() == Role.PLAYER).isPresent();
    }

    /** Spawn protection is running for the player. */
    public boolean isProtected(UUID player) {
        return phase() == Phase.RUNNING && roster.present(player).filter(PlayerSession::isProtected).isPresent();
    }

    public ScoreTracker scores() {
        return scores;
    }

    public GameEntities entities() {
        return entities;
    }

    /** Spawns an entity this game owns; it is removed when the game closes. */
    public UUID spawnEntity(String entityType, NamedPoint at) {
        if (phase() == Phase.CLOSED) {
            throw new IllegalStateException("game " + id.shortId() + " is closed");
        }
        UUID entity = context.world().spawnEntity(entityType, at);
        entities.add(entity);
        return entity;
    }

    /**
     * A player died. It counts only while RUNNING and only for a present, living player; the killer earns the kill
     * when they are another present player of this game and not a teammate. {@code killer} is nullable.
     */
    public void handleDeath(UUID victim, UUID killer) {
        combat.death(victim, killer);
    }

    /**
     * Whom {@link #handleDeath} would credit with the kill if {@code victim} died now, without changing anything:
     * empty when the death wouldn't count or no one earns it. {@code killer} is nullable.
     */
    public Optional<PlayerSession> creditedKiller(UUID victim, UUID killer) {
        return combat.creditFor(victim, killer);
    }

    /**
     * Another player hurt the player, directly or with a projectile, in damage the game allowed. While RUNNING, a
     * present, living player remembers the last hit by another present PLAYER of this game, with its tick: leaving
     * or quitting within 10 seconds of it counts as a death, and as a kill for that player by the same rules as
     * {@link #handleDeath}. A later hit replaces it; a death, a respawn or leaving forgets it.
     */
    public void handleHit(UUID victim, UUID attacker) {
        combat.hit(victim, attacker);
    }

    /**
     * Where the player respawns: a living player during the countdown or a running game at the next spawn of their
     * team (or of everyone), anyone else at the arena spawn; empty when they are not in the game. It uses up that
     * spawn, so call it once per respawn.
     */
    public Optional<NamedPoint> respawnPoint(UUID player) {
        return combat.respawnPoint(player);
    }

    /**
     * One tick after a respawn: a living player during the countdown or a running game gets a reset, a kit and,
     * while RUNNING, spawn protection; eliminated players and spectators spectate again; a waiting player is reset.
     */
    public void handleRespawn(UUID player) {
        combat.respawn(player);
    }

    /**
     * An entity this game owns died: it is forgotten in every phase, and the mode hears of it only while RUNNING.
     * {@code killer} is nullable.
     */
    public void handleEntityDeath(UUID entity, UUID killer) {
        combat.entityDeath(entity, killer);
    }

    /** A block click, handed to the mode while RUNNING, for a present, living player. */
    public void handleInteract(UUID player, NamedPoint block) {
        combat.interact(player, block);
    }

    @Override
    public String toString() {
        return "Game[" + id.shortId() + " " + mode.id() + " on " + arena.id() + ", " + phase() + "]";
    }

    void begin() {
        lifecycle.begin();
    }

    /**
     * A unit joins: as players while WAITING; during the countdown or a running game as players or as spectators, by
     * the mode's mid-game rule, except that a player eliminated from this game earlier only comes back to watch.
     */
    JoinResult add(JoinUnit unit) {
        return joins.add(unit);
    }

    /** A spectator joins; refused once the game has ended. */
    void addSpectator(PlayerRef player) {
        joins.addSpectator(player);
    }

    /**
     * The player leaves (back to the lobby), then the phase reacts: a countdown may go back to WAITING, an empty
     * running game ends. Leaving or quitting under fire (see {@link #handleHit}) first counts as a death; when that
     * death ends the game, the leave follows as from any ended game, unless closing the game at once already sent
     * everyone, this player too, to the lobby. Each step runs even when an earlier one throws a RuntimeException:
     * the phase follow-up runs once the session is marked gone, so a player who cannot be reached does not also
     * leave the game stuck mid-countdown. The first failure is rethrown at the end, with any later one attached to
     * it as a suppressed exception.
     */
    void remove(UUID player, LeaveReason reason) {
        Optional<PlayerSession> session = roster.present(player);
        if (session.isEmpty()) {
            return;
        }
        RuntimeException failure = Steps.attempt(null, () -> combat.leaving(session.get(), reason));
        if (session.get().present()) {
            failure = Steps.attempt(failure, () -> joins.remove(session.get(), reason));
            failure = Steps.attempt(failure, lifecycle::afterLeave);
        }
        if (failure != null) {
            throw failure;
        }
    }

    void startCountdown() {
        lifecycle.startCountdown();
    }

    void end(Outcome outcome) {
        lifecycle.end(outcome);
    }

    void close() {
        lifecycle.close();
    }

    /** As {@link #close()}, but every present session leaves with {@code reason}. */
    void close(LeaveReason reason) {
        lifecycle.close(reason);
    }

    GameContext context() {
        return context;
    }

    GameListener listener() {
        return listener;
    }

    ModeLogic logic() {
        return logic;
    }

    Optional<Instant> endedAt() {
        return lifecycle.endedAt();
    }
}
