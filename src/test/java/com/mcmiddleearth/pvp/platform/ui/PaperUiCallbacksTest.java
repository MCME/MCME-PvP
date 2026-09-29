package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import com.mcmiddleearth.pvp.ui.Choice;
import com.mcmiddleearth.pvp.ui.FormValues;
import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.Slider;
import com.mcmiddleearth.pvp.ui.Toggle;
import com.mcmiddleearth.pvp.ui.UiAction;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperUiCallbacksTest {

    private final List<String> ran = new ArrayList<>();
    private ServerMock server;
    private MovableClock clock;
    private DialogTracker tracker;
    private PaperUiCallbacks callbacks;
    private PlayerMock alice;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        clock = new MovableClock(Instant.parse("2026-09-24T12:00:00Z"));
        tracker = new DialogTracker(player -> {
        });
        callbacks = new PaperUiCallbacks(plugin, Messages.bundled(), tracker, clock);
        alice = server.addPlayer("Alice");
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } finally {
            CapturingClickCallbackProvider.reset();
        }
    }

    private UiAction recording() {
        return UiAction.of((viewer, values) -> ran.add(viewer.name() + (server.isPrimaryThread() ? "" : " off-thread")));
    }

    private String nextChat(PlayerMock player) {
        Component message = player.nextComponentMessage();
        return message == null ? null : plain(message);
    }

    @Test
    void aClickRunsTheActionForItsPlayer() {
        callbacks.chatCallback(Viewers.of(alice), recording()).accept(alice);
        assertEquals(List.of("Alice"), ran);
        assertNull(alice.nextComponentMessage());
    }

    @Test
    void thePermissionIsAskedOfTheLivePlayerAtClickTime() {
        ClickCallback<Audience> callback =
                callbacks.chatCallback(Viewers.of(alice), recording().requires(Permissions.STAFF_RUN));
        callback.accept(alice);
        assertEquals("You don't have permission to do that.", nextChat(alice));
        assertTrue(ran.isEmpty());
        alice.setOp(true);
        callback.accept(alice);
        assertEquals(List.of("Alice"), ran);
    }

    @Test
    void aButtonThatIsNoLongerValidAnswersAndDoesNothing() {
        UiAction ended = recording().validWhile(() -> Optional.of(new PvpException(Msg.ERROR_GAME_ENDED)));
        callbacks.chatCallback(Viewers.of(alice), ended).accept(alice);
        assertEquals("That game has ended.", nextChat(alice));
        assertTrue(ran.isEmpty());
    }

    @Test
    void aRefusalFromTheActionBecomesChat() {
        UiAction full = UiAction.of((viewer, values) -> {
            throw new PvpException(Msg.ERROR_GAME_FULL, Map.of("max", "2"));
        });
        callbacks.chatCallback(Viewers.of(alice), full).accept(alice);
        assertEquals("That game is full (2 players).", nextChat(alice));
    }

    @Test
    void buttonsStopWorkingAfterTenMinutes() {
        ClickCallback<Audience> callback = callbacks.chatCallback(Viewers.of(alice), recording());
        clock.advance(Duration.ofMinutes(10));
        callback.accept(alice);
        assertEquals(List.of("Alice"), ran);
        clock.advance(Duration.ofSeconds(1));
        callback.accept(alice);
        assertEquals(List.of("Alice"), ran);
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
    }

    @Test
    void aSingleUseButtonWorksOnce() {
        ClickCallback<Audience> callback =
                callbacks.chatCallback(Viewers.of(alice), recording().once());
        callback.accept(alice);
        callback.accept(alice);
        assertEquals(List.of("Alice"), ran);
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
    }

    @Test
    void onlyThePlayerTheButtonWasMadeForCanUseIt() {
        PlayerMock bob = server.addPlayer("Bob");
        callbacks.chatCallback(Viewers.of(alice), recording()).accept(bob);
        assertTrue(ran.isEmpty());
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(bob));
    }

    @Test
    void aClickAfterThePluginIsDisabledRunsNothing() {
        ClickCallback<Audience> callback = callbacks.chatCallback(Viewers.of(alice), recording());
        server.getPluginManager().disablePlugin(plugin);
        callback.accept(alice);
        assertTrue(ran.isEmpty());
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
    }

    @Test
    void closingADialogAfterThePluginIsDisabledSaysNothing() {
        tracker.opened(alice.getUniqueId());
        DialogActionCallback callback = callbacks.dialogCallback(Viewers.of(alice), UiAction.none(), List.of());
        server.getPluginManager().disablePlugin(plugin);
        callback.accept(new FakeResponse(Map.of(), Map.of(), Map.of()), alice);
        assertNull(nextChat(alice));
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void aHandlerThatFailsIsLoggedAndTheClickerIsAnswered() {
        List<LogRecord> logged = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        plugin.getLogger().addHandler(handler);
        try {
            IllegalStateException failure = new IllegalStateException("broken handler");
            UiAction broken = UiAction.of((viewer, values) -> {
                throw failure;
            });
            callbacks.chatCallback(Viewers.of(alice), broken).accept(alice);
            assertEquals("Something went wrong with that button. Open the menu again with /pvp.", nextChat(alice));
            assertEquals(1, logged.size());
            assertEquals(Level.SEVERE, logged.get(0).getLevel());
            assertTrue(logged.get(0).getMessage().contains("Alice"));
            assertSame(failure, logged.get(0).getThrown());
        } finally {
            // MockBukkit plugin loggers are shared by name, so a handler left behind would leak into later tests.
            plugin.getLogger().removeHandler(handler);
        }
    }

    @Test
    void closingADialogAfterItsButtonsExpiredSaysNothing() {
        tracker.opened(alice.getUniqueId());
        DialogActionCallback callback = callbacks.dialogCallback(Viewers.of(alice), UiAction.none(), List.of());
        clock.advance(Duration.ofMinutes(10).plusSeconds(30));
        callback.accept(new FakeResponse(Map.of(), Map.of(), Map.of()), alice);
        assertNull(nextChat(alice));
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void theTrackedDialogIsClosedBeforeTheHandlerSoAReopenedScreenStaysTracked() {
        tracker.opened(alice.getUniqueId());
        UiAction reopens = UiAction.of((viewer, values) -> tracker.opened(viewer.id()));
        callbacks.dialogCallback(Viewers.of(alice), reopens, List.of())
                .accept(new FakeResponse(Map.of(), Map.of(), Map.of()), alice);
        assertTrue(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void aClickFromAnotherThreadRunsOnTheServerThread() throws InterruptedException {
        ClickCallback<Audience> callback = callbacks.chatCallback(Viewers.of(alice), recording());
        Thread network = new Thread(() -> callback.accept(alice));
        network.start();
        network.join();
        assertTrue(ran.isEmpty());
        server.getScheduler().performOneTick();
        assertEquals(List.of("Alice"), ran);
    }

    @Test
    void closingADialogFromAnotherThreadClearsTheTrackerOnTheServerThread() throws InterruptedException {
        tracker.opened(alice.getUniqueId());
        DialogActionCallback callback = callbacks.dialogCallback(Viewers.of(alice), UiAction.none(), List.of());
        Thread network = new Thread(() -> callback.accept(new FakeResponse(Map.of(), Map.of(), Map.of()), alice));
        network.start();
        network.join();
        assertTrue(tracker.isOpen(alice.getUniqueId()));
        server.getScheduler().performOneTick();
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void dialogValuesAreReadAsTheFormExpectsThem() {
        List<Input> inputs = List.of(
                new Slider("teamCount", Component.text("Teams"), 2, 4, 1, 2),
                new Slider("timeLimit", Component.text("Time"), 1, 60, 1, 10),
                new Toggle("midgameJoin", Component.text("Late"), true),
                new Choice("map", Component.text("Map"), List.of(new Choice.Option("a", Component.text("A"))), "a"),
                new Slider("maxPlayers", Component.text("Max"), 2, 16, 1, 16));
        FormValues values = PaperUiCallbacks.read(new FakeResponse(
                Map.of("teamCount", 3.4f, "timeLimit", 9.6f), Map.of("midgameJoin", false), Map.of("map", "a")), inputs);
        assertEquals(Map.of("teamCount", 3, "timeLimit", 10, "midgameJoin", false, "map", "a"), values.values());
    }

    @Test
    void aDialogButtonGetsTheSubmittedValuesAndClosesTheTrackedDialog() {
        tracker.opened(alice.getUniqueId());
        List<Input> inputs = List.of(new Toggle("midgameJoin", Component.text("Late"), true));
        UiAction submit = UiAction.of((viewer, values) -> ran.add(String.valueOf(values.values())));
        callbacks.dialogCallback(Viewers.of(alice), submit, inputs)
                .accept(new FakeResponse(Map.of(), Map.of("midgameJoin", true), Map.of()), alice);
        assertEquals(List.of("{midgameJoin=true}"), ran);
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void chatButtonsGetGenerousAdventureLimitsSoWeAnswerLateOrRepeatClicksOurselves() {
        ClickEvent<?> event = callbacks.chat(Viewers.of(alice), recording().once());
        ClickCallback.Options options = CapturingClickCallbackProvider.options(event).orElseThrow();
        assertEquals(ClickCallback.UNLIMITED_USES, options.uses());
        assertTrue(options.lifetime().compareTo(Duration.ofHours(1)) >= 0, options.lifetime().toString());
        assertTrue(CapturingClickCallbackProvider.click(event, alice));
        assertEquals(List.of("Alice"), ran);
        ClickCallback.Options repeatable = PaperUiCallbacks.options(recording());
        assertEquals(ClickCallback.UNLIMITED_USES, repeatable.uses());
        assertTrue(repeatable.lifetime().compareTo(Duration.ofHours(1)) >= 0, repeatable.lifetime().toString());
    }

    @Test
    void aClickThirtyMinutesLaterStillGetsAnAnswerInsteadOfPapersSilence() {
        ClickEvent<?> event = callbacks.chat(Viewers.of(alice), recording());
        clock.advance(Duration.ofMinutes(30));

        assertTrue(CapturingClickCallbackProvider.click(event, alice));

        assertTrue(ran.isEmpty());
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
    }

    @Test
    void aThirdClickOnASingleUseButtonStillGetsAnAnswer() {
        ClickEvent<?> event = callbacks.chat(Viewers.of(alice), recording().once());

        assertTrue(CapturingClickCallbackProvider.click(event, alice));
        assertTrue(CapturingClickCallbackProvider.click(event, alice));
        assertTrue(CapturingClickCallbackProvider.click(event, alice));

        assertEquals(List.of("Alice"), ran);
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
        assertEquals("That button is out of date. Open the menu again with /pvp.", nextChat(alice));
        assertNull(nextChat(alice));
    }

    /** What a dialog client sent, by kind of input. */
    private record FakeResponse(Map<String, Float> floats, Map<String, Boolean> booleans, Map<String, String> texts)
            implements DialogResponseView {

        @Override
        public BinaryTagHolder payload() {
            return null;
        }

        @Override
        public String getText(String key) {
            return texts.get(key);
        }

        @Override
        public Boolean getBoolean(String key) {
            return booleans.get(key);
        }

        @Override
        public Float getFloat(String key) {
            return floats.get(key);
        }
    }

    /** A clock the test moves by hand. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
