package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.game.PlayerRef;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The player a screen is built for, or who clicks a button. {@code permissions} answers permission checks live
 * (the Paper layer passes {@code player::hasPermission}), so a check at click time sees the current grants.
 * Compare viewers by {@link #id()}, not {@code equals}: the generated {@code equals} also compares
 * {@code permissions}, a {@link Predicate} that two otherwise-identical viewers will rarely share by reference.
 */
public record Viewer(UUID id, String name, Predicate<String> permissions) {

    public Viewer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permissions, "permissions");
    }

    public boolean has(String permission) {
        return permissions.test(permission);
    }

    public PlayerRef ref() {
        return new PlayerRef(id, name);
    }
}
