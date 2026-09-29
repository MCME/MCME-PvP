package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.ui.Viewer;
import org.bukkit.entity.Player;

/** Makes the plain-Java {@link Viewer} of a Paper player. */
final class Viewers {

    private Viewers() {
    }

    /** Permissions are asked of the player each time, so they are current when a button is clicked. */
    static Viewer of(Player player) {
        return new Viewer(player.getUniqueId(), player.getName(), player::hasPermission);
    }
}
