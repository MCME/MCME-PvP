package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.game.PlayerRef;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiActionTest {

    private static final UUID ALICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000a11c");

    private final List<String> ran = new ArrayList<>();

    private Viewer alice(String... permissions) {
        List<String> granted = List.of(permissions);
        return new Viewer(ALICE_ID, "Alice", granted::contains);
    }

    @Test
    void aViewerAnswersPermissionsAndNamesItsPlayer() {
        Viewer viewer = alice(Permissions.PLAY);
        assertTrue(viewer.has(Permissions.PLAY));
        assertFalse(viewer.has(Permissions.STAFF_RUN));
        assertEquals(new PlayerRef(ALICE_ID, "Alice"), viewer.ref());
    }

    @Test
    void anAllowedActionRunsItsHandlerWithTheValues() {
        UiAction action = UiAction.of((viewer, values) -> ran.add(viewer.name() + " " + values.values()))
                .requires(Permissions.PLAY);
        Optional<PvpException> refusal = action.attempt(alice(Permissions.PLAY), new FormValues(Map.of("teamCount", 3)));
        assertTrue(refusal.isEmpty());
        assertEquals(List.of("Alice {teamCount=3}"), ran);
    }

    @Test
    void theClickerNeedsThePermissionAtClickTime() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran")).requires(Permissions.STAFF_RUN);
        Optional<PvpException> refusal = action.attempt(alice(Permissions.PLAY), FormValues.empty());
        assertEquals(Msg.ERROR_NO_PERMISSION, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }

    @Test
    void anActionThatIsNoLongerValidIsRefusedBeforeItsHandler() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .validWhile(() -> Optional.of(new PvpException(Msg.ERROR_GAME_ENDED)));
        Optional<PvpException> refusal = action.attempt(alice(), FormValues.empty());
        assertEquals(Msg.ERROR_GAME_ENDED, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }

    @Test
    void aRefusalFromTheHandlerIsReturned() {
        UiAction action = UiAction.of((viewer, values) -> {
            throw new PvpException(Msg.ERROR_GAME_FULL, Map.of("max", "2"));
        });
        PvpException refusal = action.attempt(alice(), FormValues.empty()).orElseThrow();
        assertEquals(Msg.ERROR_GAME_FULL, refusal.messageKey());
        assertEquals(Map.of("max", "2"), refusal.args());
    }

    @Test
    void onceMarksTheActionSingleUseAndKeepsItsChecks() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran")).requires(Permissions.STAFF_RUN);
        UiAction destructive = action.once();
        assertFalse(action.singleUse());
        assertTrue(destructive.singleUse());
        assertEquals(Permissions.STAFF_RUN, destructive.permission());
    }

    @Test
    void noneDoesNothingAndIsRecognised() {
        UiAction none = UiAction.none();
        assertTrue(none.isNone());
        assertNull(none.permission());
        assertTrue(none.attempt(alice(), FormValues.empty()).isEmpty());
        assertFalse(UiAction.of((viewer, values) -> ran.add("ran")).isNone());
    }

    @Test
    void withValuesSubmitsTheFixedValuesInstead() {
        UiAction action = UiAction.of((viewer, values) -> ran.add(String.valueOf(values.values())))
                .withValues(new FormValues(Map.of("midgameJoin", true)));
        action.attempt(alice(), new FormValues(Map.of("midgameJoin", false)));
        assertEquals(List.of("{midgameJoin=true}"), ran);
    }

    @Test
    void aFailingPermissionIsCheckedBeforeTheStillValidCheckAndTheCheckNeverRuns() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> {
                    ran.add("checked");
                    return Optional.of(new PvpException(Msg.ERROR_GAME_ENDED));
                });
        Optional<PvpException> refusal = action.attempt(alice(Permissions.PLAY), FormValues.empty());
        assertEquals(Msg.ERROR_NO_PERMISSION, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }

    @Test
    void withValuesKeepsThePermissionTheCheckAndSingleUse() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> Optional.of(new PvpException(Msg.ERROR_GAME_ENDED)))
                .once();
        UiAction withFixed = action.withValues(new FormValues(Map.of("midgameJoin", true)));
        assertEquals(Permissions.STAFF_RUN, withFixed.permission());
        assertTrue(withFixed.singleUse());
        Optional<PvpException> refusal = withFixed.attempt(alice(Permissions.STAFF_RUN), FormValues.empty());
        assertEquals(Msg.ERROR_GAME_ENDED, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }

    @Test
    void onceKeepsTheStillValidCheckNotOnlyThePermission() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .validWhile(() -> Optional.of(new PvpException(Msg.ERROR_GAME_ENDED)))
                .once();
        Optional<PvpException> refusal = action.attempt(alice(), FormValues.empty());
        assertEquals(Msg.ERROR_GAME_ENDED, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }

    @Test
    void aNonPvpExceptionFromTheHandlerPropagatesOutOfAttempt() {
        UiAction action = UiAction.of((viewer, values) -> {
            throw new IllegalStateException("boom");
        });
        assertThrows(IllegalStateException.class, () -> action.attempt(alice(), FormValues.empty()));
    }

    @Test
    void twoValidWhileChecksBothApplyInOrder() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .validWhile(() -> {
                    ran.add("first");
                    return Optional.empty();
                })
                .validWhile(() -> {
                    ran.add("second");
                    return Optional.of(new PvpException(Msg.ERROR_GAME_ENDED));
                });
        Optional<PvpException> refusal = action.attempt(alice(), FormValues.empty());
        assertEquals(Msg.ERROR_GAME_ENDED, refusal.orElseThrow().messageKey());
        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    void aCheckThatThrowsComesBackAsTheRefusal() {
        UiAction action = UiAction.of((viewer, values) -> ran.add("ran"))
                .validWhile(() -> {
                    throw new PvpException(Msg.ERROR_GAME_ENDED);
                });
        Optional<PvpException> refusal = action.attempt(alice(), FormValues.empty());
        assertEquals(Msg.ERROR_GAME_ENDED, refusal.orElseThrow().messageKey());
        assertTrue(ran.isEmpty());
    }
}
