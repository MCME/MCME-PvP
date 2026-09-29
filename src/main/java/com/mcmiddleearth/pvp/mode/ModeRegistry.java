package com.mcmiddleearth.pvp.mode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The game modes this build knows, fixed at startup. Replaces 1.1.2's reflection on the gmType string. */
public final class ModeRegistry {

    private final List<GameMode> modes;

    /** @throws IllegalArgumentException when two modes share an id (ignoring case) */
    public ModeRegistry(List<GameMode> modes) {
        Set<String> ids = new HashSet<>();
        for (GameMode mode : modes) {
            if (!ids.add(mode.id().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("duplicate mode id " + mode.id());
            }
        }
        this.modes = List.copyOf(modes);
    }

    /** The mode with this id, ignoring case. */
    public Optional<GameMode> get(String id) {
        return modes.stream().filter(mode -> mode.id().equalsIgnoreCase(id)).findFirst();
    }

    /** Every mode, in registration order. */
    public List<GameMode> all() {
        return modes;
    }

    /** The mode a 1.1.2 map's gmType stands for, e.g. "Team Deathmatch" → tdm. Empty for a null or blank name. */
    public Optional<GameMode> byLegacyName(String gmType) {
        String wanted = normalize(gmType);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        return modes.stream()
                .filter(mode -> mode.legacyNames().stream().anyMatch(name -> normalize(name).equals(wanted)))
                .findFirst();
    }

    /** Whitespace removed, lowercase ({@link Locale#ROOT}); null becomes "". */
    public static String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
