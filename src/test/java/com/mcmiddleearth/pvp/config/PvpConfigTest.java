package com.mcmiddleearth.pvp.config;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpConfigTest {

    @Test
    void theShippedConfigHoldsTheDefaults() throws IOException {
        assertEquals(PvpConfig.defaults(), PvpConfig.from(yaml(resource("/config.yml"))));
    }

    @Test
    void anEmptyFileGivesTheDefaults() {
        assertEquals(PvpConfig.defaults(), PvpConfig.from(yaml("")));
    }

    @Test
    void theLobbyIsProtectedByDefaultAsIn112() {
        assertTrue(PvpConfig.defaults().lobbyProtection());
        assertTrue(PvpConfig.from(yaml("")).lobbyProtection());
    }

    @Test
    void customValuesAreRead() {
        PvpConfig config = PvpConfig.from(yaml("""
                lobby:
                  world: hub
                  x: 1.5
                  y: 70
                  z: -3.25
                  yaw: 90
                  pitch: 10.5
                worlds: [world, arenas]
                mainServer: lobby
                startLocked: false
                lavaDamage: false
                lobbyProtection: false
                countdownSeconds: 5
                endingSeconds: 3
                modes:
                  tdm:
                    teamCount: 3
                """));

        assertEquals(new NamedPoint("hub", 1.5, 70, -3.25, 90f, 10.5f), config.lobby());
        assertEquals(List.of("world", "arenas"), config.worlds());
        assertEquals("lobby", config.mainServer());
        assertFalse(config.startLocked());
        assertFalse(config.lavaDamage());
        assertFalse(config.lobbyProtection());
        assertEquals(5, config.countdownSeconds());
        assertEquals(3, config.endingSeconds());
        assertEquals(Optional.of(3), config.modeDefault("tdm", "teamCount"));
    }

    @Test
    void aPartialLobbyTakesTheRestFromTheDefaults() {
        NamedPoint d = PvpConfig.defaults().lobby();

        PvpConfig config = PvpConfig.from(yaml("lobby:\n  x: 1.0\n"));

        assertEquals(new NamedPoint(d.world(), 1.0, d.y(), d.z(), d.yaw(), d.pitch()), config.lobby());
    }

    @Test
    void aModeSectionReplacesTheDefaultsOfThatModeOnly() {
        PvpConfig config = PvpConfig.from(yaml("modes:\n  tdm:\n    teamCount: 4\n"));

        assertEquals(Map.of("teamCount", 4), config.modeDefaults().get("tdm"));
        assertEquals(PvpConfig.defaults().modeDefaults().get("ffa"), config.modeDefaults().get("ffa"));
    }

    @Test
    void theCountdownLastsAtLeastOneSecondAndTheEndingIsNeverNegative() {
        PvpConfig config = PvpConfig.from(yaml("countdownSeconds: 0\nendingSeconds: -5\n"));

        assertEquals(1, config.countdownSeconds());
        assertEquals(0, config.endingSeconds());
    }

    @Test
    void modeDefaultLooksUpOneSettingOfOneMode() {
        PvpConfig defaults = PvpConfig.defaults();

        assertEquals(Optional.of(10), defaults.modeDefault("ffa", "timeLimit"));
        assertEquals(Optional.of(true), defaults.modeDefault("tdm", "midgameJoin"));
        assertEquals(Optional.empty(), defaults.modeDefault("tdm", "timeLimit"));
        assertEquals(Optional.empty(), defaults.modeDefault("ctf", "teamCount"));
    }

    @Test
    void theValuesAreImmutableCopies() {
        List<String> worlds = new ArrayList<>(List.of("world"));
        Map<String, Object> tdm = new HashMap<>(Map.of("teamCount", 2));
        PvpConfig config = new PvpConfig(NamedPoint.of("world", 0, 64, 0), worlds, "world", true, true, true, 7, 10,
                Map.of("tdm", tdm));

        worlds.add("later");
        tdm.put("later", 1);

        assertEquals(List.of("world"), config.worlds());
        assertEquals(Map.of("teamCount", 2), config.modeDefaults().get("tdm"));
        assertThrows(UnsupportedOperationException.class, () -> config.worlds().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> config.modeDefaults().get("tdm").put("x", 1));
    }

    static YamlConfiguration yaml(String text) {
        return YamlConfiguration.loadConfiguration(new StringReader(text));
    }

    static String resource(String name) throws IOException {
        try (InputStream in = PvpConfigTest.class.getResourceAsStream(name)) {
            assertNotNull(in, name + " is not on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
