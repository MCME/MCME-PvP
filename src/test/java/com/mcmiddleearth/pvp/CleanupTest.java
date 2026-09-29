package com.mcmiddleearth.pvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PluginFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A closed game leaves nothing behind: no task, no entity, and its players back in the lobby. */
class CleanupTest {

    private ServerMock server;
    private PvpPlugin plugin;
    private PlayerMock staff;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = PluginFixture.load(server, TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 4));
        staff = server.addPlayer("Staff");
        staff.setOp(true);
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
    void aStoppedRunningGameCancelsItsTasksRemovesItsEntitiesAndSendsEveryoneToTheLobby() {
        Set<Integer> tasksBefore = taskIds();
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        PlayerMock carol = server.addPlayer("Carol");
        console("pvpadmin start AS-FFA");
        alice.performCommand("pvp join AS-FFA");
        bob.performCommand("pvp join AS-FFA");
        carol.performCommand("pvp spectate AS-FFA");
        staff.performCommand("pvpadmin countdown AS-FFA");
        Game game = plugin.services().games().gameOn("AS-FFA").orElseThrow();
        PluginFixture.runUntil(server, game, Phase.RUNNING, 10);
        UUID first = game.spawnEntity("ZOMBIE", game.arena().spawn());
        UUID second = game.spawnEntity("ARMOR_STAND", game.arena().spawn());
        Entity zombie = server.getEntity(first);
        PaperFixture.chat(alice);

        staff.performCommand("pvpadmin stop AS-FFA");
        PluginFixture.runUntil(server, game, Phase.CLOSED, plugin.services().config().endingSeconds() + 2);

        assertEquals(tasksBefore, taskIds());
        assertNull(server.getEntity(first));
        assertNull(server.getEntity(second));
        assertFalse(zombie.isValid());
        assertTrue(plugin.services().games().games().isEmpty());
        for (PlayerMock player : List.of(alice, bob, carol)) {
            assertInLobby(player);
        }

        Outcome outcome = game.outcome().orElseThrow();
        assertEquals(EndReason.STOPPED, outcome.reason());
        assertNull(outcome.winningTeam());
        assertNull(outcome.winningPlayer());
        assertTrue(PaperFixture.chat(alice).stream().anyMatch(line -> line.contains("Game over — Free For All on AS-FFA")));
    }

    @Test
    void aGameStoppedWhileWaitingClosesAtOnce() {
        Set<Integer> tasksBefore = taskIds();
        PlayerMock alice = server.addPlayer("Alice");
        console("pvpadmin start AS-TDM");
        alice.performCommand("pvp join");
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        UUID zombie = game.spawnEntity("ZOMBIE", game.arena().spawn());
        assertNotNull(server.getEntity(zombie));

        staff.performCommand("pvpadmin stop AS-TDM");

        assertEquals(Phase.CLOSED, game.phase());
        assertEquals(tasksBefore, taskIds());
        assertNull(server.getEntity(zombie));
        assertInLobby(alice);
    }

    @Test
    void disablingThePluginClosesRunningGames() throws IOException {
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");
        console("pvpadmin start AS-TDM");
        alice.performCommand("pvp join");
        bob.performCommand("pvp join");
        staff.performCommand("pvpadmin countdown AS-TDM");
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        PluginFixture.runUntil(server, game, Phase.RUNNING, 10);
        UUID zombie = game.spawnEntity("ZOMBIE", game.arena().spawn());

        server.getPluginManager().disablePlugin(plugin);

        assertEquals(Phase.CLOSED, game.phase());
        assertNull(server.getEntity(zombie));
        assertInLobby(alice);
        assertInLobby(bob);

        Outcome outcome = game.outcome().orElseThrow();
        assertEquals(EndReason.SHUTDOWN, outcome.reason());
        assertNull(outcome.winningTeam());
        assertNull(outcome.winningPlayer());
        JsonNode record = Json.compact().readTree(oneMatchLine());
        assertEquals("shutdown", record.get("endReason").asText());
        assertTrue(record.get("winner").isNull());
    }

    private void console(String command) {
        server.dispatchCommand(server.getConsoleSender(), command);
    }

    private Set<Integer> taskIds() {
        return server.getScheduler().getPendingTasks().stream().map(BukkitTask::getTaskId).collect(Collectors.toSet());
    }

    /** onDisable drains the writer before returning, so the record is already on disk; no poll needed. */
    private String oneMatchLine() throws IOException {
        Path matches = plugin.getDataFolder().toPath().resolve("matches");
        try (Stream<Path> files = Files.list(matches)) {
            List<Path> jsonl = files.filter(file -> file.toString().endsWith(".jsonl")).toList();
            assertEquals(1, jsonl.size(), jsonl::toString);
            List<String> lines = Files.readAllLines(jsonl.getFirst());
            assertEquals(1, lines.size(), lines::toString);
            return lines.getFirst();
        }
    }

    private void assertInLobby(PlayerMock player) {
        Location at = player.getLocation();
        NamedPoint lobby = plugin.services().config().lobby();
        assertEquals(lobby.world(), at.getWorld().getName(), player.getName());
        assertEquals(lobby.x(), at.getX(), 1e-9, player.getName());
        assertEquals(lobby.z(), at.getZ(), 1e-9, player.getName());
        assertSame(server.getScoreboardManager().getMainScoreboard(), player.getScoreboard(), player.getName());
        assertEquals(GameMode.ADVENTURE, player.getGameMode(), player.getName());
        assertTrue(player.getInventory().isEmpty(), player.getName());
    }
}
