package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsArgumentTest {

    private static final List<SettingSpec> FFA = List.of(
            new SettingSpec(Setting.TIME_LIMIT, 1, 60, 1, 10),
            new SettingSpec(Setting.SCORE_GOAL, 0, 100, 1, 0),
            new SettingSpec(Setting.MAX_PLAYERS, 2, 16, 1, 16),
            new SettingSpec(Setting.MIDGAME_JOIN, 0, 0, 0, true));

    @Test
    void pairsKeepTheirTypedOrder() {
        Map<String, String> settings = SettingsArgument.parse("teamCount=3 timeLimit=12");

        assertEquals(Map.of("teamCount", "3", "timeLimit", "12"), settings);
        assertEquals(List.of("teamCount", "timeLimit"), List.copyOf(settings.keySet()));
    }

    @Test
    void blankTextAndExtraSpacesAreFine() {
        assertEquals(Map.of(), SettingsArgument.parse(""));
        assertEquals(Map.of(), SettingsArgument.parse("   "));
        assertEquals(Map.of("a", "1", "b", "2"), SettingsArgument.parse("  a=1   b=2 "));
    }

    @Test
    void anEmptyNameIsRefused() {
        assertInvalid("=3", "", "3");
        assertInvalid("=", "", "");
    }

    @Test
    void aBareWordIsAskedForItsValue() {
        assertForm("teamCount", "teamCount");
        assertForm("timeLimit=5 maxPlayers", "maxPlayers");
        assertForm("teamCount=", "teamCount");
    }

    @Test
    void aValueMayHoldAnEqualsSign() {
        assertEquals(Map.of("a", "b=c"), SettingsArgument.parse("a=b=c"));
    }

    @Test
    void aSettingGivenTwiceIsRefusedInAnySpelling() {
        assertTwice("timeLimit=5 timeLimit=7", "timeLimit");
        assertTwice("timeLimit=5 TIMELIMIT=7", "TIMELIMIT");
    }

    @Test
    void suggestionsCompleteTheLastWordWithTheDefault() {
        assertEquals(List.of("timeLimit=10"), SettingsArgument.suggestions("ti", FFA));
        assertEquals(List.of("timeLimit=10"), SettingsArgument.suggestions("TI", FFA));
        assertEquals(List.of("timeLimit=10", "scoreGoal=0", "maxPlayers=16", "midgameJoin=true"),
                SettingsArgument.suggestions("", FFA));
    }

    @Test
    void settingsAlreadyTypedAreNotSuggestedAgain() {
        assertEquals(List.of("scoreGoal=0", "maxPlayers=16", "midgameJoin=true"),
                SettingsArgument.suggestions("timeLimit=5 ", FFA));
        assertEquals(List.of(), SettingsArgument.suggestions("timeLimit=5 ti", FFA));
    }

    @Test
    void aSettingGivenInAnyCaseIsNotSuggestedAgain() {
        assertEquals(List.of("scoreGoal=0", "maxPlayers=16", "midgameJoin=true"),
                SettingsArgument.suggestions("TIMELIMIT=5 ", FFA));
    }

    @Test
    void onlyTheLastWordIsCompleted() {
        assertEquals(List.of("maxPlayers=16", "midgameJoin=true"), SettingsArgument.suggestions("timeLimit=5 m", FFA));
    }

    private static void assertInvalid(String text, String setting, String value) {
        PvpException refusal = assertThrows(PvpException.class, () -> SettingsArgument.parse(text));
        assertEquals(Msg.ERROR_SETTING_INVALID, refusal.messageKey());
        assertEquals(Map.of("setting", setting, "value", value), refusal.args());
    }

    private static void assertForm(String text, String word) {
        PvpException refusal = assertThrows(PvpException.class, () -> SettingsArgument.parse(text));
        assertEquals(Msg.ERROR_SETTING_FORM, refusal.messageKey());
        assertEquals(Map.of("word", word), refusal.args());
    }

    private static void assertTwice(String text, String setting) {
        PvpException refusal = assertThrows(PvpException.class, () -> SettingsArgument.parse(text));
        assertEquals(Msg.ERROR_SETTING_TWICE, refusal.messageKey());
        assertEquals(Map.of("setting", setting), refusal.args());
    }
}
