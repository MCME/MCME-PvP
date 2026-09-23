package com.mcmiddleearth.mcme.pvp;

import com.mcmiddleearth.mcme.pvp.Gamemode.BasePluginGamemode;
import com.mcmiddleearth.mcme.pvp.Handlers.BukkitTeamHandler;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({"aqua,aqua", "blue,blue", "darkaqua,dark_aqua", "darkgreen,dark_green", "darkpurple,dark_purple",
            "darkred,dark_red", "gold,gold", "gray,gray", "green,green", "lightpurple,light_purple", "red,red",
            "yellow,yellow"})
    void colorTeamsHaveTheirColor(String teamName, String colorName) {
        Team team = BasePluginGamemode.getScoreboard().getTeam(teamName);
        assertNotNull(team, "team not registered: " + teamName);
        assertTrue(team.hasColor(), "team has no color: " + teamName);
        assertEquals(NamedTextColor.NAMES.value(colorName), team.color(), "wrong color for team " + teamName);
    }

    @Test
    void recolorsATeamThatAlreadyExists() {
        Team aqua = BasePluginGamemode.getScoreboard().getTeam("aqua");
        aqua.color(NamedTextColor.RED);
        BukkitTeamHandler.configureBukkitTeams();
        assertEquals(NamedTextColor.AQUA, aqua.color());
    }
}
