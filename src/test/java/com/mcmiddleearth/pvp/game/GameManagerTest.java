package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.loadout.Kit;
import com.mcmiddleearth.pvp.team.TeamLayout;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.RecordingListener;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import com.mcmiddleearth.pvp.text.Msg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opening games, reserving arenas and keeping every player in at most one game. */
class GameManagerTest {

    private static final TestMode DUEL = new TestMode("duel", TestMode.twoTeams().build());

    @TempDir
    Path dir;

    private static Arena duel(String id, int x0) {
        return TestArenas.tdm(id, x0, 2).withMode("duel");
    }

    private TestEngine engine(Arena... arenas) {
        return new TestEngine(dir, new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(), arenas);
    }

    private static PvpException refused(Runnable action) {
        return assertThrows(PvpException.class, action::run);
    }

    @Test
    void anUnknownArenaCannotBeOpened() {
        TestEngine engine = engine(duel("A", 0));

        PvpException e = refused(() -> engine.open("Nowhere"));

        assertEquals(Msg.ERROR_UNKNOWN_ARENA, e.messageKey());
        assertEquals(Map.of("arena", "Nowhere"), e.args());
    }

    @Test
    void anUnassignedArenaCannotBeOpened() {
        TestEngine engine = engine(duel("A", 0).unassigned("the map has no game mode"), duel("B", 100).withMode("ctf"));

        PvpException unassigned = refused(() -> engine.open("A"));
        PvpException unknownMode = refused(() -> engine.open("B"));

        assertEquals(Msg.ERROR_ARENA_UNASSIGNED, unassigned.messageKey());
        assertEquals(Map.of("arena", "A", "reason", "the map has no game mode"), unassigned.args());
        assertEquals(Map.of("arena", "B", "reason", "unknown mode ctf"), unknownMode.args());
        PvpException settings = refused(() -> engine.games().settingsFor(duel("A", 0).unassigned("x")));
        assertEquals(Msg.ERROR_ARENA_UNASSIGNED, settings.messageKey());
    }

    @Test
    void overlappingArenasBlockEachOther() {
        TestEngine engine = engine(duel("A", 0), duel("B", 10));
        engine.open("A");

        PvpException e = refused(() -> engine.open("B"));

        assertEquals(Msg.ERROR_ARENA_BLOCKED, e.messageKey());
        assertEquals(Map.of("arena", "B", "holder", "A"), e.args());
        assertEquals("A", engine.games().blockerFor(duel("B", 10)).orElseThrow().arena().id());
    }

    @Test
    void anArenaIsHeldByOneGameAtATime() {
        TestEngine engine = engine(duel("A", 0));
        Game first = engine.open("A");

        assertEquals(Msg.ERROR_ARENA_BLOCKED, refused(() -> engine.open("a")).messageKey());

        engine.games().stop(first);
        assertEquals(Phase.WAITING, engine.open("A").phase());
    }

    @Test
    void arenasThatDoNotOverlapRunSideBySide() {
        TestEngine engine = engine(duel("A", 0), duel("B", 20));

        Game a = engine.open("A");
        Game b = engine.open("B");

        assertEquals(List.of(a, b), engine.games().games());
        assertEquals(Optional.of(b), engine.games().gameOn("b"));
        assertEquals(Optional.empty(), engine.games().blockerFor(duel("C", 40)));
    }

    @Test
    void anArenaWithoutARegionReservesItsWholeWorld() {
        Arena wholeWorld = TestArenas.withoutRegion(duel("W", 0));
        TestEngine engine = engine(wholeWorld, duel("Far", 1000), TestArenas.inWorld(duel("Elsewhere", 0), "other"));
        engine.open("W");

        assertEquals(Msg.ERROR_ARENA_BLOCKED, refused(() -> engine.open("Far")).messageKey());
        assertEquals(Phase.WAITING, engine.open("Elsewhere").phase());
    }

    @Test
    void aGameOnARegionBlocksAnArenaWithoutOne() {
        TestEngine engine = engine(duel("Far", 1000), TestArenas.withoutRegion(duel("W", 0)));
        engine.open("Far");

        PvpException e = refused(() -> engine.open("W"));

        assertEquals(Map.of("arena", "W", "holder", "Far"), e.args());
    }

    @Test
    void openingChecksTheSettingsAndTheSpawnPoints() {
        NamedPoint spawn = NamedPoint.of("world", 0, 64, 0);
        Arena oneTeam = new Arena(Arena.SCHEMA_VERSION, "One", "duel", null, "One", 16, null, spawn, Map.of(),
                Map.of("red", List.of(NamedPoint.of("world", 1, 64, 1))), null, null);
        TestEngine engine = engine(duel("A", 0), TestArenas.inWorld(oneTeam, "other"));

        assertEquals(Msg.ERROR_SETTING_UNKNOWN, refused(() -> engine.open("A", Map.of("teamCount", 3))).messageKey());
        PvpException spawns = refused(() -> engine.open("One"));
        assertEquals(Msg.ERROR_ARENA_TEAMS_ONE, spawns.messageKey());
        assertEquals(Map.of("arena", "One", "max", "1"), spawns.args());
        assertEquals(List.of(), engine.games().games());

        assertEquals(4, engine.open("A", Map.of("maxPlayers", 4)).maxPlayers());
    }

    @Test
    void moreTeamsThanTheArenaHasSpawnSetsAreRefusedWithTheArenasMaximum() {
        TestMode teams = new TestMode("teams", TestMode.twoTeams().teams(TeamLayout.configurable(2, 4)).build());
        TestEngine engine = new TestEngine(dir, new ModeRegistry(List.of(teams)), PvpConfig.defaults(),
                TestArenas.tdm("A", 0, 2).withMode("teams"));

        PvpException three = refused(() -> engine.open("A", Map.of("teamCount", 3)));
        PvpException five = refused(() -> engine.open("A", Map.of("TEAMCOUNT", "5")));

        // The mode allows 3 teams, but the arena has spawns for 2: refused with the arena's maximum.
        assertEquals(Msg.ERROR_ARENA_TEAMS, three.messageKey());
        assertEquals(Map.of("arena", "A", "max", "2"), three.args());
        // The mode never allows 5 teams, so that stays a range error.
        assertEquals(Msg.ERROR_SETTING_RANGE, five.messageKey());
        assertEquals(Map.of("setting", "teamCount", "min", "2", "max", "2"), five.args());
        assertEquals(2, engine.open("A", Map.of("teamCount", 2)).teams().size());
    }

    @Test
    void moreTeamsThanTheArenaHasSpawnSetsUsesTheSingularForOneSupportedTeam() {
        TestMode teams = new TestMode("teams", TestMode.twoTeams().teams(TeamLayout.configurable(2, 4)).build());
        TestEngine engine = new TestEngine(dir, new ModeRegistry(List.of(teams)), PvpConfig.defaults(),
                TestArenas.tdm("A", 0, 1).withMode("teams"));

        PvpException two = refused(() -> engine.open("A", Map.of("teamCount", 2)));

        assertEquals(Msg.ERROR_ARENA_TEAMS_ONE, two.messageKey());
        assertEquals(Map.of("arena", "A", "max", "1"), two.args());
    }

    @Test
    void maxPlayersBelowOnePlayerPerTeamIsRefused() {
        TestMode teams = new TestMode("teams", TestMode.twoTeams().teams(TeamLayout.configurable(2, 4)).build());
        TestEngine engine = new TestEngine(dir, new ModeRegistry(List.of(teams)), PvpConfig.defaults(),
                TestArenas.tdm("A", 0, 3).withMode("teams"));

        // Three teams need three players, one per team, but maxPlayers only allows two: the countdown could never
        // start, so staff would be stuck with a game they can only stop.
        PvpException e = refused(() -> engine.open("A", Map.of("teamCount", 3, "maxPlayers", 2)));
        assertEquals(Msg.ERROR_SETTING_RANGE, e.messageKey());
        assertEquals(Map.of("setting", "maxPlayers", "min", "3", "max", "16"), e.args());
        assertEquals(List.of(), engine.games().games());

        assertEquals(3, engine.open("A", Map.of("teamCount", 3, "maxPlayers", 3)).maxPlayers());
    }

    @Test
    void settingsForListsTheModesSettingsForTheArena() {
        TestEngine engine = engine(duel("A", 0));

        List<SettingSpec> specs = engine.games().settingsFor(duel("A", 0));

        assertEquals(List.of(Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN),
                specs.stream().map(SettingSpec::setting).toList());
    }

    @Test
    void aPlayerIsInAtMostOneGame() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        Game a = engine.open("A");
        Game b = engine.open("B");
        engine.join(a, "Alice");

        PvpException join = refused(() -> engine.join(b, "Alice"));
        PvpException spectate = refused(() -> engine.spectate(b, "Alice"));
        PvpException party = refused(() -> engine.party(b, "Bob", "Alice"));

        assertEquals(Msg.ERROR_ALREADY_IN_GAME, join.messageKey());
        assertEquals(Map.of("player", "Alice"), join.args());
        assertEquals(Msg.ERROR_ALREADY_IN_GAME, spectate.messageKey());
        assertEquals(Msg.ERROR_ALREADY_IN_GAME, party.messageKey());
        assertEquals(Optional.empty(), engine.games().gameOf(id("Bob")));
        assertEquals(Optional.of(a), engine.games().gameOf(id("Alice")));
    }

    @Test
    void whenAGameClosesItsPlayersAreFreeAgain() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        Game a = engine.open("A");
        Game b = engine.open("B");
        engine.join(a, "Alice");
        engine.spectate(a, "Carol");

        engine.games().stop(a);

        assertEquals(Optional.empty(), engine.games().gameOf(id("Alice")));
        assertEquals(Optional.empty(), engine.games().gameOf(id("Carol")));
        assertEquals(Msg.ERROR_GAME_ENDED, refused(() -> engine.join(a, "Alice")).messageKey());
        engine.join(b, "Alice");
        assertEquals(Optional.of(b), engine.games().gameOf(id("Alice")));
    }

    @Test
    void soleOpenGameIsTheOnlyGameThatAcceptsJoins() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        assertEquals(Optional.empty(), engine.games().soleOpenGame());

        Game a = engine.open("A");
        assertEquals(Optional.of(a), engine.games().soleOpenGame());

        Game b = engine.open("B");
        assertEquals(Optional.empty(), engine.games().soleOpenGame());

        engine.join(a, "Alice", "Bob");
        engine.start(a);
        engine.games().stop(a);
        assertEquals(Phase.ENDING, a.phase());
        assertEquals(Optional.of(b), engine.games().soleOpenGame());
    }

    @Test
    void aPlayerWhoseGameHasEndedLeavesItQuietlyToJoinAnother() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        Game a = engine.open("A");
        Game b = engine.open("B");
        engine.join(a, "Alice", "Bob");
        engine.start(a);
        engine.games().stop(a);
        int from = engine.events().events().size();

        engine.join(b, "Alice");

        assertEquals(Optional.of(b), engine.games().gameOf(id("Alice")));
        assertEquals(Optional.of(a), engine.games().gameOf(id("Bob")));
        assertEquals(Phase.ENDING, a.phase());
        PlayerSession aliceInA = engine.session(a, "Alice");
        assertFalse(aliceInA.present());
        assertEquals(Optional.of(LeaveReason.GAME_CLOSED), aliceInA.leftReason());
        assertEquals(List.of("left Alice GAME_CLOSED", "joined Alice PLAYING"),
                engine.events().events().subList(from, engine.events().events().size()));
        assertTrue(engine.context(a).log().contains("lobby " + id("Alice")));
    }

    @Test
    void aPlayerWhoseGameHasEndedCanSpectateAnother() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        Game a = engine.open("A");
        Game b = engine.open("B");
        engine.join(a, "Alice", "Bob");
        engine.start(a);
        engine.games().stop(a);

        engine.spectate(b, "Alice");

        assertEquals(Optional.of(b), engine.games().gameOf(id("Alice")));
        assertEquals(Role.SPECTATOR, engine.session(b, "Alice").role());
        assertEquals(Optional.of(LeaveReason.GAME_CLOSED), engine.session(a, "Alice").leftReason());
    }

    @Test
    void aRefusedJoinLeavesThePlayerInTheirEndedGame() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100));
        Game a = engine.open("A");
        Game b = engine.open("B", Map.of("maxPlayers", 2));
        engine.join(a, "Alice", "Bob");
        engine.start(a);
        engine.games().stop(a);
        engine.join(b, "Carol", "Dave");

        assertEquals(Msg.ERROR_GAME_ENDED, refused(() -> engine.join(a, "Alice")).messageKey());
        assertEquals(Msg.ERROR_GAME_ENDED, refused(() -> engine.spectate(a, "Alice")).messageKey());
        assertEquals(Msg.ERROR_GAME_FULL, refused(() -> engine.join(b, "Alice")).messageKey());

        assertEquals(Optional.of(a), engine.games().gameOf(id("Alice")));
        assertTrue(engine.session(a, "Alice").present());
        assertEquals(List.of(), engine.events().events("left "));
    }

    @Test
    void aPartyIsRefusedBeforeAnyMemberLeavesTheirEndedGame() {
        TestEngine engine = engine(duel("A", 0), duel("B", 100), duel("C", 200));
        Game a = engine.open("A");
        Game b = engine.open("B");
        Game c = engine.open("C");
        engine.join(a, "Alice", "Bob");
        engine.start(a);
        engine.games().stop(a);
        engine.join(b, "Carol", "Dave");
        engine.start(b);

        PvpException e = refused(() -> engine.party(c, "Alice", "Carol"));

        assertEquals(Msg.ERROR_ALREADY_IN_GAME, e.messageKey());
        assertEquals(Map.of("player", "Carol"), e.args());
        assertEquals(Optional.of(a), engine.games().gameOf(id("Alice")));
        assertEquals(List.of(), engine.events().events("left "));
    }

    @Test
    void aLeaveFromInsidePlayerJoinedIsNotLost() {
        GameManager[] holder = new GameManager[1];
        GameListener listener = new GameListener() {
            @Override
            public void playerJoined(Game game, PlayerSession session, JoinResult result) {
                holder[0].leave(session.id(), LeaveReason.LEFT);
            }
        };
        GameManager manager = new GameManager(TestArenas.registry(dir, duel("A", 0)), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), FakeGameContext.factory(new ArrayList<>()), listener, player -> 0.0);
        holder[0] = manager;
        Game game = manager.open("A", Map.of(), "Staff");

        manager.join(JoinUnit.solo(TestArenas.player("Alice")), game);

        assertEquals(Optional.empty(), manager.gameOf(id("Alice")));
        assertFalse(game.session(id("Alice")).orElseThrow().present());
    }

    @Test
    void aLeaveFromInsideSpectatingIsNotLost() {
        GameManager[] holder = new GameManager[1];
        GameListener listener = new GameListener() {
            @Override
            public void playerJoined(Game game, PlayerSession session, JoinResult result) {
                holder[0].leave(session.id(), LeaveReason.LEFT);
            }
        };
        GameManager manager = new GameManager(TestArenas.registry(dir, duel("A", 0)), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), FakeGameContext.factory(new ArrayList<>()), listener, player -> 0.0);
        holder[0] = manager;
        Game game = manager.open("A", Map.of(), "Staff");

        manager.spectate(TestArenas.player("Alice"), game);

        assertEquals(Optional.empty(), manager.gameOf(id("Alice")));
        assertFalse(game.session(id("Alice")).orElseThrow().present());
    }

    @Test
    void aMemberWhoNeverJoinedIsNotRecorded() {
        FakeGameContext fake = new FakeGameContext();
        UUID alice = TestArenas.player("Alice").id();
        UUID bob = TestArenas.player("Bob").id();
        GameContextFactory factory = (id, arena) ->
                new StubPlayersContext(fake, new FailsToTeleport(fake.players(), alice));
        GameManager manager = new GameManager(TestArenas.registry(dir, duel("A", 0), duel("B", 100)),
                new ModeRegistry(List.of(DUEL)), PvpConfig.defaults(), factory, new GameListener() {
                }, player -> 0.0);
        Game game = manager.open("A", Map.of(), "Staff");

        RuntimeException failure = assertThrows(RuntimeException.class, () -> manager.join(
                JoinUnit.party(List.of(TestArenas.player("Alice"), TestArenas.player("Bob"))), game));

        assertEquals("teleport failed for " + alice, failure.getMessage());
        assertEquals(Optional.of(game), manager.gameOf(alice));
        assertTrue(game.session(alice).orElseThrow().present());
        assertEquals(Optional.empty(), manager.gameOf(bob));
        assertEquals(Optional.empty(), game.session(bob));

        Game other = manager.open("B", Map.of(), "Staff");
        manager.join(JoinUnit.solo(TestArenas.player("Bob")), other);
        assertEquals(Optional.of(other), manager.gameOf(bob));
    }

    @Test
    void aGameWhoseHeartbeatCannotStartIsNotKept() {
        FakeGameContext fake = new FakeGameContext();
        GameContextFactory factory = (id, arena) -> new NoHeartbeatContext(fake);
        RecordingListener events = new RecordingListener();
        GameManager manager = new GameManager(TestArenas.registry(dir, duel("A", 0)), new ModeRegistry(List.of(DUEL)),
                PvpConfig.defaults(), factory, events, player -> 0.0);

        assertThrows(RuntimeException.class, () -> manager.open("A", Map.of(), "Staff"));

        assertEquals(List.of(), manager.games());
        assertEquals(Optional.empty(), manager.blockerFor(duel("A", 0)));
        assertTrue(fake.disposed());
        assertEquals(List.of(), events.events());
    }

    @Test
    void aTeamCountTheArenaCannotHoldIsRefusedOnTheTeamCount() {
        TestMode teams = new TestMode("teams", TestMode.twoTeams().teams(TeamLayout.configurable(2, 4)).build());
        Arena base = TestArenas.tdm("A", 0, 4).withMode("teams");
        Arena arena = new Arena(base.schemaVersion(), base.id(), base.modeId(), base.unassignedReason(), base.title(),
                3, base.resourcePackUrl(), base.spawn(), base.points(), base.teamSpawns(), base.region(),
                base.imported());
        TestEngine engine = new TestEngine(dir, new ModeRegistry(List.of(teams)), PvpConfig.defaults(), arena);

        PvpException e = refused(() -> engine.open("A", Map.of("teamCount", 4)));

        assertEquals(Msg.ERROR_SETTING_RANGE, e.messageKey());
        assertEquals(Map.of("setting", "teamCount", "min", "2", "max", "3"), e.args());
    }

    /**
     * Delegates to the fake except tasks(), whose every(...) always refuses (as a scheduler might at plugin disable).
     */
    private record NoHeartbeatContext(GameContext delegate) implements GameContext {

        @Override
        public PlayerActions players() {
            return delegate.players();
        }

        @Override
        public GameTasks tasks() {
            return new GameTasks() {
                @Override
                public TaskHandle every(long periodTicks, Runnable task) {
                    throw new RuntimeException("heartbeat refused");
                }

                @Override
                public TaskHandle after(long delayTicks, Runnable task) {
                    return delegate.tasks().after(delayTicks, task);
                }

                @Override
                public void cancelAll() {
                    delegate.tasks().cancelAll();
                }

                @Override
                public int activeCount() {
                    return delegate.tasks().activeCount();
                }

                @Override
                public long currentTick() {
                    return delegate.tasks().currentTick();
                }
            };
        }

        @Override
        public WorldActions world() {
            return delegate.world();
        }

        @Override
        public Clock clock() {
            return delegate.clock();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }

    /** Delegates to {@code delegate} except players(), which is substituted directly. */
    private record StubPlayersContext(GameContext delegate, PlayerActions players) implements GameContext {

        @Override
        public GameTasks tasks() {
            return delegate.tasks();
        }

        @Override
        public WorldActions world() {
            return delegate.world();
        }

        @Override
        public Clock clock() {
            return delegate.clock();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }

    /** Delegates every player action to {@code delegate} except teleport, which fails for one chosen player. */
    private record FailsToTeleport(PlayerActions delegate, UUID failing) implements PlayerActions {

        @Override
        public void teleport(UUID player, NamedPoint point) {
            if (player.equals(failing)) {
                throw new RuntimeException("teleport failed for " + player);
            }
            delegate.teleport(player, point);
        }

        @Override
        public void resetForPlay(UUID player) {
            delegate.resetForPlay(player);
        }

        @Override
        public void giveKit(UUID player, Kit kit, TeamSpec color) {
            delegate.giveKit(player, kit, color);
        }

        @Override
        public void setSpectator(UUID player, boolean spectator) {
            delegate.setSpectator(player, spectator);
        }

        @Override
        public void joinScoreboard(UUID player, TeamSpec color) {
            delegate.joinScoreboard(player, color);
        }

        @Override
        public void leaveScoreboard(UUID player) {
            delegate.leaveScoreboard(player);
        }

        @Override
        public void sendToLobby(UUID player) {
            delegate.sendToLobby(player);
        }
    }
}
