package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.arena.Region;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The countdown freeze and the arena's region, for the game's players and spectators only. */
class GameEventRouterMovementTest {

    private static final String BOUNDARY = "You aren't allowed to leave the map!";

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Arena arena;
    private Arena spawnOutside;
    private Arena unbounded;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        arena = TestArenas.tdm("AS-TDM", 0, 2);
        spawnOutside = spawnOutside(TestArenas.tdm("AS-OUT", 200, 2));
        unbounded = TestArenas.withoutRegion(TestArenas.tdm("AS-FREE", 400, 2));
        fixture = PaperFixture.create(server, dir, arena, spawnOutside, unbounded);
        fixture.register(new GameEventRouter(fixture.services(), fixture.plugin()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void frozenPlayersMayLookAroundButNotMove() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open("AS-TDM");
        fixture.join(game, alice, fixture.player("Bob"));
        fixture.services().games().startCountdown(game);
        Location from = alice.getLocation();
        Location to = from.clone().add(2, 1, -1);
        to.setYaw(from.getYaw() + 45);
        to.setPitch(10);

        PlayerMoveEvent move = move(alice, to);

        assertEquals(from.getX(), move.getTo().getX(), 1e-9);
        assertEquals(from.getY(), move.getTo().getY(), 1e-9);
        assertEquals(from.getZ(), move.getTo().getZ(), 1e-9);
        assertEquals(to.getYaw(), move.getTo().getYaw());
        assertEquals(10f, move.getTo().getPitch());
    }

    @Test
    void theFreezeEndsWhenTheFightStarts() {
        PlayerMock alice = fixture.player("Alice");
        running(alice, fixture.player("Bob"));
        Location to = inside();

        assertEquals(to, move(alice, to).getTo());
    }

    @Test
    void leavingTheRegionSendsThePlayerBackWithAWarningAtMostEvery3Seconds() {
        PlayerMock alice = fixture.player("Alice");
        running(alice, fixture.player("Bob"));
        Location inside = inside();
        move(alice, inside);
        PaperFixture.chat(alice);

        PlayerMoveEvent escape = move(alice, outside());

        assertSamePosition(inside, escape.getTo());
        assertEquals(List.of(BOUNDARY), PaperFixture.chat(alice));

        assertSamePosition(inside, move(alice, outside()).getTo());
        assertEquals(List.of(), PaperFixture.chat(alice));

        fixture.runSeconds(3);
        move(alice, outside());
        assertEquals(List.of(BOUNDARY), PaperFixture.chat(alice));
    }

    @Test
    void spectatorsStayInsideToo() {
        PlayerMock carol = fixture.player("Carol");
        Game game = running(fixture.player("Alice"), fixture.player("Bob"));
        fixture.services().games().spectate(PaperFixture.ref(carol), game);
        Location start = carol.getLocation();

        assertSamePosition(start, move(carol, outside()).getTo());
    }

    @Test
    void playersOutsideGamesMoveFreely() {
        running(fixture.player("Alice"), fixture.player("Bob"));
        PlayerMock dave = fixture.player("Dave");
        dave.teleport(inside());
        Location outside = outside();

        assertEquals(outside, move(dave, outside).getTo());
    }

    @Test
    void aPlayerTheGamePutsOnASpawnOutsideTheRegionWalksInFromThere() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"),
                fixture.player("Carol"), fixture.player("Dave"));
        Game game = fixture.open("AS-OUT");
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);
        PlayerMock alice = players.getFirst();
        move(alice, inside(spawnOutside));
        alice.setHealth(0);

        alice.respawn();

        assertEquals(Phase.RUNNING, game.phase());
        assertSamePosition(Locations.location(spawnOutside.spawn()).orElseThrow(), alice.getLocation());
        Location further = alice.getLocation().clone().add(1, 0, 0);
        assertEquals(further, move(alice, further).getTo());
    }

    @Test
    void aRespawnedPlayerIsPushedBackToWhereTheyNowStandNotWhereTheyDied() {
        List<PlayerMock> players = List.of(fixture.player("Alice"), fixture.player("Bob"),
                fixture.player("Carol"), fixture.player("Dave"));
        Game game = fixture.open("AS-TDM");
        fixture.join(game, players.toArray(PlayerMock[]::new));
        fixture.startAndRun(game);
        PlayerMock alice = players.getFirst();
        move(alice, inside());
        alice.setHealth(0);

        alice.respawn();

        assertEquals(Phase.RUNNING, game.phase());
        Location spawn = Locations.location(arena.spawn()).orElseThrow();
        assertSamePosition(spawn, alice.getLocation());
        assertSamePosition(spawn, move(alice, outside()).getTo());
    }

    @Test
    void theWarningThrottleIsPerPlayer() {
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        running(alice, bob);
        move(alice, inside());
        move(bob, inside());
        PaperFixture.chat(alice);
        PaperFixture.chat(bob);

        move(alice, outside());
        assertEquals(List.of(BOUNDARY), PaperFixture.chat(alice));

        move(bob, outside());
        assertEquals(List.of(BOUNDARY), PaperFixture.chat(bob));
    }

    @Test
    void arenasWithoutARegionHaveNoBoundary() {
        PlayerMock alice = fixture.player("Alice");
        Game game = fixture.open(unbounded.id());
        fixture.join(game, alice, fixture.player("Bob"));
        fixture.startAndRun(game);
        Location farAway = alice.getLocation().clone().add(500, 0, 500);

        assertEquals(farAway, move(alice, farAway).getTo());
    }

    private Game running(PlayerMock... players) {
        Game game = fixture.open("AS-TDM");
        fixture.join(game, players);
        fixture.startAndRun(game);
        return game;
    }

    /** The middle of the arena's region, at the height of its spawn. */
    private Location inside() {
        return inside(arena);
    }

    private Location inside(Arena of) {
        Region.Bounds bounds = of.region().bounds();
        return new Location(server.getWorld(of.world()), (bounds.minX() + bounds.maxX()) / 2.0 + 0.5,
                of.spawn().y(), (bounds.minZ() + bounds.maxZ()) / 2.0 + 0.5);
    }

    /** The arena with its spawn eleven blocks east of its region, like a viewing platform outside it. */
    private static Arena spawnOutside(Arena arena) {
        NamedPoint spawn = NamedPoint.of(arena.spawn().world(), arena.region().bounds().maxX() + 11.5,
                arena.spawn().y(), arena.spawn().z());
        return new Arena(arena.schemaVersion(), arena.id(), arena.modeId(), arena.unassignedReason(), arena.title(),
                arena.maxPlayers(), arena.resourcePackUrl(), spawn, arena.points(), arena.teamSpawns(), arena.region(),
                arena.imported());
    }

    /** Ten blocks east of the region. */
    private Location outside() {
        Location outside = inside();
        outside.setX(arena.region().bounds().maxX() + 10.5);
        return outside;
    }

    private static PlayerMoveEvent move(PlayerMock player, Location to) {
        return new PlayerSimulation(player).simulatePlayerMove(to);
    }

    private static void assertSamePosition(Location expected, Location actual) {
        assertEquals(expected.getX(), actual.getX(), 1e-9);
        assertEquals(expected.getY(), actual.getY(), 1e-9);
        assertEquals(expected.getZ(), actual.getZ(), 1e-9);
    }
}
