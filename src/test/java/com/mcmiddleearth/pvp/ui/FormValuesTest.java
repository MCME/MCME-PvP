package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormValuesTest {

    @Test
    void presentValuesAreReturned() {
        FormValues values = new FormValues(Map.of("teamCount", 3, "midgameJoin", true));
        assertEquals(3, values.require("teamCount"));
        assertEquals(Optional.of(true), values.get("midgameJoin"));
        assertEquals(Optional.empty(), values.get("timeLimit"));
    }

    @Test
    void aMissingValueIsRefusedAsInvalid() {
        FormValues values = FormValues.empty();
        PvpException refusal = assertThrows(PvpException.class, () -> values.require("maxPlayers"));
        assertEquals(Msg.ERROR_SETTING_INVALID, refusal.messageKey());
        assertEquals(Map.of("setting", "maxPlayers", "value", "missing"), refusal.args());
    }

    @Test
    void valuesKeepTheirOrderAndCannotBeChanged() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("timeLimit", 10);
        input.put("scoreGoal", 0);
        input.put("maxPlayers", 16);
        FormValues values = new FormValues(input);
        input.put("midgameJoin", true);
        assertEquals(List.of("timeLimit", "scoreGoal", "maxPlayers"), List.copyOf(values.values().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> values.values().put("x", 1));
    }
}
