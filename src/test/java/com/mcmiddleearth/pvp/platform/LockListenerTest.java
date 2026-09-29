package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PaperFixture.ProxyMessage;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** While PvP is locked, only players with pvp.join.locked stay; they see "Server Locked" on the action bar. */
class LockListenerTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = PaperFixture.create(server, dir, TestArenas.tdm("AS-TDM", 0, 2));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aPlayerWithoutPermissionIsToldAndSentAwayOneTickLater() {
        defaultListener();
        fixture.services().lock().lock();

        PlayerMock dave = fixture.player("Dave");

        assertEquals(List.of("PvP-server Locked"), PaperFixture.chat(dave));
        assertEquals(List.of(), fixture.proxyMessages());
        server.getScheduler().performOneTick();
        assertEquals(List.of(new ProxyMessage("Dave", List.of("ConnectOther", "Dave", "world"))), fixture.proxyMessages());
    }

    @Test
    void playersWithThePermissionStay() {
        defaultListener();
        fixture.services().lock().lock();
        PlayerMock staff = new PlayerMock(server, "Staff");
        staff.addAttachment(fixture.plugin(), Permissions.JOIN_LOCKED, true);

        server.addPlayer(staff);
        server.getScheduler().performOneTick();

        assertEquals(List.of(), PaperFixture.chat(staff));
        assertEquals(List.of(), fixture.proxyMessages());
    }

    @Test
    void nobodyIsSentAwayWhileUnlocked() {
        defaultListener();
        PlayerMock dave = fixture.player("Dave");
        server.getScheduler().performOneTick();

        assertEquals(List.of(), PaperFixture.chat(dave));
        assertEquals(List.of(), fixture.proxyMessages());
    }

    @Test
    void theLockedActionBarShowsOnceASecondToPlayersWhoMayStay() {
        LockListener listener = defaultListener();
        PlayerMock staff = fixture.player("Staff");
        staff.setOp(true);
        PlayerMock dave = fixture.player("Dave");
        listener.start();
        fixture.services().lock().lock();

        server.getScheduler().performTicks(20);
        assertEquals("Server Locked", PaperFixture.plain(staff.nextActionBar()));
        assertNull(dave.nextActionBar());
        server.getScheduler().performTicks(20);
        assertEquals("Server Locked", PaperFixture.plain(staff.nextActionBar()));

        fixture.services().lock().unlock();
        server.getScheduler().performTicks(20);
        assertNull(staff.nextActionBar());
    }

    @Test
    void aPlayerWaitsUntilTheProxyBecomesReady() {
        AtomicBoolean ready = new AtomicBoolean(false);
        List<String> sentTo = new ArrayList<>();
        fixture.register(readinessListener(player -> ready.get(), sentTo));
        fixture.services().lock().lock();

        fixture.player("Dave");
        server.getScheduler().performTicks(10);
        assertEquals(List.of(), sentTo);

        ready.set(true);
        server.getScheduler().performOneTick();
        assertEquals(List.of("Dave"), sentTo);

        server.getScheduler().performTicks(10);
        assertEquals(List.of("Dave"), sentTo);
    }

    @Test
    void anUnlockWhileWaitingForTheProxySendsNobody() {
        AtomicBoolean ready = new AtomicBoolean(false);
        List<String> sentTo = new ArrayList<>();
        fixture.register(readinessListener(player -> ready.get(), sentTo));
        fixture.services().lock().lock();

        fixture.player("Dave");
        server.getScheduler().performTicks(10);
        fixture.services().lock().unlock();
        ready.set(true);
        server.getScheduler().performTicks(10);

        assertEquals(List.of(), sentTo);
    }

    @Test
    void aPlayerWhoNeverBecomesReadyIsNeverSentAndIsLogged() {
        List<String> sentTo = new ArrayList<>();
        fixture.register(readinessListener(player -> false, sentTo));
        fixture.services().lock().lock();

        List<LogRecord> warnings = new ArrayList<>();
        Handler handler = capturing(warnings);
        fixture.plugin().getLogger().addHandler(handler);
        try {
            fixture.player("Dave");

            server.getScheduler().performTicks(99);
            assertEquals(List.of(), warnings);

            server.getScheduler().performOneTick();
            assertEquals(1, warnings.size());
            assertEquals(Level.WARNING, warnings.get(0).getLevel());
            assertTrue(warnings.get(0).getMessage().contains("bungee-plugin-message-channel"));
            assertTrue(warnings.get(0).getMessage().contains("velocity.toml"));

            server.getScheduler().performTicks(20);
            assertEquals(List.of(), sentTo);
            assertEquals(1, warnings.size());
        } finally {
            fixture.plugin().getLogger().removeHandler(handler);
        }
    }

    @Test
    void aPlayerWhoQuitsWhileWaitingIsNeitherSentNorLogged() {
        List<String> sentTo = new ArrayList<>();
        fixture.register(readinessListener(player -> false, sentTo));
        fixture.services().lock().lock();

        List<LogRecord> warnings = new ArrayList<>();
        Handler handler = capturing(warnings);
        fixture.plugin().getLogger().addHandler(handler);
        try {
            PlayerMock dave = fixture.player("Dave");

            server.getScheduler().performTicks(10);
            dave.disconnect();

            server.getScheduler().performTicks(100);
            assertEquals(List.of(), sentTo);
            assertEquals(List.of(), warnings);
        } finally {
            fixture.plugin().getLogger().removeHandler(handler);
        }
    }

    @Test
    void theLockedActionBarSkipsAPlayerInARunningGame() {
        LockListener listener = defaultListener();
        PlayerMock staff = fixture.player("Staff");
        staff.setOp(true);
        PlayerMock dave = fixture.player("Dave");
        listener.start();
        fixture.services().lock().lock();

        Game game = fixture.open("AS-TDM");
        fixture.join(game, staff, dave);
        fixture.startAndRun(game);
        drainActionBar(staff);

        server.getScheduler().performTicks(20);
        assertNull(staff.nextActionBar());
    }

    @Test
    void theLockedActionBarStillShowsInTheLobbyAndBeforeAGameRuns() {
        LockListener listener = defaultListener();
        PlayerMock staff = fixture.player("Staff");
        staff.setOp(true);
        listener.start();
        fixture.services().lock().lock();

        server.getScheduler().performTicks(20);
        assertEquals("Server Locked", PaperFixture.plain(staff.nextActionBar()));

        Game game = fixture.open("AS-TDM");
        fixture.join(game, staff);
        server.getScheduler().performTicks(20);
        assertEquals("Server Locked", PaperFixture.plain(staff.nextActionBar()));
    }

    /** Registers, and returns, a listener on the fixture's own always-ready proxy. */
    private LockListener defaultListener() {
        LockListener listener = new LockListener(fixture.services(), fixture.plugin());
        fixture.register(listener);
        return listener;
    }

    /** A listener whose proxy's readiness is controlled by {@code ready}, recording every player it sends by name. */
    private LockListener readinessListener(Predicate<Player> ready, List<String> sentTo) {
        PvpServices base = fixture.services();
        ProxyMessenger proxy = new ProxyMessenger(fixture.plugin(), base.config().mainServer(),
                (through, payload) -> sentTo.add(through.getName()), ready);
        PvpServices withProxy = new PvpServices(base.config(), base.messages(), base.arenas(), base.modes(),
                base.games(), base.stats(), base.lobby(), base.lock(), proxy);
        return new LockListener(withProxy, fixture.plugin());
    }

    /** Discards whatever action bar messages are already queued, so only later ones are checked. */
    private static void drainActionBar(PlayerMock player) {
        while (player.nextActionBar() != null) {
            // discard
        }
    }

    private static Handler capturing(List<LogRecord> into) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                into.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}
