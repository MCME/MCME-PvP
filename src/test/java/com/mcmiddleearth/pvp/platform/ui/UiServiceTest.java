package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.Button;
import com.mcmiddleearth.pvp.ui.ChatLine;
import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.ChatPart;
import com.mcmiddleearth.pvp.ui.MenuScreen;
import com.mcmiddleearth.pvp.ui.NoticeScreen;
import com.mcmiddleearth.pvp.ui.UiAction;
import com.mcmiddleearth.pvp.ui.Viewer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static com.mcmiddleearth.pvp.testing.UiTesting.plain;
import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiServiceTest {

    private final List<String> ran = new ArrayList<>();
    private final RecordingLog log = new RecordingLog();
    private ServerMock server;
    private PluginMock plugin;
    private DialogTracker tracker;
    private DialogRenderer dialogs;
    private UiService service;
    private PlayerMock alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        Messages messages = Messages.bundled();
        tracker = new DialogTracker(player -> {
        });
        PaperUiCallbacks callbacks = new PaperUiCallbacks(plugin, messages, tracker);
        dialogs = new DialogRenderer(callbacks, messages);
        service = new UiService(dialogs, new ChatRenderer(callbacks, messages), tracker, messages, log.logger());
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

    private List<String> chat(PlayerMock player) {
        List<String> lines = new ArrayList<>();
        for (Component line = player.nextComponentMessage(); line != null; line = player.nextComponentMessage()) {
            lines.add(plain(line));
        }
        return lines;
    }

    @Test
    void chatMenusAndMessagesGoToTheViewersChat() {
        service.show(Viewers.of(alice), ChatMenu.of(text("PvP games"),
                List.of(ChatLine.of(ChatPart.text(text("No games right now."))))));
        service.message(Viewers.of(alice), text("You're back in the lobby."));
        assertEquals(List.of("PvP games", "No games right now.", "You're back in the lobby."), chat(alice));
    }

    @Test
    void withoutDialogSupportAScreenIsWrittenAsChat() {
        assertFalse(dialogs.supported());
        service.show(Viewers.of(alice), new NoticeScreen(text("Rules"), List.of(text("No respawns.")),
                Button.of(text("Close"), UiAction.none())));
        assertEquals(List.of("Rules", "No respawns."), chat(alice));
        assertFalse(tracker.isOpen(alice.getUniqueId()));
    }

    @Test
    void theChatFallbackIsLoggedOnce() {
        service.show(Viewers.of(alice), new NoticeScreen(text("Rules"), List.of(),
                Button.of(text("Close"), UiAction.none())));
        service.show(Viewers.of(alice), new NoticeScreen(text("More"), List.of(),
                Button.of(text("Close"), UiAction.none())));
        assertEquals(List.of("Paper dialogs are not available, so menus are shown in chat instead"),
                log.messages(Level.WARNING));
    }

    @Test
    void theButtonsOfAScreenWrittenAsChatStillWork() {
        service.show(Viewers.of(alice), new MenuScreen(text("PvP"), List.of(),
                List.of(Button.of(text("Rules"), UiAction.of((viewer, values) -> ran.add(viewer.name())))),
                1, Button.of(text("Close"), UiAction.none())));
        assertEquals("PvP", plain(alice.nextComponentMessage()));
        Component rules = alice.nextComponentMessage();
        assertEquals("[Rules]", plain(rules));
        assertTrue(CapturingClickCallbackProvider.click(rules.clickEvent(), alice));
        assertEquals(List.of("Alice"), ran);
    }

    @Test
    void performRunsTheCommandAsTheViewer() {
        server.getCommandMap().register("uitest", new Command("uitest") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ran.add(sender.getName() + " " + String.join(" ", args));
                return true;
            }
        });
        service.perform(Viewers.of(alice), "uitest kickall");
        assertEquals(List.of("Alice kickall"), ran);
        assertEquals(List.of("Alice runs /uitest kickall from a menu"), log.messages(Level.INFO));
        assertNull(alice.nextComponentMessage());
    }

    @Test
    void logWritesToTheServerLog() {
        service.log("Alice starts the countdown on AS-TDM from a menu");

        assertEquals(List.of("Alice starts the countdown on AS-TDM from a menu"), log.messages(Level.INFO));
    }

    @Test
    void aCommandThatDoesNotRunIsAnsweredAndLogged() {
        service.perform(Viewers.of(alice), "nosuchcommand");
        assertEquals(List.of("Something went wrong with that button. Open the menu again with /pvp."), chat(alice));
        assertEquals(List.of("/nosuchcommand from a menu did not run for Alice"), log.messages(Level.WARNING));
    }

    @Test
    void viewersWhoAreOfflineAreSkipped() {
        server.getCommandMap().register("uitest", new Command("uitest") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ran.add(sender.getName() + " " + String.join(" ", args));
                return true;
            }
        });
        Viewer gone = new Viewer(UUID.randomUUID(), "Gone", node -> true);
        service.show(gone, new NoticeScreen(text("Rules"), List.of(), Button.of(text("Close"), UiAction.none())));
        service.show(gone, ChatMenu.of(text("PvP games"), List.of()));
        service.message(gone, text("hello"));
        service.perform(gone, "uitest");
        assertNull(alice.nextComponentMessage());
        assertTrue(ran.isEmpty());
        assertTrue(log.messages(Level.INFO).isEmpty());
    }
}
