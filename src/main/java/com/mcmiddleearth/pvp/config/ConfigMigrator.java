package com.mcmiddleearth.pvp.config;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Brings a 1.1.2/2.0.0 config.yml to the 3.0 layout. 1.1.2 wrote {@code worlds}, {@code noHunger},
 * {@code PVP.*} and {@code lavaDamage}; only {@code worlds} and {@code lavaDamage} ever had an effect, so they
 * are the only values carried over. Everything new gets the value of {@link PvpConfig#defaults()}, which keeps
 * 1.1.2's protected lobby ({@code lobbyProtection: true}).
 */
public final class ConfigMigrator {

    public static final int CURRENT_VERSION = 1;

    /** Every key the migration writes or drops; they are removed first so the file reads in the shipped order. */
    private static final List<String> KEYS = List.of("configVersion", "lobby", "worlds", "mainServer", "startLocked",
            "lavaDamage", "lobbyProtection", "countdownSeconds", "endingSeconds", "modes", "noHunger", "PVP");

    private ConfigMigrator() {
    }

    /** Brings a 1.1.2/2.0.0 config to version 1 in place. Returns true when it changed anything (then save it). */
    public static boolean migrate(ConfigurationSection config) {
        if (config.getInt("configVersion", 0) >= CURRENT_VERSION) {
            return false;
        }
        // isSet ignores attached defaults, so both reads see only what the file itself holds.
        // 1.1.2 read getBoolean("lavaDamage", false): a file without the key had no lava damage.
        boolean lavaDamage = config.isSet("lavaDamage") && config.getBoolean("lavaDamage");
        List<String> worlds = config.isList("worlds") ? config.getStringList("worlds") : List.of("world");

        KEYS.forEach(key -> config.set(key, null));

        PvpConfig defaults = PvpConfig.defaults();
        NamedPoint lobby = defaults.lobby();
        config.set("configVersion", CURRENT_VERSION);
        config.set("lobby.world", lobby.world());
        config.set("lobby.x", lobby.x());
        config.set("lobby.y", lobby.y());
        config.set("lobby.z", lobby.z());
        // Floats, not doubles: the YAML then reads 0.3 instead of 0.30000001192092896.
        config.set("lobby.yaw", lobby.yaw());
        config.set("lobby.pitch", lobby.pitch());
        config.set("worlds", worlds);
        config.set("mainServer", defaults.mainServer());
        config.set("startLocked", defaults.startLocked());
        config.set("lavaDamage", lavaDamage);
        config.set("lobbyProtection", defaults.lobbyProtection());
        config.set("countdownSeconds", defaults.countdownSeconds());
        config.set("endingSeconds", defaults.endingSeconds());
        // Copied from the shipped file rather than defaults.modeDefaults() (a Map.of(), with no defined order) so
        // modes and their settings are written in the shipped order; PvpConfigTest pins the two as equal.
        ConfigurationSection modes = shippedConfig().getConfigurationSection("modes");
        for (String mode : modes.getKeys(false)) {
            ConfigurationSection settings = modes.getConfigurationSection(mode);
            for (String setting : settings.getKeys(false)) {
                config.set("modes." + mode + "." + setting, settings.get(setting));
            }
        }
        return true;
    }

    /**
     * The YAML error in {@code file}, if it has one; a missing file has none. Bukkit's own {@code getConfig()} reads
     * a file with a YAML error as empty and only logs it, so migrating and saving that would replace every setting
     * with the defaults; the plugin checks this first and leaves such a file alone.
     */
    public static Optional<String> yamlError(Path file) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        try {
            new YamlConfiguration().load(file.toFile());
            return Optional.empty();
        } catch (InvalidConfigurationException | IOException e) {
            String message = e.getMessage();
            return Optional.of(message == null ? e.getClass().getSimpleName() : message);
        }
    }

    /** The config.yml shipped in the plugin jar, read fresh so its key order is preserved. */
    private static YamlConfiguration shippedConfig() {
        try (InputStream in = ConfigMigrator.class.getResourceAsStream("/config.yml")) {
            if (in == null) {
                throw new IllegalStateException("config.yml is missing from the plugin jar");
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("config.yml could not be read from the plugin jar", e);
        }
    }
}
