package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsTest {

    private static final List<SettingSpec> SPECS = List.of(
            new SettingSpec(Setting.TEAM_COUNT, 2, 4, 1, 2),
            new SettingSpec(Setting.MAX_PLAYERS, 2, 16, 1, 16),
            new SettingSpec(Setting.MIDGAME_JOIN, 0, 0, 0, true));

    private static GameSettings resolve(Map<String, ?> input) {
        return GameSettings.resolve(SPECS, input, "tdm");
    }

    private static PvpException refused(Map<String, ?> input) {
        return assertThrows(PvpException.class, () -> resolve(input));
    }

    @Test
    void missingKeysTakeTheDefaults() {
        GameSettings settings = resolve(Map.of());

        assertEquals(2, settings.get(Setting.TEAM_COUNT));
        assertEquals(16, settings.get(Setting.MAX_PLAYERS));
        assertTrue(settings.get(Setting.MIDGAME_JOIN));
        assertEquals(GameSettings.defaults(SPECS).asMap(), settings.asMap());
    }

    @Test
    void keysMatchSettingIdsIgnoringCase() {
        GameSettings settings = resolve(Map.of("TEAMCOUNT", 3, "maxplayers", "8"));

        assertEquals(3, settings.get(Setting.TEAM_COUNT));
        assertEquals(8, settings.get(Setting.MAX_PLAYERS));
    }

    @Test
    void anUnknownKeyIsRefusedWithTheModeAndItsSettings() {
        PvpException e = refused(Map.of("timeLimit", 5));

        assertEquals(Msg.ERROR_SETTING_UNKNOWN, e.messageKey());
        assertEquals(Map.of("setting", "timeLimit", "mode", "tdm", "settings", "teamCount, maxPlayers, midgameJoin"),
                e.args());
    }

    @Test
    void wholeNumbersAcceptNumbersAndIntegerText() {
        assertEquals(4, resolve(Map.of("teamCount", 3.6)).get(Setting.TEAM_COUNT));
        assertEquals(2, resolve(Map.of("teamCount", 2.4f)).get(Setting.TEAM_COUNT));
        assertEquals(3, resolve(Map.of("teamCount", 3L)).get(Setting.TEAM_COUNT));
        assertEquals(3, resolve(Map.of("teamCount", " 3 ")).get(Setting.TEAM_COUNT));
    }

    @Test
    void aWholeNumberOfTheWrongKindIsInvalid() {
        PvpException text = refused(Map.of("teamCount", "three"));
        assertEquals(Msg.ERROR_SETTING_INVALID, text.messageKey());
        assertEquals(Map.of("setting", "teamCount", "value", "three"), text.args());

        assertEquals(Msg.ERROR_SETTING_INVALID, refused(Map.of("teamCount", "2.5")).messageKey());
        assertEquals(Msg.ERROR_SETTING_INVALID, refused(Map.of("teamCount", true)).messageKey());
        assertEquals(Msg.ERROR_SETTING_INVALID, refused(Map.of("teamCount", Double.NaN)).messageKey());
    }

    @Test
    void aWholeNumberOutsideTheRangeIsRefused() {
        PvpException high = refused(Map.of("teamCount", 5));
        assertEquals(Msg.ERROR_SETTING_RANGE, high.messageKey());
        assertEquals(Map.of("setting", "teamCount", "min", "2", "max", "4"), high.args());

        assertEquals(Msg.ERROR_SETTING_RANGE, refused(Map.of("teamCount", "1")).messageKey());
        assertEquals(Msg.ERROR_SETTING_RANGE, refused(Map.of("teamCount", 1.4)).messageKey());
    }

    @Test
    void switchesAcceptBooleansAndWords() {
        assertFalse(resolve(Map.of("midgameJoin", false)).get(Setting.MIDGAME_JOIN));
        assertTrue(resolve(Map.of("midgameJoin", "yes")).get(Setting.MIDGAME_JOIN));
        assertFalse(resolve(Map.of("midgameJoin", "OFF")).get(Setting.MIDGAME_JOIN));
        assertTrue(resolve(Map.of("midgameJoin", " On ")).get(Setting.MIDGAME_JOIN));
        assertFalse(resolve(Map.of("midgameJoin", "no")).get(Setting.MIDGAME_JOIN));
        assertTrue(resolve(Map.of("midgameJoin", "TRUE")).get(Setting.MIDGAME_JOIN));
    }

    @Test
    void anyOtherSwitchValueIsInvalid() {
        PvpException word = refused(Map.of("midgameJoin", "maybe"));
        assertEquals(Msg.ERROR_SETTING_INVALID, word.messageKey());
        assertEquals(Map.of("setting", "midgameJoin", "value", "maybe"), word.args());

        assertEquals(Msg.ERROR_SETTING_INVALID, refused(Map.of("midgameJoin", 1)).messageKey());
    }

    @Test
    void aSettingTheGameDoesNotHaveCannotBeRead() {
        GameSettings settings = GameSettings.defaults(SPECS);

        assertTrue(settings.has(Setting.TEAM_COUNT));
        assertFalse(settings.has(Setting.TIME_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> settings.get(Setting.TIME_LIMIT));
    }

    @Test
    void aPlusSignAnExponentAndInfinityGetTheirOwnRefusals() {
        // teamCount's max here is 4: below 5, so a leading "+" that failed to parse would show up as INVALID
        // instead of RANGE.
        PvpException plus = refused(Map.of("teamCount", "+5"));
        assertEquals(Msg.ERROR_SETTING_RANGE, plus.messageKey());

        PvpException exponent = refused(Map.of("teamCount", "1e3"));
        assertEquals(Msg.ERROR_SETTING_INVALID, exponent.messageKey());

        PvpException infinity = refused(Map.of("teamCount", Double.POSITIVE_INFINITY));
        assertEquals(Msg.ERROR_SETTING_RANGE, infinity.messageKey());
    }

    @Test
    void asMapIsSortedByIdAndReadOnly() {
        Map<String, Object> map = resolve(Map.of("teamCount", 3)).asMap();

        assertEquals(List.of("maxPlayers", "midgameJoin", "teamCount"), List.copyOf(map.keySet()));
        assertEquals(16, map.get("maxPlayers"));
        assertEquals(true, map.get("midgameJoin"));
        assertEquals(3, map.get("teamCount"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("scoreGoal", 1));
    }
}
