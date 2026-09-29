package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameContext;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Paper context drives a real game from the engine's GameManager. */
class PaperGameContextTest {

    @TempDir
    Path dir;

    private ServerMock server;
    private PaperFixture fixture;
    private Arena arena;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        arena = TestArenas.tdm("AS-TDM", 0, 2);
        fixture = PaperFixture.create(server, dir, arena);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void eachGameHasItsOwnScoreboardUntilItCloses() {
        Game game = fixture.open("AS-TDM");
        Scoreboard board = board(game);
        assertNotSame(server.getScoreboardManager().getMainScoreboard(), board);

        fixture.services().games().stop(game);

        assertTrue(fixture.contexts().scoreboard(game.id()).isEmpty());
    }

    @Test
    void joiningPlayersStandOnTheArenaSpawnAndSeeTheGameBoard() {
        Game game = fixture.open("AS-TDM");
        PlayerMock alice = fixture.player("Alice");

        fixture.join(game, alice);

        Location spawn = Locations.location(arena.spawn()).orElseThrow();
        assertEquals(spawn.getX(), alice.getLocation().getX(), 1e-9);
        assertEquals(spawn.getY(), alice.getLocation().getY(), 1e-9);
        assertEquals(spawn.getZ(), alice.getLocation().getZ(), 1e-9);
        assertSame(board(game), alice.getScoreboard());
        assertEquals(GameMode.ADVENTURE, alice.getGameMode());
    }

    @Test
    void theCountdownPutsPlayersInColoredTeamsWithDyedKits() {
        Game game = fixture.open("AS-TDM");
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.join(game, alice, bob);

        fixture.services().games().startCountdown(game);

        for (PlayerMock player : List.of(alice, bob)) {
            TeamSpec color = color(game, player);
            assertEquals(color.id(), board(game).getEntryTeam(player.getName()).getName());
            LeatherArmorMeta helmet = (LeatherArmorMeta) player.getInventory().getHelmet().getItemMeta();
            assertEquals(Color.fromRGB(color.armorRgb()), helmet.getColor());
        }
        assertNotEquals(color(game, alice), color(game, bob));
    }

    @Test
    void theHeartbeatRunsTheGameAndCloseCleansEverythingUp() {
        int tasksBefore = server.getScheduler().getPendingTasks().size();
        Game game = fixture.open("AS-TDM");
        PlayerMock alice = fixture.player("Alice");
        PlayerMock bob = fixture.player("Bob");
        fixture.join(game, alice, bob);
        fixture.startAndRun(game);

        fixture.services().games().stop(game);
        fixture.runUntil(game, Phase.CLOSED, fixture.services().config().endingSeconds() + 2);

        assertTrue(fixture.contexts().scoreboard(game.id()).isEmpty());
        assertEquals(tasksBefore, server.getScheduler().getPendingTasks().size());
        for (PlayerMock player : List.of(alice, bob)) {
            assertSame(server.getScoreboardManager().getMainScoreboard(), player.getScoreboard());
            assertEquals(344.47, player.getLocation().getX(), 1e-9);
            assertTrue(player.getInventory().isEmpty());
        }
    }

    @Test
    void gameEntitiesAreSpawnedInTheWorldAndRemovedAtClose() {
        Game game = fixture.open("AS-TDM");

        UUID zombie = game.spawnEntity("ZOMBIE", arena.spawn());

        Entity entity = server.getEntity(zombie);
        assertNotNull(entity);
        assertEquals(EntityType.ZOMBIE, entity.getType());

        fixture.services().games().stop(game);

        assertNull(server.getEntity(zombie));
        assertFalse(entity.isValid());
    }

    @Test
    void disposingAContextCancelsTheTasksItStillHas() {
        int tasksBefore = server.getScheduler().getPendingTasks().size();
        GameContext context = fixture.contexts().create(GameId.random(), arena);
        context.tasks().every(20, () -> { });
        context.tasks().after(40, () -> { });

        context.dispose();

        assertEquals(0, context.tasks().activeCount());
        assertEquals(tasksBefore, server.getScheduler().getPendingTasks().size());
    }

    @Test
    void anArenaWhoseWorldIsntLoadedCannotOpen() {
        Arena elsewhere = TestArenas.inWorld(TestArenas.tdm("AS-ELSEWHERE", 100, 2), "unloaded");
        GameId id = GameId.random();

        PvpException e = assertThrows(PvpException.class, () -> fixture.contexts().create(id, elsewhere));

        assertEquals(Msg.ERROR_ARENA_UNASSIGNED, e.messageKey());
        assertTrue(e.args().get("reason").contains("unloaded"));
        assertTrue(fixture.contexts().scoreboard(id).isEmpty());
    }

    private Scoreboard board(Game game) {
        return fixture.contexts().scoreboard(game.id()).orElseThrow().board();
    }

    private static TeamSpec color(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow().color().orElseThrow();
    }
}
