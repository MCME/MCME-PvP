package com.mcmiddleearth.mcme.pvp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginEnableTest {

    private ServerMock server;
    private PVPPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(PVPPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginEnables() {
        assertTrue(plugin.isEnabled());
    }

    @Test
    void defaultConfigLoads() {
        assertEquals(2, plugin.getConfig().getInt("PVP.Broadcast_minutes"));
        assertEquals(List.of("world"), plugin.getConfig().getStringList("noHunger"));
        assertTrue(plugin.getConfig().getBoolean("lavaDamage"));
    }

    @Test
    void pluginYmlVersionIsFiltered() {
        String version = plugin.getPluginMeta().getVersion();
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"), "unfiltered version: " + version);
    }

    @Test
    void declaresApiVersion26_2() {
        assertEquals("26.2", plugin.getPluginMeta().getAPIVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WorldJump", "World", "PlugUp", "locker", "event", "winter", "summer", "pvp", "t", "mapeditor"})
    void registersCommand(String name) {
        assertNotNull(plugin.getCommand(name), "command not registered: " + name);
    }
}
