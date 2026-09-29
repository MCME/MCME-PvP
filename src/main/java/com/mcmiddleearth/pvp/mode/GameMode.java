package com.mcmiddleearth.pvp.mode;

import java.util.Optional;
import java.util.Set;

/** A game mode: its declared rules, plus optional hooks for special mechanics. */
public interface GameMode {

    /** Short id used in arena files, config.yml and messages.yml, e.g. "tdm". */
    String id();

    /** The 1.1.2 gmType names of this mode; compared after removing whitespace, ignoring case. */
    Set<String> legacyNames();

    ModeRules rules();

    /** Hooks for special mechanics; one new instance per game. */
    default Optional<ModeLogic> newLogic() {
        return Optional.empty();
    }
}
