package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.LoadReport;
import com.mcmiddleearth.pvp.config.ConfigMigrator;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin loads from the new plugin.yml on MockBukkit and wires config, messages, arenas and stats. Files the
 * plugin's own writer thread writes are only checked after the plugin is disabled, which closes the writer.
 */
class PluginSkeletonTest {

    private ServerMock server;
    private PvpPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(PvpPlugin.class);
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
    void versionIsFilteredFromThePom() {
        assertTrue(plugin.getPluginMeta().getVersion().matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"));
    }

    @Test
    void declaresApiVersion26_2() {
        assertEquals("26.2", plugin.getPluginMeta().getAPIVersion());
    }

    @Test
    void softDependsOnPlaceholderApiAndMultiverse() {
        assertEquals(List.of("PlaceholderAPI", "Multiverse-Core"), plugin.getPluginMeta().getPluginSoftDependencies());
        assertEquals(List.of(), plugin.getPluginMeta().getPluginDependencies());
    }

    @Test
    void theDefaultConfigIsSavedWithVersion1() {
        Path file = data().resolve("config.yml");

        assertTrue(Files.exists(file));
        assertEquals(ConfigMigrator.CURRENT_VERSION, YamlConfiguration.loadConfiguration(file.toFile()).getInt("configVersion"));
        assertEquals(PvpConfig.defaults(), plugin.services().config());
    }

    @Test
    void aLegacyConfigIsMigratedAndSavedWhenThePluginEnables() throws IOException {
        // Paper lets a plugin register its commands only during its first enable, so this test starts a new server
        // and puts the legacy file in place before the plugin enables for the first time.
        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = (PvpPlugin) server.getPluginManager().loadPlugin(PvpPlugin.class);
        Path file = data().resolve("config.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "worlds:\n- world\nnoHunger:\n- world\nPVP:\n    Enabled: true\n    Broadcast_minutes: 2\n");

        server.getPluginManager().enablePlugin(plugin);

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(1, saved.getInt("configVersion"));
        assertTrue(saved.isSet("lavaDamage"));
        assertFalse(saved.getBoolean("lavaDamage"), "1.1.2 had no lava damage without the key");
        assertFalse(saved.isSet("noHunger"));
        assertFalse(plugin.services().config().lavaDamage());
        assertTrue(plugin.services().config().lobbyProtection());
    }

    @Test
    void aConfigWithAYamlErrorIsLeftAloneAndTheDefaultsAreUsed() throws IOException {
        // First-enable style: the plugin must not have saved config.yml yet when the broken file is planted.
        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = (PvpPlugin) server.getPluginManager().loadPlugin(PvpPlugin.class);
        List<LogRecord> logs = new ArrayList<>();
        plugin.getLogger().addHandler(recording(logs));
        Path file = data().resolve("config.yml");
        Files.writeString(file, "lobby: [unclosed\nlavaDamage: true\n");
        byte[] before = Files.readAllBytes(file);

        server.getPluginManager().enablePlugin(plugin);

        assertTrue(plugin.isEnabled());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(PvpConfig.defaults(), plugin.services().config());
        assertTrue(logs.stream().anyMatch(record -> record.getLevel() == Level.SEVERE
                && record.getMessage().contains("config.yml has a YAML error")), "expected a SEVERE log line: " + logs);
    }

    @Test
    void aMissingLobbyWorldIsWarnedAboutAtEnable() throws IOException {
        // First-enable style: the plugin must not have saved config.yml yet when the file naming it is planted.
        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = (PvpPlugin) server.getPluginManager().loadPlugin(PvpPlugin.class);
        List<LogRecord> logs = new ArrayList<>();
        plugin.getLogger().addHandler(recording(logs));
        Path file = data().resolve("config.yml");
        Files.writeString(file, "configVersion: 1\nlobby:\n  world: missingworld\n");

        server.getPluginManager().enablePlugin(plugin);

        assertTrue(logs.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("missingworld")
                && record.getMessage().contains("add it to worlds in config.yml")),
                "expected a WARNING log line: " + logs);
    }

    @Test
    void aLoadedLobbyWorldLogsNoWarningAtEnable() {
        // First-enable style, like aMissingLobbyWorldIsWarnedAboutAtEnable, but the shipped default already names
        // a world that is loaded.
        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = (PvpPlugin) server.getPluginManager().loadPlugin(PvpPlugin.class);
        List<LogRecord> logs = new ArrayList<>();
        plugin.getLogger().addHandler(recording(logs));

        server.getPluginManager().enablePlugin(plugin);

        assertTrue(logs.stream().noneMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("isn't loaded")), "expected no lobby-world WARNING: " + logs);
    }

    @Test
    void aMessagesFileWithAYamlErrorFallsBackToTheBundledTexts() throws IOException {
        MockBukkit.unmock();
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = (PvpPlugin) server.getPluginManager().loadPlugin(PvpPlugin.class);
        List<LogRecord> logs = new ArrayList<>();
        plugin.getLogger().addHandler(recording(logs));
        Path file = data().resolve("messages.yml");
        Files.writeString(file, "error:\n  no-permission: \"<danger>unterminated\n bad: [x\n");
        byte[] before = Files.readAllBytes(file);

        server.getPluginManager().enablePlugin(plugin);

        assertEquals("You don't have permission to do that.", plugin.services().messages().plain(Msg.ERROR_NO_PERMISSION));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertTrue(logs.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("messages.yml could not be loaded")), "expected a WARNING log line: " + logs);
    }

    @Test
    void messagesYmlIsSavedToTheDataFolder() {
        assertTrue(Files.exists(data().resolve("messages.yml")));
        assertEquals("You don't have permission to do that.", plugin.services().messages().plain(Msg.ERROR_NO_PERMISSION));
    }

    @Test
    void arenasImportALegacyMapDroppedIntoMapsOnTheNextLoad() throws IOException {
        TestFiles.copyResource("/fixtures/maps/AS-TDM", data().resolve("maps/AS-TDM"));

        LoadReport report = plugin.services().arenas().load();

        assertEquals(List.of("AS-TDM"), report.imported());
        Arena arena = plugin.services().arenas().get("AS-TDM").orElseThrow();
        assertEquals("tdm", arena.modeId());
        assertTrue(Files.exists(data().resolve("arenas/AS-TDM.json")));
    }

    @Test
    void aNewPlayerHasEmptyStats() {
        UUID id = UUID.randomUUID();

        assertEquals(PlayerStats.empty(id, "Alice"), plugin.services().stats().get(id, "Alice"));
    }

    @Test
    void disablingClosesTheWriterCleanly() {
        UUID id = UUID.randomUUID();

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());
        // A closed writer saves on the caller's thread, so the file is there as soon as the call returns.
        plugin.services().stats().recordSpectated(id, "Alice");
        assertTrue(Files.exists(data().resolve("players").resolve(id + ".json")));
    }

    private static Handler recording(List<LogRecord> into) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                into.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }

    private Path data() {
        return plugin.getDataFolder().toPath();
    }
}
