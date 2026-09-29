package com.mcmiddleearth.pvp.game;

import java.util.Objects;
import java.util.UUID;

/** How a game ended. At most one winner is set; neither means a draw. Both winner fields are nullable. */
public record Outcome(EndReason reason, String winningTeam, UUID winningPlayer) {

    public Outcome {
        Objects.requireNonNull(reason, "reason");
        if (winningTeam != null && winningPlayer != null) {
            throw new IllegalArgumentException("an outcome has at most one winner");
        }
    }

    public static Outcome team(String teamId, EndReason reason) {
        return new Outcome(reason, teamId, null);
    }

    public static Outcome player(UUID playerId, EndReason reason) {
        return new Outcome(reason, null, playerId);
    }

    public static Outcome draw(EndReason reason) {
        return new Outcome(reason, null, null);
    }

    public boolean draw() {
        return winningTeam == null && winningPlayer == null;
    }
}
