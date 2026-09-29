package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.ModeLogic;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.testing.FakeGameContext;
import com.mcmiddleearth.pvp.testing.TestArenas;
import com.mcmiddleearth.pvp.testing.TestEngine;
import com.mcmiddleearth.pvp.testing.TestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mcmiddleearth.pvp.testing.TestEngine.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mode hooks (ModeLogic) and the entities a game owns. */
class ModeHooksTest {

    /** Writes every hook call; an entity kill is worth 5 points to the killer. */
    private static final class Recorder implements ModeLogic {

        private final List<String> calls = new ArrayList<>();
        private Boolean victimAliveDuringOnDeath;
        private Integer victimTeamAliveCountDuringOnDeath;

        @Override
        public void onStart(Game game) {
            calls.add("start");
        }

        @Override
        public void onDeath(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
            calls.add("death " + victim.name() + killer.map(k -> " by " + k.name()).orElse(""));
            victimAliveDuringOnDeath = victim.alive();
            victimTeamAliveCountDuringOnDeath = victim.team().map(GameTeam::aliveCount).orElse(-1);
        }

        @Override
        public void onRespawn(Game game, PlayerSession player) {
            calls.add("respawn " + player.name());
        }

        @Override
        public void onLeave(Game game, PlayerSession player) {
            calls.add("leave " + player.name());
        }

        @Override
        public void onInteract(Game game, PlayerSession player, NamedPoint block) {
            calls.add("interact " + player.name() + " " + block.x());
        }

        @Override
        public void onEntityKilled(Game game, UUID entity, Optional<PlayerSession> killer) {
            calls.add("entity " + killer.map(PlayerSession::name).orElse("-"));
            killer.ifPresent(k -> game.scores().addScore(k, 5));
        }

        @Override
        public void tick(Game game) {
            calls.add("tick");
        }
    }

    private final List<Recorder> logics = new ArrayList<>();

    @TempDir
    Path dir;

    private TestEngine engine;

    private Game running(String... names) {
        TestMode hooked = new TestMode("hooked", TestMode.twoTeams()
                .respawn(Respawn.AT_SPAWN)
                .win(WinConditions.lastTeamStanding().orFirstTo(Setting.SCORE_GOAL))
                .build(), () -> {
                    Recorder logic = new Recorder();
                    logics.add(logic);
                    return logic;
                });
        engine = new TestEngine(dir, new ModeRegistry(List.of(hooked)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("hooked"),
                TestArenas.tdm("AS-TDM2", 100, 2).withMode("hooked"));
        Game game = engine.open("AS-TDM", Map.of("scoreGoal", 5));
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    /** A NEVER-respawn, last-team-standing game: a death eliminates the victim instead of respawning them. */
    private Game runningNever(String... names) {
        TestMode never = new TestMode("never", TestMode.twoTeams().build(), () -> {
            Recorder logic = new Recorder();
            logics.add(logic);
            return logic;
        });
        engine = new TestEngine(dir, new ModeRegistry(List.of(never)), PvpConfig.defaults(),
                TestArenas.tdm("AS-TDM", 0, 2).withMode("never"));
        Game game = engine.open("AS-TDM");
        engine.join(game, names);
        engine.start(game);
        return game;
    }

    private List<String> calls() {
        return logics.get(0).calls;
    }

    @Test
    void theGameStartsAndTicksTheHooks() {
        Game game = running("Alice", "Bob");

        engine.seconds(game, 2);

        assertEquals(List.of("start", "tick", "tick"), calls());
    }

    @Test
    void deathsRespawnsAndLeavesReachTheHooks() {
        Game game = running("Alice", "Bob", "Carol");

        engine.kill(game, "Bob", "Alice");
        game.handleRespawn(id("Bob"));
        engine.context(game).advanceTicks(1);
        engine.games().leave(id("Carol"), LeaveReason.LEFT);

        assertEquals(List.of("start", "death Bob by Alice", "respawn Bob", "leave Carol"), calls());
    }

    @Test
    void onDeathSeesTheVictimAfterTheEliminationUnderNever() {
        Game game = runningNever("Alice", "Bob", "Carol", "Dave");

        engine.kill(game, "Bob", "Alice");

        Recorder logic = logics.get(0);
        assertEquals(Boolean.FALSE, logic.victimAliveDuringOnDeath);
        assertEquals(Integer.valueOf(1), logic.victimTeamAliveCountDuringOnDeath);
    }

    @Test
    void interactionsReachTheHooksOnlyForLivingPlayersWhileRunning() {
        Game game = running("Alice", "Bob");
        engine.spectate(game, "Carol");

        game.handleInteract(id("Alice"), NamedPoint.of("world", 7, 64, 7));
        game.handleInteract(id("Carol"), NamedPoint.of("world", 8, 64, 8));
        engine.games().stop(game);
        game.handleInteract(id("Bob"), NamedPoint.of("world", 9, 64, 9));

        assertEquals(List.of("start", "interact Alice 7.0"), calls());
    }

    @Test
    void anEliminatedPlayersInteractDoesNotReachTheHook() {
        Game game = runningNever("Alice", "Bob", "Carol", "Dave");
        engine.kill(game, "Bob", "Alice");

        game.handleInteract(id("Bob"), NamedPoint.of("world", 7, 64, 7));

        assertEquals(List.of("start", "death Bob by Alice"), calls());
    }

    @Test
    void everyGameGetsItsOwnHooks() {
        Game first = running("Alice", "Bob");
        Game second = engine.open("AS-TDM2");

        assertEquals(2, logics.size());
        assertNotSame(logics.get(0), logics.get(1));
        assertEquals(Phase.WAITING, second.phase());
        assertEquals(Phase.RUNNING, first.phase());
    }

    @Test
    void spawnedEntitiesBelongToTheGameAndGoAtClose() {
        Game game = running("Alice", "Bob");
        FakeGameContext context = engine.context(game);

        UUID zombie = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5));

        assertTrue(game.entities().owns(zombie));
        assertEquals(List.of(zombie), List.copyOf(game.entities().all()));
        assertTrue(context.spawnedEntities().contains(zombie));

        engine.games().stop(game);
        engine.seconds(game, 10);

        assertEquals(Phase.CLOSED, game.phase());
        assertTrue(context.spawnedEntities().isEmpty());
        assertTrue(context.log().contains("remove " + zombie));
    }

    @Test
    void spawningAnEntityOnAClosedGameThrows() {
        Game game = running("Alice", "Bob");
        engine.games().stop(game);
        engine.seconds(game, 10);
        assertEquals(Phase.CLOSED, game.phase());

        assertThrows(IllegalStateException.class,
                () -> game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5)));
        assertTrue(engine.context(game).log("spawn ").isEmpty());
    }

    @Test
    void anEntityDeathReachesOnlyTheGameThatOwnsIt() {
        Game game = running("Alice", "Bob");

        game.handleEntityDeath(UUID.randomUUID(), id("Alice"));
        assertEquals(List.of("start"), calls());

        Game other = engine.open("AS-TDM2");
        UUID zombie = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5));

        other.handleEntityDeath(zombie, id("Alice"));
        assertEquals(List.of(), logics.get(1).calls);

        game.handleEntityDeath(zombie, id("Alice"));

        assertEquals(List.of("start", "entity Alice"), calls());
        assertFalse(game.entities().owns(zombie));
    }

    @Test
    void anEntityKillWithoutACreditedKillerHasAnEmptyKiller() {
        Game game = running("Alice", "Bob", "Dave");
        engine.spectate(game, "Carol");
        // Dave still has a PLAYER role after leaving, so this also proves the check is presence, not just a
        // session that once existed (a stranger's random uuid could never tell the two apart).
        engine.games().leave(id("Dave"), LeaveReason.LEFT);
        UUID bySpectator = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5));
        UUID byAbsentPlayer = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 6, 64, 6));
        UUID byNullKiller = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 7, 64, 7));

        game.handleEntityDeath(bySpectator, id("Carol"));
        game.handleEntityDeath(byAbsentPlayer, id("Dave"));
        game.handleEntityDeath(byNullKiller, null);

        assertEquals(List.of("start", "leave Dave", "entity -", "entity -", "entity -"), calls());
    }

    @Test
    void anEntityDeathDuringEndingIsForgottenButDoesNotReachTheHook() {
        Game game = running("Alice", "Bob");
        UUID zombie = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5));
        engine.games().stop(game);

        game.handleEntityDeath(zombie, id("Alice"));

        assertEquals(Phase.ENDING, game.phase());
        assertFalse(game.entities().owns(zombie));
        assertEquals(List.of("start"), calls());
    }

    @Test
    void aHookThatScoresCanWinTheGame() {
        Game game = running("Alice", "Bob");
        UUID zombie = game.spawnEntity("ZOMBIE", NamedPoint.of("world", 5, 64, 5));

        game.handleEntityDeath(zombie, id("Alice"));

        assertEquals(Phase.ENDING, game.phase());
        assertEquals(Optional.of(Outcome.team("red", EndReason.WIN_CONDITION)), game.outcome());
    }
}
