package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.util.UUID;

/** What a game asks the server to do with a player. Every method is a no-op for a player who is not online. */
public interface PlayerActions {

    void teleport(UUID player, NamedPoint point);

    /** Clears inventory, armor and potion effects, heals, feeds, sets ADVENTURE, turns flight off, clears fire and fall distance. */
    void resetForPlay(UUID player);

    /** Clears the inventory and gives the kit; dyed items take {@code color}'s armor color (color null: undyed). */
    void giveKit(UUID player, Kit kit, TeamSpec color);

    /** true: SPECTATOR game mode. false: back to ADVENTURE. */
    void setSpectator(UUID player, boolean spectator);

    /** Shows this game's scoreboard to the player and puts them in the scoreboard team of {@code color} (null: no team). */
    void joinScoreboard(UUID player, TeamSpec color);

    /** Back to the server's main scoreboard. */
    void leaveScoreboard(UUID player);

    /** Teleports to the lobby, clears inventory and effects, sets ADVENTURE and heals. */
    void sendToLobby(UUID player);
}
