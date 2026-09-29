package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.Lobby;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.platform.ServerLock;
import com.mcmiddleearth.pvp.stats.StatsService;
import com.mcmiddleearth.pvp.text.Messages;

/** The services that commands, listeners and menus share; built once in {@link PvpPlugin#onEnable()}. */
public record PvpServices(PvpConfig config, Messages messages, ArenaRegistry arenas, ModeRegistry modes, GameManager games,
                          StatsService stats, Lobby lobby, ServerLock lock, ProxyMessenger proxy) {
}
