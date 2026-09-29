package com.mcmiddleearth.pvp.mode;

import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModeRegistryTest {

    private record Named(String id, Set<String> legacyNames) implements GameMode {
        @Override
        public ModeRules rules() {
            return TestMode.twoTeams().build();
        }
    }

    private static final GameMode TDM = new Named("tdm", Set.of("TeamDeathmatch"));
    private static final GameMode FFA = new Named("ffa", Set.of("FreeForAll"));

    private final ModeRegistry modes = new ModeRegistry(List.of(TDM, FFA));

    @Test
    void modesAreFoundByIdIgnoringCase() {
        assertEquals(Optional.of(TDM), modes.get("TDM"));
        assertEquals(Optional.of(FFA), modes.get("ffa"));
        assertEquals(Optional.empty(), modes.get("ctf"));
    }

    @Test
    void allKeepsTheRegistrationOrder() {
        assertEquals(List.of(TDM, FFA), modes.all());
    }

    @Test
    void twoModesCannotShareAnId() {
        assertThrows(IllegalArgumentException.class, () -> new ModeRegistry(List.of(TDM, new Named("TDM", Set.of()))));
    }

    @Test
    void legacyNamesMatchWithoutSpacesAndIgnoringCase() {
        assertEquals(Optional.of(TDM), modes.byLegacyName("Team Deathmatch"));
        assertEquals(Optional.of(TDM), modes.byLegacyName(" TEAM  deathmatch "));
        assertEquals(Optional.of(FFA), modes.byLegacyName("Free For All"));
        assertEquals(Optional.empty(), modes.byLegacyName("Capture The Flag"));
        assertEquals(Optional.empty(), modes.byLegacyName(""));
        assertEquals(Optional.empty(), modes.byLegacyName(null));
    }

    @Test
    void normalizeRemovesWhitespaceAndLowercases() {
        assertEquals("freeforall", ModeRegistry.normalize("Free For\tAll"));
        assertEquals("", ModeRegistry.normalize(null));
    }
}
