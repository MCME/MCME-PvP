package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One player's place in one game: role, team or personal color, and this game's counters. A session outlives leaving,
 * because the match record lists everyone who took part; only the game changes it (the mutators are package-private).
 */
public final class PlayerSession {

    private final int joinOrder;
    private PlayerRef player;
    private Role role = Role.PLAYER;
    private GameTeam team;
    private TeamSpec personalColor;
    private boolean eliminated;
    private boolean present = true;
    private boolean participated;
    private int kills;
    private int deaths;
    private int score;
    private int protectedSeconds;
    private LeaveReason leftReason;
    /** The last player of the game who hit them, and the tick of that hit; null when there is none to count. */
    private UUID hitBy;
    private long hitAt;

    PlayerSession(PlayerRef player, int joinOrder) {
        this.player = player;
        this.joinOrder = joinOrder;
    }

    public PlayerRef player() {
        return player;
    }

    public UUID id() {
        return player.id();
    }

    public String name() {
        return player.name();
    }

    public Role role() {
        return role;
    }

    public Optional<GameTeam> team() {
        return Optional.ofNullable(team);
    }

    /** The team's spec, or the personal color in modes without teams; empty while WAITING and for spectators. */
    public Optional<TeamSpec> color() {
        if (role != Role.PLAYER) {
            return Optional.empty();
        }
        // Read once: a caller off the server thread (PlaceholderAPI) could otherwise see the field turn null
        // between the check and the use, when a game clears the team concurrently.
        GameTeam current = team;
        return current != null ? Optional.of(current.spec()) : Optional.ofNullable(personalColor);
    }

    /** A PLAYER who has not been eliminated. Does not check {@link #present()}. */
    public boolean alive() {
        return role == Role.PLAYER && !eliminated;
    }

    /**
     * Out of this game for good, by a counted death in a mode without respawns. It lasts through leaving and coming
     * back to watch, so the match record still gives them their team's result.
     */
    public boolean eliminated() {
        return eliminated;
    }

    /** Still in the game. */
    public boolean present() {
        return present;
    }

    /** Was a PLAYER once the countdown began; listed in the match record. */
    public boolean participated() {
        return participated;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int score() {
        return score;
    }

    /** Kills per death, as 1.1.2 showed it: the kills themselves when there are no deaths. */
    public double killDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public Optional<LeaveReason> leftReason() {
        return Optional.ofNullable(leftReason);
    }

    @Override
    public String toString() {
        return "PlayerSession[" + player.name() + " " + role + (eliminated ? " eliminated" : "")
                + (present ? "" : " gone") + "]";
    }

    int joinOrder() {
        return joinOrder;
    }

    /** Back in the game after leaving; the name may have changed since. */
    void reactivate(PlayerRef ref) {
        if (!ref.id().equals(player.id())) {
            throw new IllegalArgumentException("reactivate: " + ref.id() + " is not this session's " + player.id());
        }
        player = ref;
        present = true;
        leftReason = null;
    }

    void play() {
        role = Role.PLAYER;
        eliminated = false;
    }

    /**
     * A spectator shows no color; a team they played for earlier stays, so the match record still lists them there,
     * and so does an elimination, which is final for the rest of the game.
     */
    void spectate() {
        role = Role.SPECTATOR;
        protectedSeconds = 0;
    }

    void eliminate() {
        eliminated = true;
        protectedSeconds = 0;
    }

    void markParticipated() {
        participated = true;
    }

    void joinTeam(GameTeam newTeam) {
        Objects.requireNonNull(newTeam, "newTeam");
        if (team != null) {
            team.drop(this);
        }
        team = newTeam;
        newTeam.add(this);
    }

    void personalColor(TeamSpec color) {
        personalColor = color;
    }

    /** No team and no color, as while WAITING. */
    void clearPlacement() {
        if (team != null) {
            team.drop(this);
            team = null;
        }
        personalColor = null;
    }

    void clearParticipation() {
        participated = false;
    }

    void addKill() {
        kills++;
    }

    void addDeath() {
        deaths++;
    }

    void addScore(int points) {
        score += points;
    }

    void protectFor(int seconds) {
        protectedSeconds = seconds;
    }

    boolean isProtected() {
        return protectedSeconds > 0;
    }

    void tickProtection() {
        if (protectedSeconds > 0) {
            protectedSeconds--;
        }
    }

    void clearProtection() {
        protectedSeconds = 0;
    }

    /** Another player of the game hit them at {@code tick}; this replaces any earlier hit. */
    void hitBy(UUID attacker, long tick) {
        hitBy = attacker;
        hitAt = tick;
    }

    void clearHit() {
        hitBy = null;
    }

    /** Who hit them last, when that hit was at most {@code ticks} before the tick {@code now}. */
    Optional<UUID> hitWithin(long now, long ticks) {
        return hitBy != null && now - hitAt <= ticks ? Optional.of(hitBy) : Optional.empty();
    }

    /** Leaving ends the stretch of play a hit belongs to, so a player who comes back starts without one. */
    void leave(LeaveReason reason) {
        Objects.requireNonNull(reason, "reason");
        present = false;
        leftReason = reason;
        protectedSeconds = 0;
        hitBy = null;
    }
}
