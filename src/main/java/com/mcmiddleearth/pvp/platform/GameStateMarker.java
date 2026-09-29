package com.mcmiddleearth.pvp.platform;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Marks a player's saved file while a game has put them in game state (spectating, or reset for play), so a later
 * join can tell the difference between that and a player who was never touched.
 */
public final class GameStateMarker {

    private final NamespacedKey key;

    public GameStateMarker(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "in-game");
    }

    public void mark(Player player) {
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    public void clear(Player player) {
        player.getPersistentDataContainer().remove(key);
    }

    public boolean marked(Player player) {
        return player.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
