package com.mcmiddleearth.pvp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.mcmiddleearth.pvp.config.PvpConfigTest.resource;
import static com.mcmiddleearth.pvp.config.PvpConfigTest.yaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {

    /** The config.yml of 1.1.2 and 2.0.0. */
    private static final String CONFIG_112 = """
            worlds:
            - world
            noHunger:
            - world
            PVP:
                Enabled: true
                Broadcast_minutes: 2
            lavaDamage: true
            """;

    /** A 1.1.2 config written before lavaDamage existed (2024-06-10), with a second world. */
    private static final String CONFIG_112_WITHOUT_LAVA_DAMAGE = """
            worlds:
            - world
            - pvp_arenas
            noHunger:
            - world
            PVP:
                Enabled: true
                Broadcast_minutes: 2
            """;

    @Test
    void migratesA112ConfigToVersion1() {
        YamlConfiguration config = yaml(CONFIG_112);

        assertTrue(ConfigMigrator.migrate(config));

        assertEquals(1, config.getInt("configVersion"));
        assertTrue(config.getBoolean("lavaDamage"));
        assertEquals(List.of("world"), config.getStringList("worlds"));
        assertFalse(config.isSet("noHunger"), "noHunger never had an effect");
        assertFalse(config.isSet("PVP"), "PVP.* never had an effect");
        assertEquals(PvpConfig.defaults(), PvpConfig.from(config));
    }

    @Test
    void aMissingLavaDamageKeyMeansNoLavaDamage() {
        // 1.1.2 read getBoolean("lavaDamage", false).
        YamlConfiguration config = yaml(CONFIG_112_WITHOUT_LAVA_DAMAGE);

        assertTrue(ConfigMigrator.migrate(config));

        assertTrue(config.isSet("lavaDamage"));
        assertFalse(config.getBoolean("lavaDamage"));
    }

    @Test
    void aLegacyConfigKeepsTheProtectedLobbyOf112() {
        YamlConfiguration config = yaml(CONFIG_112_WITHOUT_LAVA_DAMAGE);

        ConfigMigrator.migrate(config);

        assertTrue(config.isSet("lobbyProtection"));
        assertTrue(config.getBoolean("lobbyProtection"));
        assertTrue(PvpConfig.from(config).lobbyProtection());
    }

    @Test
    void lavaDamageFalseStaysFalse() {
        YamlConfiguration config = yaml("lavaDamage: false\n");

        ConfigMigrator.migrate(config);

        assertFalse(config.getBoolean("lavaDamage"));
    }

    @Test
    void configuredWorldsAreKept() {
        YamlConfiguration config = yaml(CONFIG_112_WITHOUT_LAVA_DAMAGE);

        ConfigMigrator.migrate(config);

        assertEquals(List.of("world", "pvp_arenas"), config.getStringList("worlds"));
    }

    @Test
    void missingWorldsBecomeTheMainWorld() {
        YamlConfiguration config = yaml("lavaDamage: true\n");

        ConfigMigrator.migrate(config);

        assertEquals(List.of("world"), config.getStringList("worlds"));
    }

    @Test
    void aNonListWorldsValueBecomesTheMainWorld() {
        // worlds: world is a scalar, not a list: getStringList would silently return [], loading no worlds at all.
        YamlConfiguration config = yaml("worlds: world\n");

        ConfigMigrator.migrate(config);

        assertEquals(List.of("world"), config.getStringList("worlds"));
    }

    @Test
    void theNewKeysGetTheShippedDefaults() {
        YamlConfiguration config = yaml(CONFIG_112);

        ConfigMigrator.migrate(config);

        assertEquals("world", config.getString("lobby.world"));
        assertEquals(344.47, config.getDouble("lobby.x"));
        assertEquals(39.0, config.getDouble("lobby.y"));
        assertEquals(521.58, config.getDouble("lobby.z"));
        assertEquals(0.3f, (float) config.getDouble("lobby.yaw"));
        assertEquals(-24.15f, (float) config.getDouble("lobby.pitch"));
        assertEquals("world", config.getString("mainServer"));
        assertTrue(config.getBoolean("startLocked"));
        assertTrue(config.getBoolean("lobbyProtection"));
        assertEquals(7, config.getInt("countdownSeconds"));
        assertEquals(10, config.getInt("endingSeconds"));
        assertEquals(2, config.getInt("modes.tdm.teamCount"));
        assertTrue(config.getBoolean("modes.tdm.midgameJoin"));
        assertEquals(10, config.getInt("modes.ffa.timeLimit"));
        assertEquals(0, config.getInt("modes.ffa.scoreGoal"));
        assertTrue(config.getBoolean("modes.ffa.midgameJoin"));
    }

    @Test
    void theModesSectionKeepsTheShippedOrder() throws IOException {
        YamlConfiguration config = yaml(CONFIG_112);
        ConfigMigrator.migrate(config);
        YamlConfiguration shipped = yaml(resource("/config.yml"));

        assertEquals(List.copyOf(shipped.getConfigurationSection("modes").getKeys(false)),
                List.copyOf(config.getConfigurationSection("modes").getKeys(false)));
        for (String mode : shipped.getConfigurationSection("modes").getKeys(false)) {
            assertEquals(
                    List.copyOf(shipped.getConfigurationSection("modes").getConfigurationSection(mode).getKeys(false)),
                    List.copyOf(config.getConfigurationSection("modes").getConfigurationSection(mode).getKeys(false)),
                    mode);
        }
    }

    @Test
    void migratingAgainChangesNothing() {
        YamlConfiguration config = yaml(CONFIG_112);
        assertTrue(ConfigMigrator.migrate(config));
        String once = config.saveToString();

        assertFalse(ConfigMigrator.migrate(config));

        assertEquals(once, config.saveToString());
    }

    @Test
    void theShippedConfigIsAlreadyCurrent() throws IOException {
        YamlConfiguration config = yaml(resource("/config.yml"));
        String before = config.saveToString();

        assertFalse(ConfigMigrator.migrate(config));

        assertEquals(before, config.saveToString());
    }

    @Test
    void theMigratedFileReadsBackAfterSaving() {
        YamlConfiguration config = yaml(CONFIG_112);
        ConfigMigrator.migrate(config);
        String saved = config.saveToString();

        assertTrue(saved.startsWith("configVersion: 1"), saved);
        assertTrue(saved.contains("yaw: 0.3"), "a float is saved without rounding noise: " + saved);
        assertEquals(PvpConfig.defaults(), PvpConfig.from(yaml(saved)));
    }

    @Test
    void yamlErrorIsEmptyForAValidFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, CONFIG_112);

        assertEquals(Optional.empty(), ConfigMigrator.yamlError(file));
    }

    @Test
    void yamlErrorIsEmptyForAMissingFile(@TempDir Path dir) {
        Path file = dir.resolve("config.yml");

        assertEquals(Optional.empty(), ConfigMigrator.yamlError(file));
    }

    @Test
    void yamlErrorMentionsTheLineOfABrokenFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "lobby: [unclosed\nlavaDamage: true\n");

        Optional<String> error = ConfigMigrator.yamlError(file);

        assertTrue(error.isPresent());
        assertTrue(error.get().contains("line"), error.get());
    }
}
