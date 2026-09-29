package com.mcmiddleearth.pvp.mode.rules;

import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.IntSetting;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Setting;

import java.util.List;
import java.util.Optional;

/** Decides when a running game is over and who won. */
public interface WinCondition {

    /** Checked after every death, leave, entity death and score change, and once a second, while the game runs. */
    Optional<Outcome> evaluate(Game game);

    /** True when the game ends at its time limit (the mode then has the TIME_LIMIT setting). */
    default boolean usesTimeLimit() {
        return false;
    }

    /** The result when the time limit runs out; only called when {@link #usesTimeLimit()}. */
    default Outcome timeUp(Game game) {
        return Outcome.draw(EndReason.TIME_LIMIT);
    }

    /** The settings this condition reads. */
    default List<Setting> settings() {
        return List.of();
    }

    /** This condition, or else: the first player or team to reach {@code goal} points wins (a goal of 0 is off). */
    default WinCondition orFirstTo(IntSetting goal) {
        return WinConditions.either(this, WinConditions.firstTo(goal));
    }
}
