package com.mcmiddleearth.pvp.game;

import java.util.UUID;

/** Sends a player who is not in a game to the lobby (for /pvp lobby and the menu's Lobby button). */
public interface Lobby {

    void send(UUID player);
}
