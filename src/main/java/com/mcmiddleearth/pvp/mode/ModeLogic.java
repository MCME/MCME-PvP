package com.mcmiddleearth.pvp.mode;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.PlayerSession;

import java.util.Optional;
import java.util.UUID;

/**
 * Optional hooks a mode adds on top of its declared rules. Hooks change scores through {@code game.scores()}. Every
 * hook runs on the server thread inside one of the engine's steps and must not throw: a throw skips the rest of that
 * step.
 */
public interface ModeLogic {

    /** Once, when the countdown ends, before {@code phaseChanged(COUNTDOWN, RUNNING)}. */
    default void onStart(Game game) {
    }

    /**
     * A counted death (RUNNING only), after the victim's death, the killer's credit and, under {@code Respawn.NEVER},
     * the elimination. Score changes wait until after {@code playerDied}. The killer is the credited one: empty after a
     * team kill. Leaving under fire counts as a death too: then it comes before {@link #onLeave}, and score changes
     * wait until after {@code playerFled}.
     */
    default void onDeath(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
    }

    /**
     * One tick after a respawn during COUNTDOWN or RUNNING, after the kit; it can come before {@link #onStart};
     * only for a living player, not for one who respawns to spectate.
     */
    default void onRespawn(Game game, PlayerSession player) {
    }

    /**
     * A player left (any phase but CLOSED), once they are in the lobby, before {@code playerLeft} and before the
     * countdown, win and abandon checks; spectators who leave count too. Not called for the players a closing
     * game sends away.
     */
    default void onLeave(Game game, PlayerSession player) {
    }

    /** RUNNING only, for a present, living player. */
    default void onInteract(Game game, PlayerSession player, NamedPoint block) {
    }

    /** For objective zones in later modes; nothing calls it yet. */
    default void onRegionEnter(Game game, PlayerSession player, String zone) {
    }

    /** An owned entity died while RUNNING, after it is forgotten. The killer is any present PLAYER of the game. */
    default void onEntityKilled(Game game, UUID entity, Optional<PlayerSession> killer) {
    }

    /** Once a second while RUNNING, except in the second the time limit runs out. */
    default void tick(Game game) {
    }
}
