package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A player who is in no game respawns at the PvP lobby, instead of the vanilla bed or world spawn. */
class LobbyRespawnTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Arena tdm;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        tdm = TestArenas.tdm("AS-TDM", 0, 2);
        fixture = PaperFixture.create(server, dir, tdm);
        fixture.register(new LobbyRespawn(fixture.services(), fixture.lobby()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aLobbyPlayerWhoDiesRespawnsAtTheLobbyPoint() {
        PlayerMock dave = fixture.player("Dave");
        dave.setHealth(0);

        PlayerRespawnEvent respawn = respawn(dave, RespawnReason.DEATH);

        Location at = respawn.getRespawnLocation();
        assertEquals("world", at.getWorld().getName());
        assertEquals(344.47, at.getX(), 1e-9);
        assertEquals(39, at.getY(), 1e-9);
        assertEquals(521.58, at.getZ(), 1e-9);
    }

    @Test
    void anEndPortalRespawnIsLeftAlone() {
        // The one respawn that doesn't follow a death; a plugin on pvpserver may want to place it itself.
        PlayerMock dave = fixture.player("Dave");
        Location suggested = dave.getWorld().getSpawnLocation();

        PlayerRespawnEvent event = respawn(dave, suggested, RespawnReason.END_PORTAL);

        assertEquals(suggested, event.getRespawnLocation());
    }

    @Test
    void aPluginRespawnAlsoGoesToTheLobby() {
        // Auto-respawn plugins call Player.spigot().respawn() after a death; MockBukkit's PlayerMock.respawn()
        // always reports that as PLUGIN.
        PlayerMock dave = fixture.player("Dave");
        dave.setHealth(0);

        PlayerRespawnEvent respawn = dave.respawn();

        Location at = respawn.getRespawnLocation();
        assertEquals("world", at.getWorld().getName());
        assertEquals(344.47, at.getX(), 1e-9);
        assertEquals(39, at.getY(), 1e-9);
        assertEquals(521.58, at.getZ(), 1e-9);
    }

    @Test
    void onRespawnIsRegisteredAtLowPriority() throws NoSuchMethodException {
        // LOW, not the default, so another plugin's own placement (a mode plugin's, say) still wins over the
        // lobby's; registration order in setUp must not be what makes the other tests here pass.
        EventHandler annotation = LobbyRespawn.class.getMethod("onRespawn", PlayerRespawnEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.LOW, annotation.priority());
    }

    @Test
    void aPlayerStillDeadWhenTheGameClosesRespawnsAtTheLobbyPoint() {
        // The game can close (staff stop, or the result screen's time running out) while a player who died is
        // still on the death screen. By the time they actually respawn, they must already read as being in no
        // game, so this handler - not the game's own, now-gone, respawn rules - places them.
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running(alice, bob);
        alice.simulateDamage(100, bob);

        while (game.phase() != Phase.CLOSED) {
            fixture.services().games().stop(game);
        }
        assertTrue(fixture.services().games().gameOf(alice.getUniqueId()).isEmpty());

        PlayerRespawnEvent respawn = respawn(alice, RespawnReason.DEATH);

        Location at = respawn.getRespawnLocation();
        assertEquals("world", at.getWorld().getName());
        assertEquals(344.47, at.getX(), 1e-9);
        assertEquals(39, at.getY(), 1e-9);
        assertEquals(521.58, at.getZ(), 1e-9);
    }

    @Test
    void aGamePlayerStillRespawnsByTheGamesRules() {
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running(players.toArray(PlayerMock[]::new));
        PlayerMock loner = players.stream().filter(p -> teamSize(game, p) == 1).findFirst().orElseThrow();
        PlayerMock victim = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();

        victim.simulateDamage(100, loner);
        PlayerRespawnEvent respawn = victim.respawn();
        server.getScheduler().performOneTick();

        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(tdm.spawn().x(), respawn.getRespawnLocation().getX(), 1e-9);
        assertEquals(tdm.spawn().z(), respawn.getRespawnLocation().getZ(), 1e-9);
        assertEquals(GameMode.SPECTATOR, victim.getGameMode());
    }

    private Game running(PlayerMock... players) {
        Game game = fixture.open("AS-TDM");
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private static int teamSize(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().team().orElseThrow().members().size();
    }

    /**
     * Fires a respawn with the given reason. PlayerMock's own respawn() always reports PLUGIN, so a test of the
     * reason itself has to build the event by hand.
     */
    private PlayerRespawnEvent respawn(Player player, RespawnReason reason) {
        return respawn(player, player.getWorld().getSpawnLocation(), reason);
    }

    private PlayerRespawnEvent respawn(Player player, Location suggested, RespawnReason reason) {
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, suggested, false, false, false, reason);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
