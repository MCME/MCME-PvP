package com.mcmiddleearth.pvp.game;

import java.util.UUID;

/** A player by uuid and current name. */
public record PlayerRef(UUID id, String name) {
}
