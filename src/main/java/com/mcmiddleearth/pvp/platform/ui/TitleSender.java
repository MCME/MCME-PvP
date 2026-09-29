package com.mcmiddleearth.pvp.platform.ui;

import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/** Shows a title to a player. A seam: MockBukkit drops Adventure titles, so the HUD tests record them instead. */
@FunctionalInterface
interface TitleSender {

    void show(Player player, Title title);

    /** Takes a title that is still on screen away. */
    default void clear(Player player) {
        player.clearTitle();
    }
}
