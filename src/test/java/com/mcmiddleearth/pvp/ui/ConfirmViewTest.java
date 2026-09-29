package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.testing.RecordingNavigator;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mcmiddleearth.pvp.testing.UiTesting.click;
import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmViewTest {

    private final RecordingNavigator navigator = new RecordingNavigator();
    private final ConfirmView confirm = new ConfirmView(Messages.bundled(), navigator);
    private final Viewer staff = new Viewer(UUID.randomUUID(), "Staff", node -> true);
    private final List<String> stopped = new ArrayList<>();
    private boolean stillOpen = true;

    private UiAction stop() {
        return UiAction.of((viewer, values) -> stopped.add(viewer.name()))
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> stillOpen ? Optional.empty() : Optional.of(new PvpException(Msg.ERROR_GAME_ENDED)));
    }

    @Test
    void theQuestionHasASingleUseDangerousYesAndAHarmlessNo() {
        ConfirmScreen screen = confirm.confirm(Component.text("Stop?"), Component.text("Everyone leaves."), stop());
        assertEquals("Stop?", plain(screen.title()));
        assertEquals(List.of("Everyone leaves."), plain(screen.body()));
        assertEquals("Yes", plain(screen.yes().label()));
        assertEquals(ButtonStyle.DANGER, screen.yes().style());
        assertTrue(screen.yes().action().singleUse());
        assertEquals("No", plain(screen.no().label()));
        assertTrue(screen.no().action().isNone());
    }

    @Test
    void theAskingButtonShowsTheQuestionAndDoesNothingYet() {
        UiAction asking = confirm.asking(Component.text("Stop?"), Component.text("Everyone leaves."), stop());
        assertFalse(asking.singleUse());
        assertTrue(asking.attempt(staff, FormValues.empty()).isEmpty());
        assertTrue(stopped.isEmpty());
        ConfirmScreen shown = (ConfirmScreen) navigator.lastScreen();
        assertTrue(click(shown.yes(), staff).isEmpty());
        assertEquals(List.of("Staff"), stopped);
    }

    @Test
    void yesRefusesAndDoesNotRunWhenTheActionStoppedBeingValidAfterAsking() {
        UiAction asking = confirm.asking(Component.text("Stop?"), Component.text("Everyone leaves."), stop());
        assertTrue(asking.attempt(staff, FormValues.empty()).isEmpty());
        ConfirmScreen shown = (ConfirmScreen) navigator.lastScreen();
        stillOpen = false;
        assertEquals(Msg.ERROR_GAME_ENDED, click(shown.yes(), staff).orElseThrow().messageKey());
        assertTrue(stopped.isEmpty());
    }

    @Test
    void aStaleAskingButtonIsRefusedBeforeItAsks() {
        UiAction asking = confirm.asking(Component.text("Stop?"), Component.text("Everyone leaves."), stop());
        stillOpen = false;
        assertEquals(Msg.ERROR_GAME_ENDED, asking.attempt(staff, FormValues.empty()).orElseThrow().messageKey());
        assertTrue(navigator.log().isEmpty());
    }

    @Test
    void theAskingButtonKeepsThePermissionOfTheAction() {
        UiAction stop = stop();
        UiAction asking = confirm.asking(Component.text("Stop?"), Component.text("Everyone leaves."), stop);
        assertEquals(Permissions.STAFF_RUN, asking.permission());
        assertSame(stop.check(), asking.check());
        Viewer player = new Viewer(UUID.randomUUID(), "Player", Permissions.PLAY::equals);
        assertEquals(Msg.ERROR_NO_PERMISSION, asking.attempt(player, FormValues.empty()).orElseThrow().messageKey());
    }
}
