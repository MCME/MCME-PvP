package com.mcmiddleearth.pvp.stats;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameListeners;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsRecorderTest {

    /** Stats kept in memory, saved at once. */
    private static final class MemoryStats implements StatsStore {
        private final Map<UUID, PlayerStats> saved = new HashMap<>();

        @Override
        public Optional<PlayerStats> load(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public void save(PlayerStats stats) {
            saved.put(stats.id(), stats);
        }
    }

    /** A store whose save always fails, to prove a failing stats side doesn't cost the history. */
    private static final class BrokenSaveStats implements StatsStore {
        @Override
        public Optional<PlayerStats> load(UUID id) {
            return Optional.empty();
        }

        @Override
        public void save(PlayerStats stats) {
            throw new IllegalStateException("disk is full");
        }
    }

    @TempDir
    Path dir;

    @Test
    void participantsGetTheirGameAndSpectatorsASpectatedGame() {
        MemoryStats store = new MemoryStats();
        List<MatchRecord> history = new ArrayList<>();
        RecordingLog log = new RecordingLog();
        LegacyStatsImporter legacy = new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
        StatsService stats = new StatsService(store, legacy);
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new StatsRecorder(stats, history::add));
        List<FakeGameContext> contexts = new ArrayList<>();
        ArenaRegistry arenas = TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2));
        GameManager games = new GameManager(arenas, TestArenas.modes(), PvpConfig.defaults(),
                FakeGameContext.factory(contexts), listeners, player -> 0.0);
        Game game = games.open("AS-TDM", Map.of(), "Staff");
        games.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        games.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        games.spectate(TestArenas.player("Carol"), game);
        games.startCountdown(game);
        contexts.get(0).advanceSeconds(7);

        game.handleDeath(TestArenas.player("Alice").id(), TestArenas.player("Bob").id());

        assertEquals(1, history.size());
        PlayerStats alice = store.load(TestArenas.player("Alice").id()).orElseThrow();
        PlayerStats bob = store.load(TestArenas.player("Bob").id()).orElseThrow();
        PlayerStats carol = store.load(TestArenas.player("Carol").id()).orElseThrow();
        assertEquals(List.of(1, 0, 1, 0, 1, 0), List.of(alice.gamesPlayed(), alice.gamesWon(), alice.gamesLost(),
                alice.kills(), alice.deaths(), alice.gamesSpectated()));
        assertEquals(List.of(1, 1, 0, 1, 0, 0), List.of(bob.gamesPlayed(), bob.gamesWon(), bob.gamesLost(),
                bob.kills(), bob.deaths(), bob.gamesSpectated()));
        assertEquals(List.of(0, 1), List.of(carol.gamesPlayed(), carol.gamesSpectated()));
        assertEquals("Carol", carol.name());
    }

    @Test
    void aParticipantWhoCameBackToWatchCountsAsPlayedNotAlsoAsSpectated() {
        MemoryStats store = new MemoryStats();
        List<MatchRecord> history = new ArrayList<>();
        RecordingLog log = new RecordingLog();
        LegacyStatsImporter legacy = new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
        StatsService stats = new StatsService(store, legacy);
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new StatsRecorder(stats, history::add));
        List<FakeGameContext> contexts = new ArrayList<>();
        ArenaRegistry arenas = TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2));
        GameManager games = new GameManager(arenas, TestArenas.modes(), PvpConfig.defaults(),
                FakeGameContext.factory(contexts), listeners, player -> 0.0);
        Game game = games.open("AS-TDM", Map.of(), "Staff");
        for (String name : List.of("Alice", "Bob", "Carol", "Dave")) {
            games.join(JoinUnit.solo(TestArenas.player(name)), game);
        }
        games.startCountdown(game);
        contexts.get(0).advanceSeconds(7);
        UUID alice = TestArenas.player("Alice").id();
        UUID bob = TestArenas.player("Bob").id();

        // Alice (red) dies, leaves, and comes back: she is out, so she can only watch, and shares red's loss.
        game.handleDeath(alice, bob);
        games.leave(alice, LeaveReason.LEFT);
        games.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        game.handleDeath(TestArenas.player("Carol").id(), bob);

        MatchRecord.Participant entry = history.getFirst().participants().stream()
                .filter(participant -> participant.uuid().equals(alice.toString())).findFirst().orElseThrow();
        assertEquals(MatchRecord.LOST, entry.result());
        PlayerStats stored = store.load(alice).orElseThrow();
        assertEquals(List.of(1, 0, 1, 1, 0), List.of(stored.gamesPlayed(), stored.gamesWon(), stored.gamesLost(),
                stored.deaths(), stored.gamesSpectated()));
    }

    @Test
    void anEliminatedPlayerWhoLeftCountsHerTeamsLaterWin() {
        MemoryStats store = new MemoryStats();
        RecordingLog log = new RecordingLog();
        LegacyStatsImporter legacy = new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
        StatsService stats = new StatsService(store, legacy);
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new StatsRecorder(stats, record -> { }));
        List<FakeGameContext> contexts = new ArrayList<>();
        ArenaRegistry arenas = TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2));
        GameManager games = new GameManager(arenas, TestArenas.modes(), PvpConfig.defaults(),
                FakeGameContext.factory(contexts), listeners, player -> 0.0);
        Game game = games.open("AS-TDM", Map.of(), "Staff");
        for (String name : List.of("Alice", "Bob", "Carol", "Dave")) {
            games.join(JoinUnit.solo(TestArenas.player(name)), game);
        }
        games.startCountdown(game);
        contexts.get(0).advanceSeconds(7);
        UUID alice = TestArenas.player("Alice").id();
        UUID carol = TestArenas.player("Carol").id();

        // Alice (red) dies and leaves; Carol (red) then wins it for her team.
        game.handleDeath(alice, TestArenas.player("Bob").id());
        games.leave(alice, LeaveReason.LEFT);
        game.handleDeath(TestArenas.player("Bob").id(), carol);
        game.handleDeath(TestArenas.player("Dave").id(), carol);

        PlayerStats stored = store.load(alice).orElseThrow();
        assertEquals(List.of(1, 1, 0, 1), List.of(stored.gamesPlayed(), stored.gamesWon(), stored.gamesLost(),
                stored.deaths()));
    }

    @Test
    void aFailingHistoryStillRecordsTheStats() {
        MemoryStats store = new MemoryStats();
        RecordingLog log = new RecordingLog();
        LegacyStatsImporter legacy = new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
        StatsService stats = new StatsService(store, legacy);
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new StatsRecorder(stats, record -> {
            throw new IllegalStateException("the history is broken");
        }));
        List<FakeGameContext> contexts = new ArrayList<>();
        ArenaRegistry arenas = TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2));
        GameManager games = new GameManager(arenas, TestArenas.modes(), PvpConfig.defaults(),
                FakeGameContext.factory(contexts), listeners, player -> 0.0);
        Game game = games.open("AS-TDM", Map.of(), "Staff");
        games.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        games.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        games.startCountdown(game);
        contexts.get(0).advanceSeconds(7);

        game.handleDeath(TestArenas.player("Alice").id(), TestArenas.player("Bob").id());

        assertEquals(1, store.load(TestArenas.player("Alice").id()).orElseThrow().gamesLost());
        assertEquals(1, store.load(TestArenas.player("Bob").id()).orElseThrow().gamesWon());
        List<String> severe = log.messages(Level.SEVERE);
        assertEquals(1, severe.size(), severe.toString());
        assertTrue(severe.getFirst().contains("gameEnded"), severe.getFirst());
    }

    @Test
    void aFailingStatsStillRecordsTheHistory() {
        BrokenSaveStats store = new BrokenSaveStats();
        List<MatchRecord> history = new ArrayList<>();
        RecordingLog log = new RecordingLog();
        LegacyStatsImporter legacy = new LegacyStatsImporter(dir.resolve("stats"), Json.pretty(), log.logger());
        StatsService stats = new StatsService(store, legacy);
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new StatsRecorder(stats, history::add));
        List<FakeGameContext> contexts = new ArrayList<>();
        ArenaRegistry arenas = TestArenas.registry(dir, TestArenas.tdm("AS-TDM", 0, 2));
        GameManager games = new GameManager(arenas, TestArenas.modes(), PvpConfig.defaults(),
                FakeGameContext.factory(contexts), listeners, player -> 0.0);
        Game game = games.open("AS-TDM", Map.of(), "Staff");
        games.join(JoinUnit.solo(TestArenas.player("Alice")), game);
        games.join(JoinUnit.solo(TestArenas.player("Bob")), game);
        games.startCountdown(game);
        contexts.get(0).advanceSeconds(7);

        game.handleDeath(TestArenas.player("Alice").id(), TestArenas.player("Bob").id());

        assertEquals(1, history.size());
        List<String> severe = log.messages(Level.SEVERE);
        assertEquals(1, severe.size(), severe.toString());
        assertTrue(severe.getFirst().contains("gameEnded"), severe.getFirst());
    }
}
