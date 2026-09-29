package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deaths, hits, respawns, quits and game entities reach the right game; players outside games are left alone. */
class GameEventRouterTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Arena tdm;
    private Arena ffa;
    private final List<PlayerDeathEvent> deaths = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        tdm = TestArenas.tdm("AS-TDM", 0, 2);
        ffa = TestArenas.ffa("AS-FFA", 100, 4);
        fixture = PaperFixture.create(server, dir, tdm, ffa);
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
        fixture.register(new DeathSpy());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aKillIsHandedToTheGameWithoutDropsXpOrServerWideMessage() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);

        bob.simulateDamage(100, alice);

        PlayerDeathEvent death = deaths.getLast();
        assertTrue(death.getDrops().isEmpty());
        assertEquals(0, death.getDroppedExp());
        assertNull(death.deathMessage());
        assertEquals(1, session(game, bob).deaths());
        assertEquals(1, session(game, alice).kills());
        assertFalse(session(game, bob).alive());
    }

    @Test
    void aDeathWithoutKillerKeepsTheVanillaTextForTheGameOnly() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, bob);
        PaperFixture.chat(alice);
        PaperFixture.chat(carol);

        bob.setHealth(0);

        List<String> seen = PaperFixture.chat(alice);
        assertEquals(1, seen.size());
        assertTrue(seen.getFirst().contains("Bob"), seen.getFirst());
        assertEquals(List.of(), PaperFixture.chat(carol));
        assertEquals(1, session(game, bob).deaths());
    }

    @Test
    void deathsOutsideGamesAreLeftAlone() {
        PlayerMock carol = fixture.player("Carol");
        carol.getInventory().addItem(new ItemStack(Material.DIRT));

        carol.setHealth(0);

        PlayerDeathEvent death = deaths.getLast();
        assertTrue(death.getDrops().stream().anyMatch(item -> item != null && item.getType() == Material.DIRT));
        assertNotNull(death.deathMessage());
    }

    @Test
    void anFfaPlayerRespawnsAtTheNextSpawnWithAFreshKit() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-FFA", alice, bob);
        bob.simulateDamage(100, alice);

        PlayerRespawnEvent respawn = bob.respawn();
        server.getScheduler().performOneTick();

        Location at = respawn.getRespawnLocation();
        List<NamedPoint> spawns = SpawnPoints.numbered("PlayerSpawn").points(ffa, null);
        assertTrue(spawns.stream().anyMatch(p -> p.x() == at.getX() && p.y() == at.getY() && p.z() == at.getZ()), at.toString());
        assertEquals(24, bob.getInventory().getItem(8).getAmount());
        assertEquals(GameMode.ADVENTURE, bob.getGameMode());
    }

    @Test
    void anEliminatedPlayerRespawnsOnTheArenaSpawnAndSpectates() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
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

    @Test
    void quittingLeavesTheGame() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice);

        alice.disconnect();

        assertTrue(fixture.services().games().gameOf(alice.getUniqueId()).isEmpty());
        assertEquals(Optional.of(LeaveReason.QUIT), session(game, alice).leftReason());
        // The game resets a leaving player inside the quit event, while the server still has them.
        assertSame(server.getScoreboardManager().getMainScoreboard(), alice.getScoreboard());
        assertEquals(344.47, alice.getLocation().getX(), 1e-9);
        assertFalse(fixture.contexts().marker().marked(alice));
    }

    @Test
    void aPlayerWhoDiedBeforeTheCountdownRespawnsOnTheirTeamSpawnWithAKit() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob);
        alice.setHealth(0);

        // Paper doesn't teleport a dead player to the spawns of the countdown; the respawn handler places them.
        fixture.services().games().startCountdown(game);
        PlayerRespawnEvent respawn = alice.respawn();
        server.getScheduler().performOneTick();

        String team = session(game, alice).team().orElseThrow().spec().id();
        NamedPoint spawn = tdm.teamSpawns().get(team).getFirst();
        assertEquals(spawn.x(), respawn.getRespawnLocation().getX(), 1e-9);
        assertEquals(spawn.z(), respawn.getRespawnLocation().getZ(), 1e-9);
        assertEquals(0, session(game, alice).deaths());
        assertEquals(24, alice.getInventory().getItem(8).getAmount());
    }

    @Test
    void theDeathOfAGameEntityIsHandedToItsGame() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);
        UUID zombie = game.spawnEntity("ZOMBIE", ffa.spawn());

        ((LivingEntityMock) server.getEntity(zombie)).simulateDamage(100, alice);

        assertFalse(game.entities().owns(zombie));
    }

    @Test
    void otherEntitiesMayDieInPeace() {
        Game game = fixture.open("AS-FFA");
        Zombie wild = server.getWorld("world").spawn(new Location(server.getWorld("world"), 5, 64, 5), Zombie.class);

        wild.setHealth(0);

        assertTrue(game.entities().all().isEmpty());
        assertTrue(wild.isDead());
    }

    @Test
    void aDeathCreditedToNoOneKeepsTheVanillaTextEvenWithAKiller() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-FFA", alice, bob, carol);
        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);
        PaperFixture.chat(carol);

        bob.setKiller(alice);
        bob.setHealth(0);

        List<String> seen = PaperFixture.chat(carol);
        assertEquals(1, seen.size());
        assertTrue(seen.getFirst().contains("Bob"), seen.getFirst());
        assertEquals(1, session(game, bob).deaths());
        assertEquals(0, session(game, alice).kills());
    }

    @Test
    void aCancelledEntityDeathKeepsTheEntityInItsGame() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);
        UUID zombie = game.spawnEntity("ZOMBIE", ffa.spawn());
        fixture.register(new Reviver(zombie));

        ((LivingEntityMock) server.getEntity(zombie)).simulateDamage(100, alice);

        assertTrue(game.entities().owns(zombie));
    }

    @Test
    void aDeathAnotherPluginCancelsIsNotCounted() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);
        fixture.register(new DeathCanceller());

        bob.simulateDamage(100, alice);

        assertEquals(0, session(game, bob).deaths());
        assertEquals(0, session(game, alice).kills());
        assertTrue(session(game, bob).alive());
        assertEquals(Phase.RUNNING, game.phase());
    }

    @Test
    void anEmptyDeathTextIsNotSent() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);
        fixture.register(new EmptyMessageSetter());
        PaperFixture.chat(alice);

        bob.setHealth(0);

        assertEquals(List.of(), PaperFixture.chat(alice));
        assertEquals(1, session(game, bob).deaths());
    }

    @Test
    void anAllowedHitIsRememberedSoLeavingRightAfterCountsAsKilled() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);

        assertFalse(alice.simulateDamage(5, bob).isCancelled());
        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertEquals(1, session(game, alice).deaths());
        assertEquals(1, session(game, bob).kills());
    }

    @Test
    void theShooterOfAProjectileIsTheOneWhoHit() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);
        Arrow arrow = bob.getWorld().spawn(bob.getLocation(), Arrow.class);
        arrow.setShooter(bob);

        assertFalse(alice.simulateDamage(5, arrow).isCancelled());
        alice.disconnect();

        assertEquals(1, session(game, alice).deaths());
        assertEquals(1, session(game, bob).kills());
    }

    @Test
    void aHitTheGameCancelsIsNotRemembered() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"),
                fixture.player("Dave"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock alice = players.getFirst();
        PlayerMock mate = players.stream().skip(1)
                .filter(p -> session(game, p).team().equals(session(game, alice).team())).findFirst().orElseThrow();

        assertTrue(alice.simulateDamage(5, mate).isCancelled());
        assertTrue(alice.simulateDamage(5, fixture.player("Erin")).isCancelled());
        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertEquals(0, session(game, alice).deaths());
    }

    @Test
    void aHitAnotherPluginCancelsIsNotRemembered() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-FFA", alice, bob);
        fixture.register(new DamageCanceller());

        assertTrue(alice.simulateDamage(5, bob).isCancelled());
        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertEquals(0, session(game, alice).deaths());
    }

    @Test
    void aHitFromOutsideTheGameIsNotRememberedEvenWhenAnotherPluginLetsItThrough() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        Game game = running("AS-FFA", alice, fixture.player("Bob"));
        fixture.register(new DamageAllower());

        assertFalse(alice.simulateDamage(5, dave).isCancelled());
        fixture.services().games().leave(alice.getUniqueId(), LeaveReason.LEFT);

        assertEquals(0, session(game, alice).deaths());
    }

    /** Opens a game on the arena, lets the players join and runs it until the fight starts. */
    private Game running(String arena, PlayerMock... players) {
        Game game = fixture.open(arena);
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private static PlayerSession session(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow();
    }

    private static int teamSize(Game game, PlayerMock player) {
        return session(game, player).team().orElseThrow().members().size();
    }

    /** Sees each death after the router did its work. */
    private final class DeathSpy implements Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(PlayerDeathEvent event) {
            deaths.add(event);
        }
    }

    /** Cancels the death of one entity, as a plugin that revives it would. */
    private static final class Reviver implements Listener {

        private final UUID entity;

        private Reviver(UUID entity) {
            this.entity = entity;
        }

        @EventHandler
        public void onDeath(EntityDeathEvent event) {
            if (event.getEntity().getUniqueId().equals(entity)) {
                event.setCancelled(true);
            }
        }
    }

    /** Cancels every player's death, as a plugin that revives them would. */
    private static final class DeathCanceller implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent event) {
            event.setCancelled(true);
        }
    }

    /** Blanks the death message, as some plugins do without knowing the engine already hides it. */
    private static final class EmptyMessageSetter implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            event.deathMessage(Component.empty());
        }
    }

    /** Cancels every hit after the game's own rules, as a protection plugin would. */
    private static final class DamageCanceller implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDamage(EntityDamageEvent event) {
            event.setCancelled(true);
        }
    }

    /** Lets every hit through after the game's own rules, as a plugin that forces PvP on would. */
    private static final class DamageAllower implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDamage(EntityDamageEvent event) {
            event.setCancelled(false);
        }
    }
}
