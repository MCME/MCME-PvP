package com.mcmiddleearth.pvp.game;

import java.util.Locale;

/** Why a game ended. */
public enum EndReason {
    WIN_CONDITION, TIME_LIMIT, STOPPED, SHUTDOWN, ABANDONED;

    /** The name used in match records: "win_condition", "time_limit", "stopped", "shutdown", "abandoned". */
    public String recordName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
