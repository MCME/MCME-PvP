package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Joining after the countdown began, by the mode's MidgameJoin policy and the MIDGAME_JOIN setting. */
class MidgameJoinTest {

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(String arenaId, Map<String, ?> settings, String... names) {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2), TestArenas.ffa("AS-FFA", 100, 3));
        Game game = engine.open(arenaId, settings);
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    private JoinResult join(Game game, String name) {
        return engine.games().join(JoinUnit.solo(TestArenas.player(name)), game);
    }

    /** The players join together as one party; returns where they ended up. */
    private JoinResult party(Game game, String... names) {
        List<PlayerRef> members = Arrays.stream(names).map(TestArenas::player).toList();
        return engine.games().join(JoinUnit.party(members), game);
    }

    @Test
    void freeForAllLetsLateJoinersPlayAtOnce() {
        Game game = running("AS-FFA", Map.of(), "Alice", "Bob");

        assertEquals(JoinResult.PLAYING, join(game, "Carol"));

        PlayerSession carol = engine.session(game, "Carol");
        FakeGameContext context = engine.context(game);
        assertTrue(carol.alive());
        assertTrue(carol.participated());
        assertEquals(Optional.of(Palette.PERSONAL.get(2)), carol.color());
        assertEquals(game.arena().points().get("PlayerSpawn2"), context.positions().get(carol.id()));
        assertEquals(Kits.STANDARD, context.kits().get(carol.id()));
        assertEquals(Palette.PERSONAL.get(2), context.colors().get(carol.id()));
        assertEquals("joined Carol PLAYING", engine.events().events().getLast());
    }

    @Test
    void teamDeathmatchPutsLateJoinersOnTheSmallestTeamWhileNoTeamIsBelowItsStart() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");

        assertEquals(JoinResult.PLAYING, join(game, "Erin"));
        assertEquals(JoinResult.PLAYING, join(game, "Frank"));

        assertEquals("red", engine.session(game, "Erin").team().orElseThrow().id());
        assertEquals("blue", engine.session(game, "Frank").team().orElseThrow().id());
        assertEquals(3, game.teams().get(0).aliveCount());
        assertEquals(3, game.teams().get(1).aliveCount());
    }

    @Test
    void onceATeamIsBelowItsStartLateJoinersSpectate() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        engine.kill(game, "Bob", "Alice");
        assertFalse(game.canJoinAsPlayer(1));

        assertEquals(JoinResult.SPECTATING, join(game, "Erin"));

        assertEquals(Role.SPECTATOR, engine.session(game, "Erin").role());
        assertTrue(engine.context(game).spectating().contains(id("Erin")));
        assertEquals("joined Erin SPECTATING", engine.events().events().getLast());
    }

    @Test
    void aPartyJoiningLateStaysTogether() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");

        assertEquals(JoinResult.PLAYING, party(game, "Erin", "Frank"));

        assertEquals("red", engine.session(game, "Erin").team().orElseThrow().id());
        assertEquals("red", engine.session(game, "Frank").team().orElseThrow().id());
    }

    @Test
    void withMidgameJoinOffLateJoinersSpectate() {
        Game game = running("AS-FFA", Map.of("midgameJoin", false), "Alice", "Bob");

        assertFalse(game.canJoinAsPlayer(1));
        assertEquals(JoinResult.SPECTATING, join(game, "Carol"));
    }

    @Test
    void aFullGameLetsLateJoinersSpectate() {
        Game game = running("AS-FFA", Map.of("maxPlayers", 2), "Alice", "Bob");

        assertFalse(game.canJoinAsPlayer(1));
        assertEquals(JoinResult.SPECTATING, join(game, "Carol"));
        assertEquals(2, game.playerCount());
    }

    @Test
    void aLateJoinerDuringTheCountdownIsFrozenWithTheOthers() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.ffa("AS-FFA", 0, 3));
        Game game = engine.open("AS-FFA");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);

        assertTrue(game.canJoinAsPlayer(1));
        assertEquals(JoinResult.PLAYING, join(game, "Carol"));
        assertTrue(game.isFrozen(id("Carol")));
    }

    @Test
    void canJoinAsPlayerLooksAtRoomWhileWaiting() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.ffa("AS-FFA", 0, 3));
        Game game = engine.open("AS-FFA", Map.of("maxPlayers", 3));
        engine.join(game, "Alice", "Bob");

        assertTrue(game.canJoinAsPlayer(1));
        assertFalse(game.canJoinAsPlayer(2));
    }

    @Test
    void midgamePlacementPicksTheFewestAliveNotTheFewestMembers() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        assertEquals(JoinResult.PLAYING, join(game, "Erin"));
        engine.kill(game, "Alice", "Bob");

        // Red now has 3 members but only 2 alive, tying blue's 2 members/2 alive: fewest alive ties and palette
        // order picks red, while fewest members would have picked blue outright (2 < 3).
        assertEquals(JoinResult.PLAYING, join(game, "Frank"));

        assertEquals("red", engine.session(game, "Frank").team().orElseThrow().id());
    }

    @Test
    void aPartyBiggerThanTheFreePlacesSpectatesEntirely() {
        Game game = running("AS-FFA", Map.of("maxPlayers", 3), "Alice", "Bob");

        assertEquals(JoinResult.SPECTATING, party(game, "Carol", "Dave"));

        assertEquals(Role.SPECTATOR, engine.session(game, "Carol").role());
        assertEquals(Role.SPECTATOR, engine.session(game, "Dave").role());
        assertEquals(2, game.playerCount());
    }

    @Test
    void canJoinAsPlayerIsFalseWhileEndingOrClosed() {
        Game game = running("AS-FFA", Map.of(), "Alice", "Bob");

        engine.games().stop(game);
        assertEquals(Phase.ENDING, game.phase());
        assertFalse(game.canJoinAsPlayer(1));

        engine.seconds(game, PvpConfig.defaults().endingSeconds());
        assertEquals(Phase.CLOSED, game.phase());
        assertFalse(game.canJoinAsPlayer(1));
    }

    @Test
    void aFormerParticipantWhoComesBackKeepsHerStatsAndPlays() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        assertEquals(JoinResult.PLAYING, join(game, "Erin"));
        assertEquals(JoinResult.PLAYING, join(game, "Frank"));
        engine.kill(game, "Bob", "Alice");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(JoinResult.PLAYING, join(game, "Alice"));

        PlayerSession alice = engine.session(game, "Alice");
        assertEquals(Role.PLAYER, alice.role());
        assertTrue(alice.alive());
        assertEquals(1, alice.kills());
        assertEquals("red", alice.team().orElseThrow().id());
        assertEquals(3, game.team("red").orElseThrow().aliveCount());
    }

    @Test
    void anEliminatedPlayerWhoComesBackOnlyWatchesEvenWhereANewcomerWouldPlay() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        assertEquals(JoinResult.PLAYING, join(game, "Erin"));
        engine.kill(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);
        // Erin made up for Alice, so every team is at its start again and a newcomer would still play.
        assertTrue(game.canJoinAsPlayer(1));

        assertEquals(JoinResult.SPECTATING, join(game, "Alice"));

        PlayerSession alice = engine.session(game, "Alice");
        assertEquals(Role.SPECTATOR, alice.role());
        assertFalse(alice.alive());
        assertTrue(alice.eliminated());
        assertEquals(1, alice.deaths());
        assertEquals("red", alice.team().orElseThrow().id());
        assertEquals(2, game.team("red").orElseThrow().aliveCount());
        assertTrue(engine.context(game).spectating().contains(id("Alice")));
        assertEquals("joined Alice SPECTATING", engine.events().events().getLast());
    }

    @Test
    void anEliminatedPlayerInAPartyWatchesWhileTheRestOfThePartyPlays() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave");
        assertEquals(JoinResult.PLAYING, join(game, "Erin"));
        engine.kill(game, "Alice", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(JoinResult.PLAYING, party(game, "Alice", "George"));

        assertEquals(Role.SPECTATOR, engine.session(game, "Alice").role());
        PlayerSession george = engine.session(game, "George");
        assertTrue(george.alive());
        assertEquals("red", george.team().orElseThrow().id());
    }

    @Test
    void aFormerParticipantSpectatesWhenHerTeamCannotTakeHerBack() {
        Game game = running("AS-TDM", Map.of(), "Alice", "Bob", "Carol", "Dave", "Erin", "Frank");
        // Red (Alice, Carol, Erin) falls below its start of three.
        engine.kill(game, "Carol", "Bob");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(JoinResult.SPECTATING, join(game, "Alice"));

        PlayerSession alice = engine.session(game, "Alice");
        assertEquals(Role.SPECTATOR, alice.role());
        assertFalse(alice.eliminated());
        assertEquals("red", alice.team().orElseThrow().id());
        assertFalse(game.team("red").orElseThrow().members().contains(alice));
    }

    @Test
    void aFormerFfaParticipantWhoComesBackKeepsHerStatsAndColor() {
        Game game = running("AS-FFA", Map.of(), "Alice", "Bob");
        engine.kill(game, "Bob", "Alice");
        engine.games().leave(id("Alice"), LeaveReason.LEFT);

        assertEquals(JoinResult.PLAYING, join(game, "Alice"));

        PlayerSession alice = engine.session(game, "Alice");
        assertEquals(1, alice.kills());
        assertEquals(1, alice.score());
        assertEquals(Optional.of(Palette.PERSONAL.get(0)), alice.color());
    }

    @Test
    void aLeaverDuringTheCountdownDoesNotCloseLateJoiningForTheWholeGame() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, 2));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob", "Carol", "Dave", "Erin", "Frank");
        engine.games().startCountdown(game);

        engine.games().leave(id("Carol"), LeaveReason.LEFT);

        assertTrue(game.canJoinAsPlayer(1));
        assertEquals(JoinResult.PLAYING, join(game, "George"));

        engine.seconds(game, PvpConfig.defaults().countdownSeconds());

        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(3, game.team("red").orElseThrow().startingSize());
        assertEquals(3, game.team("blue").orElseThrow().startingSize());
    }

    @Test
    void aTeamThatLostAPlayerInTheCountdownStartsSmallerAndStillTakesLateJoiners() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, 2));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob", "Carol", "Dave", "Erin", "Frank");
        engine.games().startCountdown(game);

        engine.games().leave(id("Carol"), LeaveReason.LEFT);

        engine.seconds(game, PvpConfig.defaults().countdownSeconds());

        assertEquals(Phase.RUNNING, game.phase());
        assertTrue(game.canJoinAsPlayer(1));
        assertEquals(2, game.team("red").orElseThrow().startingSize());
    }

    @Test
    void startingSizesAreTakenAgainWhenTheGameStartsRunning() {
        engine = new TestEngine(dir, TestArenas.modes(), PvpConfig.defaults(), TestArenas.tdm("AS-TDM", 0, 2));
        Game game = engine.open("AS-TDM");
        engine.join(game, "Alice", "Bob");
        engine.games().startCountdown(game);

        assertEquals(JoinResult.PLAYING, join(game, "Carol"));

        engine.seconds(game, PvpConfig.defaults().countdownSeconds());

        assertEquals(Phase.RUNNING, game.phase());
        assertEquals(2, game.team("red").orElseThrow().startingSize());
        assertEquals(1, game.team("blue").orElseThrow().startingSize());
    }
}
