package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.ProbeMode;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1.1.2's damage rules, per game, through the router. */
class DamageRulesTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nothingHurtsBeforeTheGameRuns() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, bob);

        assertTrue(bob.simulateDamage(5, alice).isCancelled());
        assertTrue(fire(alice, DamageCause.FALL).isCancelled());
        fixture.services().games().startCountdown(game);
        assertTrue(bob.simulateDamage(5, alice).isCancelled());
        assertEquals(20, bob.getHealth());
    }

    @Test
    void lavaAndFireStillHurtBeforeTheGameRunsWithLavaDamageOn() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertFalse(fire(alice, DamageCause.LAVA).isCancelled());
        assertFalse(fire(alice, DamageCause.FIRE_TICK).isCancelled());
        assertTrue(fire(alice, DamageCause.FIRE).isCancelled());
    }

    @Test
    void withLavaDamageOffLavaIsHarmlessBeforeTheGameRuns() {
        init(PaperFixture.config(false, true));
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertTrue(fire(alice, DamageCause.LAVA).isCancelled());
        assertTrue(fire(alice, DamageCause.FIRE_TICK).isCancelled());
    }

    @Test
    void opponentsHurtEachOtherWhileTheGameRuns() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-TDM", alice, bob);

        assertFalse(bob.simulateDamage(5, alice).isCancelled());
        assertEquals(15, bob.getHealth());
        assertFalse(fire(alice, DamageCause.FALL).isCancelled());
    }

    @Test
    void contactDamageIsCancelledWhileTheGameRuns() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        running("AS-TDM", alice, fixture.player("Bob"));

        assertTrue(fire(alice, DamageCause.CONTACT).isCancelled());
    }

    @Test
    void teammatesCannotHurtEachOther() {
        init(PvpConfig.defaults());
        List<PlayerMock> players = players(4);
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock first = players.getFirst();
        PlayerMock mate = players.stream().skip(1).filter(p -> color(game, p).equals(color(game, first))).findFirst().orElseThrow();
        PlayerMock enemy = players.stream().filter(p -> !color(game, p).equals(color(game, first))).findFirst().orElseThrow();

        assertTrue(mate.simulateDamage(5, first).isCancelled());
        assertFalse(enemy.simulateDamage(5, first).isCancelled());
    }

    @Test
    void theSamePersonalColorDoesNotMakeTeammates() {
        init(PvpConfig.defaults());
        List<PlayerMock> players = players(12);
        Game game = running("AS-FFA", players.toArray(PlayerMock[]::new));
        PlayerMock first = players.getFirst();
        PlayerMock twin = players.stream().skip(1).filter(p -> color(game, p).equals(color(game, first))).findFirst().orElseThrow();

        assertTrue(color(game, first).personal());
        assertFalse(twin.simulateDamage(5, first).isCancelled());
    }

    @Test
    void ownArrowsCannotHitTheShooter() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-TDM", alice, bob);

        assertTrue(alice.simulateDamage(5, arrow(alice)).isCancelled());
        assertFalse(alice.simulateDamage(5, arrow(bob)).isCancelled());
    }

    @Test
    void spectatorsNeitherDealNorTakeDamage() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-TDM", alice, fixture.player("Bob"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertTrue(carol.simulateDamage(5, alice).isCancelled());
        assertTrue(alice.simulateDamage(5, carol).isCancelled());
    }

    @Test
    void gamePlayersAndOutsidersCannotHurtEachOther() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        running("AS-TDM", alice, fixture.player("Bob"));

        assertTrue(alice.simulateDamage(5, dave).isCancelled());
        assertTrue(dave.simulateDamage(5, alice).isCancelled());
    }

    @Test
    void playersInDifferentGamesCannotHurtEachOther() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock carol = fixture.player("Carol");
        running("AS-TDM", alice, fixture.player("Bob"));
        running("AS-FFA", carol, fixture.player("Dave"));

        assertTrue(alice.simulateDamage(5, carol).isCancelled());
        assertTrue(carol.simulateDamage(5, alice).isCancelled());
    }

    @Test
    void playersOutsideGamesAreLeftAlone() {
        init(PvpConfig.defaults());
        PlayerMock dave = fixture.player("Dave");
        PlayerMock erin = fixture.player("Erin");

        assertFalse(erin.simulateDamage(5, dave).isCancelled());
        assertFalse(fire(dave, DamageCause.FALL).isCancelled());
    }

    @Test
    void spawnProtectionCancelsDamageForItsSeconds() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running("AS-PROBE", alice, bob);
        bob.simulateDamage(100, alice);
        bob.respawn();
        server.getScheduler().performOneTick();

        assertTrue(bob.simulateDamage(5, alice).isCancelled());
        fixture.runSeconds(6);
        assertFalse(bob.simulateDamage(5, alice).isCancelled());
    }

    @Test
    void gameEntitiesOnlyFightWithTheirOwnGame() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock dave = fixture.player("Dave");
        Game game = running("AS-FFA", alice, fixture.player("Bob"));
        LivingEntityMock zombie = (LivingEntityMock) server.getEntity(game.spawnEntity("ZOMBIE", game.arena().spawn()));

        assertFalse(zombie.simulateDamage(1, alice).isCancelled());
        assertTrue(zombie.simulateDamage(1, dave).isCancelled());
        assertFalse(alice.simulateDamage(1, zombie).isCancelled());
        assertTrue(dave.simulateDamage(1, zombie).isCancelled());
    }

    @Test
    void aWaitingGamesEntitiesCannotBeHurt() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-FFA");
        fixture.join(game, alice);
        LivingEntityMock zombie = (LivingEntityMock) server.getEntity(game.spawnEntity("ZOMBIE", game.arena().spawn()));

        assertTrue(zombie.simulateDamage(1, alice).isCancelled());
    }

    @Test
    void voidAndKillDamageAlwaysGoesThrough() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        fixture.join(fixture.open("AS-TDM"), alice);

        assertFalse(fire(alice, DamageCause.VOID).isCancelled());
        assertFalse(fire(alice, DamageCause.KILL).isCancelled());

        PlayerMock carol = fixture.player("Carol");
        Game game = running("AS-FFA", fixture.player("Bob"), fixture.player("Dave"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);

        assertFalse(fire(carol, DamageCause.VOID).isCancelled());
        assertFalse(fire(carol, DamageCause.KILL).isCancelled());
    }

    @Test
    void nothingHurtsWhileEnding() {
        init(PvpConfig.defaults());
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        Game game = running("AS-TDM", alice, bob);
        fixture.services().games().stop(game);
        assertEquals(Phase.ENDING, game.phase());

        assertTrue(bob.simulateDamage(5, alice).isCancelled());
        assertTrue(fire(alice, DamageCause.FALL).isCancelled());
    }

    @Test
    void anEliminatedPlayerNeitherTakesNorDealsDamage() {
        init(PvpConfig.defaults());
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"), fixture.player("Carol"));
        Game game = running("AS-TDM", players.toArray(PlayerMock[]::new));
        PlayerMock loner = players.stream().filter(p -> teamSize(game, p) == 1).findFirst().orElseThrow();
        PlayerMock victim = players.stream().filter(p -> teamSize(game, p) == 2).findFirst().orElseThrow();

        victim.simulateDamage(100, loner);
        victim.respawn();
        server.getScheduler().performOneTick();

        assertEquals(Phase.RUNNING, game.phase());
        assertTrue(victim.simulateDamage(5, loner).isCancelled());
        assertTrue(loner.simulateDamage(5, victim).isCancelled());
    }

    private void init(PvpConfig config) {
        Arena probe = TestArenas.ffa("AS-PROBE", 200, 4).withMode("probe");
        fixture = PaperFixture.create(server, dir, config, List.of(new ProbeMode("probe", 5, true)),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4), probe);
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
    }

    private Game running(String arena, PlayerMock... players) {
        Game game = fixture.open(arena);
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    private List<PlayerMock> players(int count) {
        List<PlayerMock> players = new ArrayList<>();
        IntStream.rangeClosed(1, count).forEach(i -> players.add(fixture.player("Player" + i)));
        return players;
    }

    private Arrow arrow(Player shooter) {
        Location at = shooter.getLocation();
        Arrow arrow = at.getWorld().spawn(at, Arrow.class);
        arrow.setShooter(shooter);
        return arrow;
    }

    /** Environmental damage; Paper only offers deprecated constructors for it, so tests build it this way. */
    @SuppressWarnings("deprecation")
    private EntityDamageEvent fire(Player victim, DamageCause cause) {
        EntityDamageEvent event = new EntityDamageEvent(victim, cause, DamageSource.builder(DamageType.GENERIC).build(), 2);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private static TeamSpec color(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().color().orElseThrow();
    }

    private static int teamSize(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().team().orElseThrow().members().size();
    }
}
