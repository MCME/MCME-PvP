package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One team of one game. It remembers every session placed on it, so players who left still count for the match
 * record; {@link #members()} and {@link #aliveCount()} only count those still in the game.
 */
public final class GameTeam {

    private final TeamSpec spec;
    private final List<PlayerSession> assigned = new ArrayList<>();
    private int startingSize;
    private int score;

    GameTeam(TeamSpec spec) {
        this.spec = spec;
    }

    public TeamSpec spec() {
        return spec;
    }

    /** {@code spec().id()}. */
    public String id() {
        return spec.id();
    }

    /** Present members, alive or not, in join order. A member who became a spectator is not listed. */
    public List<PlayerSession> members() {
        return assigned.stream()
                .filter(session -> session.present() && session.role() == Role.PLAYER)
                .sorted(Comparator.comparingInt(PlayerSession::joinOrder))
                .toList();
    }

    /** Present members who are alive PLAYERs. */
    public int aliveCount() {
        return (int) assigned.stream().filter(s -> s.present() && s.alive()).count();
    }

    /** Members when the game started running (during the countdown: when it began; 0 before). */
    public int startingSize() {
        return startingSize;
    }

    public int score() {
        return score;
    }

    @Override
    public String toString() {
        return "GameTeam[" + spec.id() + "]";
    }

    void add(PlayerSession session) {
        if (!assigned.contains(session)) {
            assigned.add(session);
        }
    }

    void drop(PlayerSession session) {
        assigned.remove(session);
    }

    void startingSize(int size) {
        startingSize = size;
    }

    void addScore(int points) {
        score += points;
    }
}
