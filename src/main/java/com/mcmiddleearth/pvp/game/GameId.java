package com.mcmiddleearth.pvp.game;

import java.util.UUID;

/** The unique id of one game; written to its match record. */
public record GameId(UUID value) {

    public static GameId random() {
        return new GameId(UUID.randomUUID());
    }

    /** The first 8 hex digits, for logs. */
    public String shortId() {
        return value.toString().substring(0, 8);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
