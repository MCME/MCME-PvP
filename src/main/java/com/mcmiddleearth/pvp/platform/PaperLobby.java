package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** The PvP lobby of config.yml: where players go when they leave a game, or ask for it with /pvp lobby. */
public final class PaperLobby implements Lobby {

    private static final double DEFAULT_MAX_HEALTH = 20;

    private final NamedPoint lobby;

    public PaperLobby(NamedPoint lobby) {
        this.lobby = lobby;
    }

    /**
     * Teleports an online player to the lobby, keeping their inventory, game mode and effects ({@code /pvp lobby}
     * and the Lobby button use this outside games). Does nothing for a dead player: Paper refuses that teleport,
     * and {@link LobbyRespawn} places their eventual respawn at the lobby instead. Nothing happens for a player
     * who is offline. Leaving a game resets first: {@link PaperPlayerActions#sendToLobby}.
     */
    @Override
    public void send(UUID player) {
        Player online = Bukkit.getPlayer(player);
        if (online == null || online.isDead()) {
            return;
        }
        location().ifPresent(online::teleport);
    }

    /** The lobby's location, or empty when its world is not loaded. */
    public Optional<Location> location() {
        return Locations.location(lobby);
    }

    /**
     * Closes the open inventory view, clears inventory, armor and potion effects, resets fire, fall distance and
     * absorption, sets ADVENTURE, turns flight and gliding off, heals (not a dead player), feeds and clears
     * experience, so experience from kills does not pile up across games.
     */
    public void reset(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.clearActivePotionEffects();
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setAbsorptionAmount(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setGliding(false);
        // A dead player cannot be healed; the respawn gives them full health anyway (1.1.2 did the same check).
        if (!player.isDead()) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            player.setHealth(maxHealth == null ? DEFAULT_MAX_HEALTH : maxHealth.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(5f);
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
    }
}
