package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.config.PvpConfig;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSetupTest {

    private ServerMock server;
    private final RecordingLog log = new RecordingLog();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsTheConfiguredWorldsAndSwitchesImmediateRespawnOn() {
        WorldSetup.apply(worlds("world", "arena2"), log.logger());

        World arena2 = server.getWorld("arena2");
        assertNotNull(arena2);
        assertEquals(true, server.getWorld("world").getGameRuleValue(GameRules.IMMEDIATE_RESPAWN));
        assertEquals(true, arena2.getGameRuleValue(GameRules.IMMEDIATE_RESPAWN));
        assertEquals(List.of("Loaded world arena2"), log.messages(Level.INFO));
    }

    @Test
    void aWorldThatCannotBeLoadedIsLoggedAndSkipped() {
        WorldSetup.apply(worlds("world", "missing"), log.logger(), name -> null);

        assertEquals(true, server.getWorld("world").getGameRuleValue(GameRules.IMMEDIATE_RESPAWN));
        assertEquals(List.of("World missing from config.yml (worlds) could not be loaded; its arenas cannot be played"),
                log.messages(Level.WARNING));
        assertEquals(List.of(), log.messages(Level.INFO));
    }

    @Test
    void aFailingLoaderIsLoggedToo() {
        WorldSetup.apply(worlds("missing", "world"), log.logger(), name -> {
            throw new IllegalStateException("no such folder");
        });

        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(2, warnings.size());
        assertTrue(warnings.getFirst().contains("no such folder"), warnings.getFirst());
        assertEquals(true, server.getWorld("world").getGameRuleValue(GameRules.IMMEDIATE_RESPAWN));
    }

    private static PvpConfig worlds(String... names) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds", List.of(names));
        return PvpConfig.from(yaml);
    }
}
