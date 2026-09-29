package com.mcmiddleearth.pvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.testing.CapturingClickCallbackProvider;
import com.mcmiddleearth.pvp.testing.PaperFixture;
import com.mcmiddleearth.pvp.testing.PluginFixture;
import com.mcmiddleearth.pvp.testing.TestArenas;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One Team Deathmatch from /pvpadmin start to the match record, through commands and events only. */
class TdmRoundTest {

    private ServerMock server;
    private PvpPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = PluginFixture.load(server, TestArenas.tdm("AS-TDM", 0, 2));
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
    void aFullRoundEndsInAMatchRecordAndUpdatedStats() throws Exception {
        PlayerMock staff = server.addPlayer("Staff");
        staff.setOp(true);
        PlayerMock alice = server.addPlayer("Alice");
        PlayerMock bob = server.addPlayer("Bob");

        staff.performCommand("pvpadmin start AS-TDM teamCount=2");
        alice.performCommand("pvp join AS-TDM");
        bob.performCommand("pvp join");
        staff.performCommand("pvpadmin countdown AS-TDM");
        Game game = plugin.services().games().gameOn("AS-TDM").orElseThrow();
        PluginFixture.runUntil(server, game, Phase.RUNNING, 10);
        String aliceTeam = game.session(alice.getUniqueId()).orElseThrow().team().orElseThrow().id();
        PaperFixture.chat(alice);

        bob.simulateDamage(100, alice);
        bob.respawn();

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(aliceTeam, game.outcome().orElseThrow().winningTeam());
        List<String> seen = PaperFixture.chat(alice);
        assertTrue(seen.contains("Bob was killed by Alice"), seen.toString());
        assertTrue(seen.contains("Game over — Team Deathmatch on AS-TDM"), seen.toString());

        PluginFixture.runUntil(server, game, Phase.CLOSED, plugin.services().config().endingSeconds() + 2);

        JsonNode record = Json.compact().readTree(awaitMatchLine());
        assertEquals("AS-TDM", record.get("arena").asText());
        assertEquals("tdm", record.get("mode").asText());
        assertEquals(aliceTeam, record.get("winner").asText());
        assertEquals("win_condition", record.get("endReason").asText());
        assertEquals(2, record.get("participants").size());

        PlayerStats aliceStats = plugin.services().stats().find(alice.getUniqueId()).orElseThrow();
        PlayerStats bobStats = plugin.services().stats().find(bob.getUniqueId()).orElseThrow();
        assertEquals(List.of(1, 1, 0, 1), List.of(aliceStats.gamesPlayed(), aliceStats.gamesWon(), aliceStats.gamesLost(),
                aliceStats.kills()));
        assertEquals(List.of(1, 0, 1, 1), List.of(bobStats.gamesPlayed(), bobStats.gamesWon(), bobStats.gamesLost(),
                bobStats.deaths()));
    }

    /**
     * The plugin's writer runs on its own thread, so the match file appears shortly after the game ended. A
     * complete write is one line, ending with its own {@code \n}; a concurrent write can make the file briefly
     * unreadable, so an {@link IOException} just means try again.
     */
    private String awaitMatchLine() throws InterruptedException {
        Path matches = plugin.getDataFolder().toPath().resolve("matches");
        List<Path> jsonl = List.of();
        int newlines = -1;
        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                if (Files.isDirectory(matches)) {
                    try (Stream<Path> files = Files.list(matches)) {
                        jsonl = files.filter(file -> file.toString().endsWith(".jsonl")).toList();
                    }
                    if (jsonl.size() == 1) {
                        String content = Files.readString(jsonl.getFirst());
                        newlines = (int) content.chars().filter(c -> c == '\n').count();
                        if (newlines == 1 && content.endsWith("\n")) {
                            return content.substring(0, content.length() - 1);
                        }
                    }
                }
            } catch (IOException e) {
                // a write in progress can make the file briefly unreadable; retry
            }
            Thread.sleep(50);
        }
        throw new AssertionError("no complete match record in " + matches + " after 5 seconds (files: " + jsonl
                + ", newlines seen: " + newlines + ")");
    }
}
