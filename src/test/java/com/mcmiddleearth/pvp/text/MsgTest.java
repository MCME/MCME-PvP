package com.mcmiddleearth.pvp.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Msg and the bundled messages.yml cannot drift apart. */
class MsgTest {

    private final Messages bundled = Messages.bundled();

    @Test
    void everyConstantExistsInTheBundledFile() {
        List<String> keys = constants();

        assertFalse(keys.isEmpty());
        for (String key : keys) {
            assertTrue(bundled.has(key), key + " is missing from messages.yml");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"tdm", "ffa"})
    void everyModeHasANameAndRules(String modeId) {
        assertTrue(bundled.has(Msg.modeName(modeId)), Msg.modeName(modeId));
        assertTrue(bundled.has(Msg.modeRules(modeId)), Msg.modeRules(modeId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"teamCount", "timeLimit", "scoreGoal", "maxPlayers", "midgameJoin"})
    void everySettingHasALabel(String settingId) {
        assertTrue(bundled.has(Msg.setting(settingId)), Msg.setting(settingId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "COUNTDOWN", "RUNNING", "ENDING"})
    void everyShownPhaseHasAStatusWord(String phase) {
        assertTrue(bundled.has(Msg.phase(phase)), Msg.phase(phase));
    }

    @Test
    void helpersBuildTheKeys() {
        assertEquals("mode.tdm.name", Msg.modeName("tdm"));
        assertEquals("mode.ffa.rules", Msg.modeRules("ffa"));
        assertEquals("setting.teamCount", Msg.setting("teamCount"));
        assertEquals("phase.waiting", Msg.phase("WAITING"));
    }

    @Test
    void everyBundledKeyHasAConstantOrAHelper() {
        Set<String> constants = Set.copyOf(constants());

        for (String key : bundled.keys()) {
            boolean helperKey = key.matches("mode\\.[a-z0-9_-]+\\.(name|rules)")
                    || key.startsWith("setting.") || key.startsWith("phase.");
            assertTrue(constants.contains(key) || helperKey, key + " has no constant in Msg");
        }
    }

    /** The values of every public static String constant of {@link Msg}. */
    private static List<String> constants() {
        List<String> keys = new ArrayList<>();
        for (Field field : Msg.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    keys.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        return keys;
    }
}
