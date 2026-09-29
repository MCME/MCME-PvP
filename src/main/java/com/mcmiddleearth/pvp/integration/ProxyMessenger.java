package com.mcmiddleearth.pvp.integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Predicate;

/**
 * Talks to the proxy over the {@code BungeeCord} plugin-message channel, which Velocity understands with
 * {@code bungee-plugin-message-channel = true}. Only two sub-channels are used, as in 1.1.2: {@code ConnectOther}
 * moves a player to another server and {@code Message ALL} broadcasts to the whole network. A plugin message
 * always travels through the connection of some online player.
 */
public final class ProxyMessenger {

    /** The legacy channel name; Bukkit maps it to {@code bungeecord:main}. */
    static final String CHANNEL = "BungeeCord";
    /** The namespaced form Bukkit stores a registration of {@link #CHANNEL} under. */
    private static final String CHANNEL_NAMESPACED = "bungeecord:main";
    /** The fix for a player whose proxy channel never became ready, shared by every warning that names one. */
    public static final String CHANNEL_HINT = "Check bungee-plugin-message-channel = true in velocity.toml.";

    /** Sends payload bytes through a player's connection. Tests pass a recording one: MockBukkit records nothing. */
    @FunctionalInterface
    public interface Sender {
        void send(Player through, byte[] payload);
    }

    private final Plugin plugin;
    private final String mainServer;
    private final Sender sender;
    private final Predicate<Player> ready;

    public ProxyMessenger(Plugin plugin, String mainServer) {
        this(plugin, mainServer, (player, payload) -> player.sendPluginMessage(plugin, CHANNEL, payload),
                player -> player.getListeningPluginChannels().contains(CHANNEL)
                        || player.getListeningPluginChannels().contains(CHANNEL_NAMESPACED));
    }

    public ProxyMessenger(Plugin plugin, String mainServer, Sender sender) {
        this(plugin, mainServer, sender, player -> true);
    }

    public ProxyMessenger(Plugin plugin, String mainServer, Sender sender, Predicate<Player> ready) {
        this.plugin = plugin;
        this.mainServer = mainServer;
        this.sender = sender;
        this.ready = ready;
    }

    /** Registers the outgoing channel; sending before this throws {@code ChannelNotRegisteredException}. */
    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    /**
     * Asks the proxy to move the player to the main server ({@code mainServer} in config.yml). Runs on the server
     * thread; silently does nothing when the player is no longer online, since a caller may hold a {@link Player}
     * captured before a one-tick delay during which they disconnected.
     */
    public void sendToMainServer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        sender.send(player, connectOther(player.getName(), mainServer));
    }

    /**
     * Whether the proxy has registered the BungeeCord channel for this player; Paper drops a plugin message sent
     * before that.
     */
    public boolean ready(Player player) {
        return ready.test(player);
    }

    /**
     * Broadcasts legacy text to every server, through the first online player whose proxy channel is ready;
     * nothing happens when nobody is online, or nobody's channel is ready yet, to carry the message. Runs on the
     * server thread.
     */
    public void broadcast(String legacyText) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (ready(player)) {
                sender.send(player, message("ALL", legacyText));
                return;
            }
        }
    }

    /** {@code ConnectOther <player> <server>}. */
    static byte[] connectOther(String player, String server) {
        return utf("ConnectOther", player, server);
    }

    /** {@code Message <target> <text>}; the target {@code ALL} reaches every player on the network. */
    static byte[] message(String target, String text) {
        return utf("Message", target, text);
    }

    private static byte[] utf(String... parts) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            for (String part : parts) {
                out.writeUTF(part);
            }
        } catch (IOException e) {
            // writeUTF only throws past 65535 bytes for a single part, which none of these are; an in-memory
            // stream raises nothing else here. The limit that actually binds a caller is Bukkit's
            // Messenger.MAX_MESSAGE_SIZE (32766 bytes) on the whole payload, enforced by sendPluginMessage with
            // a MessageTooLargeException, not by this method.
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
