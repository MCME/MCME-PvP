package com.mcmiddleearth.pvp.config;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The values of config.yml the plugin uses, read once at startup. */
public record PvpConfig(
        NamedPoint lobby,
        List<String> worlds,
        String mainServer,
        boolean startLocked,
        boolean lavaDamage,
        boolean lobbyProtection,
        int countdownSeconds,
        int endingSeconds,
        Map<String, Map<String, Object>> modeDefaults) {

    public PvpConfig {
        worlds = List.copyOf(worlds);
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        modeDefaults.forEach((mode, values) -> copy.put(mode, Map.copyOf(values)));
        modeDefaults = Map.copyOf(copy);
    }

    /** The defaults that ship in config.yml: 1.1.2's lobby, lobby protection, a 7-second countdown and the mode defaults. */
    public static PvpConfig defaults() {
        return new PvpConfig(
                new NamedPoint("world", 344.47, 39, 521.58, 0.3f, -24.15f),
                List.of("world"),
                "world",
                true,   // startLocked
                true,   // lavaDamage
                true,   // lobbyProtection
                7,      // countdownSeconds
                10,     // endingSeconds
                Map.of(
                        "tdm", Map.of("teamCount", 2, "midgameJoin", true),
                        "ffa", Map.of("timeLimit", 10, "scoreGoal", 0, "midgameJoin", true)));
    }

    /** Reads a migrated config (see {@link ConfigMigrator}); missing keys take the value from {@link #defaults()}. */
    public static PvpConfig from(ConfigurationSection root) {
        PvpConfig d = defaults();
        ConfigurationSection l = root.getConfigurationSection("lobby");
        NamedPoint lobby = l == null ? d.lobby() : new NamedPoint(
                l.getString("world", d.lobby().world()),
                l.getDouble("x", d.lobby().x()),
                l.getDouble("y", d.lobby().y()),
                l.getDouble("z", d.lobby().z()),
                (float) l.getDouble("yaw", d.lobby().yaw()),
                (float) l.getDouble("pitch", d.lobby().pitch()));
        Map<String, Map<String, Object>> modes = new LinkedHashMap<>(d.modeDefaults());
        ConfigurationSection m = root.getConfigurationSection("modes");
        if (m != null) {
            for (String modeId : m.getKeys(false)) {
                ConfigurationSection section = m.getConfigurationSection(modeId);
                if (section != null) {
                    modes.put(modeId, new LinkedHashMap<>(section.getValues(false)));
                }
            }
        }
        return new PvpConfig(
                lobby,
                root.isList("worlds") ? root.getStringList("worlds") : d.worlds(),
                root.getString("mainServer", d.mainServer()),
                root.getBoolean("startLocked", d.startLocked()),
                root.getBoolean("lavaDamage", d.lavaDamage()),
                root.getBoolean("lobbyProtection", d.lobbyProtection()),
                Math.max(1, root.getInt("countdownSeconds", d.countdownSeconds())),
                Math.max(0, root.getInt("endingSeconds", d.endingSeconds())),
                modes);
    }

    /** The configured default of one setting of one mode, if any. */
    public Optional<Object> modeDefault(String modeId, String settingId) {
        return Optional.ofNullable(modeDefaults.getOrDefault(modeId, Map.of()).get(settingId));
    }
}
