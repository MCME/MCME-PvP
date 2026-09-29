package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.Region;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PluginFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.Location;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Team Deathmatch and a Free For All run at the same time and never see each other. */
class ParallelGamesTest {

    private final Arena tdmArena = TestArenas.tdm("AS-TDM", 0, 2);
    private final Arena ffaArena = TestArenas.ffa("AS-FFA", 100, 4);

    private ServerMock server;
    private PvpPlugin plugin;
    private PlayerMock alice;
    private PlayerMock bob;
    private PlayerMock carol;
    private PlayerMock dave;
    private Game tdm;
    private Game ffa;

    @BeforeEach
    void bothGamesRunning() {
        server = MockBukkit.mock();
        plugin = PluginFixture.load(server, tdmArena, ffaArena);
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        carol = server.addPlayer("Carol");
        dave = server.addPlayer("Dave");
        staff.performCommand("pvpadmin start AS-TDM teamCount=2");
        staff.performCommand("pvpadmin start AS-FFA timeLimit=5");
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join AS-TDM");
        carol.performCommand("pvp join AS-FFA");
        dave.performCommand("pvp join AS-FFA");
        staff.performCommand("pvpadmin countdown AS-TDM");
        staff.performCommand("pvpadmin countdown AS-FFA");
        tdm = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        ffa = plugin.services().games().gameOn("AS-FFA").orElseThrow();
        PluginFixture.runUntil(server, tdm, Phase.RUNNING, 10);
        PluginFixture.runUntil(server, ffa, Phase.RUNNING, 10);
        for (PlayerMock player : List.of(alice, bob, carol, dave)) {
            PaperFixture.chat(player);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } finally {
            CapturingClickCallbackProvider.reset();
        }
    }

    @Test
    void eachPlayerIsInExactlyOneGame() {
        assertEquals(List.of(alice.getUniqueId(), bob.getUniqueId()), ids(tdm.players()));
        assertEquals(List.of(carol.getUniqueId(), dave.getUniqueId()), ids(ffa.players()));
        assertSame(tdm, plugin.services().games().gameOf(alice.getUniqueId()).orElseThrow());
        assertSame(ffa, plugin.services().games().gameOf(carol.getUniqueId()).orElseThrow());
    }

    @Test
    void eachGameHasItsOwnScoreboard() {
        Scoreboard tdmBoard = alice.getScoreboard();
        Scoreboard ffaBoard = carol.getScoreboard();

        assertNotSame(tdmBoard, ffaBoard);
        assertSame(tdmBoard, bob.getScoreboard());
        assertSame(ffaBoard, dave.getScoreboard());
        assertNull(ffaBoard.getEntryTeam(alice.getName()));
        assertNull(tdmBoard.getEntryTeam(carol.getName()));
    }

    @Test
    void chatAndDeathsStayInTheirGame() {
        bob.simulateDamage(100, alice);

        assertTrue(PaperFixture.chat(alice).contains("Bob was killed by Alice"));
        assertEquals(List.of(), PaperFixture.chat(carol));
        assertEquals(List.of(), PaperFixture.chat(dave));
        assertEquals(Phase.ENDING, tdm.phase());
        assertEquals(Phase.RUNNING, ffa.phase());
        assertEquals(0, session(ffa, carol).deaths() + session(ffa, dave).deaths());
        assertEquals(1, session(tdm, bob).deaths());
        assertEquals(1, session(tdm, alice).kills());
    }

    @Test
    void closingOneGameDoesNotDisturbTheOther() {
        Scoreboard ffaBoard = carol.getScoreboard();
        bob.simulateDamage(100, alice);

        PluginFixture.runUntil(server, tdm, Phase.CLOSED, plugin.services().config().endingSeconds() + 2);
        int clockAtClose = ffa.secondsLeft();
        server.getScheduler().performTicks(40);

        assertEquals(Phase.RUNNING, ffa.phase());
        assertTrue(ffa.secondsLeft() < clockAtClose); // the FFA's heartbeat survived the TDM's cancelAll/dispose
        assertNotSame(server.getScoreboardManager().getMainScoreboard(), ffaBoard);
        assertSame(ffaBoard, carol.getScoreboard());
        assertSame(ffaBoard, dave.getScoreboard());
        assertTrue(PaperFixture.chat(carol).stream().noneMatch(line -> line.contains("Bob") || line.contains("AS-TDM")));
        assertSame(server.getScoreboardManager().getMainScoreboard(), alice.getScoreboard());
    }

    @Test
    void playersOfDifferentGamesCannotHurtEachOther() {
        assertTrue(alice.simulateDamage(5, carol).isCancelled());
        assertTrue(carol.simulateDamage(5, alice).isCancelled());
        assertFalse(dave.simulateDamage(5, carol).isCancelled());
    }

    @Test
    void eachGameKeepsItsPlayersInItsOwnRegion() {
        Location tdmInside = middle(tdmArena);
        Location ffaInside = middle(ffaArena);
        move(alice, tdmInside);
        move(carol, ffaInside);

        assertEquals(tdmInside, move(alice, tdmInside));
        assertEquals(tdmInside.getX(), move(alice, ffaInside).getX(), 1e-9);
        assertEquals(ffaInside.getX(), move(carol, tdmInside).getX(), 1e-9);
    }

    private Location middle(Arena arena) {
        Region.Bounds bounds = arena.region().bounds();
        return new Location(server.getWorld(arena.world()), (bounds.minX() + bounds.maxX()) / 2.0 + 0.5,
                arena.spawn().y(), (bounds.minZ() + bounds.maxZ()) / 2.0 + 0.5);
    }

    private static Location move(PlayerMock player, Location to) {
        return new PlayerSimulation(player).simulatePlayerMove(to).getTo();
    }

    private static PlayerSession session(Game game, PlayerMock player) {
        return game.session(player.getUniqueId()).orElseThrow();
    }

    private static List<UUID> ids(List<PlayerSession> sessions) {
        return sessions.stream().map(PlayerSession::id).toList();
    }
}
