package com.mcmiddleearth.pvp;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpExceptionTest {

    @Test
    void keepsTheMessageKeyAndItsArguments() {
        PvpException e = new PvpException("error.game-full", Map.of("max", "8"));

        assertEquals("error.game-full", e.messageKey());
        assertEquals(Map.of("max", "8"), e.args());
        assertTrue(e.getMessage().startsWith("error.game-full"), e.getMessage());
    }

    @Test
    void aKeyAloneHasNoArguments() {
        assertEquals(Map.of(), new PvpException("error.game-ended").args());
    }

    @Test
    void argumentsAreAnImmutableCopy() {
        Map<String, String> args = new HashMap<>();
        args.put("arena", "AS-TDM");
        PvpException e = new PvpException("error.unknown-arena", args);
        args.put("later", "change");

        assertEquals(Map.of("arena", "AS-TDM"), e.args());
        assertThrows(UnsupportedOperationException.class, () -> e.args().put("x", "y"));
    }
}
