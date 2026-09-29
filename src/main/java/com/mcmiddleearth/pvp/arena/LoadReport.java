package com.mcmiddleearth.pvp.arena;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** What {@link ArenaRegistry#load()} found; logged at startup. Maps are sorted by id, so the log reads the same every start. */
public record LoadReport(int arenas, List<String> imported, Map<String, String> unassigned,
                         Map<String, List<String>> unknownProperties, Map<String, String> failed) {

    public LoadReport {
        imported = List.copyOf(imported);
        unassigned = Collections.unmodifiableMap(new TreeMap<>(unassigned));
        unknownProperties = Collections.unmodifiableMap(new TreeMap<>(unknownProperties));
        failed = Collections.unmodifiableMap(new TreeMap<>(failed));
    }

    /** One line for the log, e.g. "12 arenas (3 imported now), 4 unassigned, 1 failed". */
    public String summary() {
        return arenas + " arenas (" + imported.size() + " imported now), " + unassigned.size() + " unassigned, "
                + failed.size() + " failed";
    }
}
