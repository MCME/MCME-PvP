package com.mcmiddleearth.pvp.integration;

import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyMessengerTest {

    private ServerMock server;
    private PluginMock plugin;
    private final List<String> through = new ArrayList<>();
    private final List<List<String>> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void connectOtherNamesThePlayerAndTheServer() throws IOException {
        assertEquals(List.of("ConnectOther", "Alice", "world"), read(ProxyMessenger.connectOther("Alice", "world")));
    }

    @Test
    void messageTargetsEveryServer() throws IOException {
        assertEquals(List.of("Message", "ALL", "§7Alice opened a game"),
                read(ProxyMessenger.message("ALL", "§7Alice opened a game")));
    }

    @Test
    void sendingNeedsTheRegisteredChannel() {
        PlayerMock alice = server.addPlayer("Alice");
        ProxyMessenger proxy = new ProxyMessenger(plugin, "world");
        assertThrows(ChannelNotRegisteredException.class, () -> proxy.sendToMainServer(alice));

        proxy.register();

        assertTrue(server.getMessenger().isOutgoingChannelRegistered(plugin, "BungeeCord"));
        assertDoesNotThrow(() -> proxy.sendToMainServer(alice));
        assertDoesNotThrow(() -> proxy.broadcast("§aHello"));
    }

    @Test
    void sendToMainServerGoesThroughThatPlayer() {
        PlayerMock bob = server.addPlayer("Bob");
        server.addPlayer("Carol");
        recording("lobby").sendToMainServer(bob);

        assertEquals(List.of("Bob"), through);
        assertEquals(List.of(List.of("ConnectOther", "Bob", "lobby")), sent);
    }

    @Test
    void broadcastNeedsSomeoneOnlineToCarryIt() {
        ProxyMessenger proxy = recording("world");
        proxy.broadcast("§7nobody hears this");
        assertEquals(List.of(), sent);

        server.addPlayer("Carol");
        proxy.broadcast("§7hello");

        assertEquals(List.of("Carol"), through);
        assertEquals(List.of(List.of("Message", "ALL", "§7hello")), sent);
    }

    @Test
    void broadcastWaitsForAReadyChannel() {
        server.addPlayer("Bob");
        ProxyMessenger proxy = new ProxyMessenger(plugin, "world", (player, payload) -> {
            through.add(player.getName());
            try {
                sent.add(read(payload));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }, player -> player.getName().equals("Carol"));

        proxy.broadcast("§7nobody is ready yet");
        assertEquals(List.of(), sent);

        server.addPlayer("Carol");
        proxy.broadcast("§7hello");

        assertEquals(List.of("Carol"), through);
        assertEquals(List.of(List.of("Message", "ALL", "§7hello")), sent);
    }

    @Test
    void sendToMainServerSkipsAPlayerWhoWentOffline() {
        PlayerMock bob = server.addPlayer("Bob");
        ProxyMessenger proxy = recording("lobby");
        bob.disconnect();

        assertDoesNotThrow(() -> proxy.sendToMainServer(bob));

        assertEquals(List.of(), through);
        assertEquals(List.of(), sent);
    }

    private ProxyMessenger recording(String mainServer) {
        return new ProxyMessenger(plugin, mainServer, (player, payload) -> {
            through.add(player.getName());
            try {
                sent.add(read(payload));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
    }

    private static List<String> read(byte[] payload) throws IOException {
        List<String> parts = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            while (in.available() > 0) {
                parts.add(in.readUTF());
            }
        }
        return parts;
    }
}
