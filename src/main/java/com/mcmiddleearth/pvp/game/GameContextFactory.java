package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.Arena;

/** Creates the context of each new game. */
public interface GameContextFactory {

    GameContext create(GameId id, Arena arena);
}
