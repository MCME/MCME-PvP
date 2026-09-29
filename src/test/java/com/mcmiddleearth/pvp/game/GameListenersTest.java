package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.RecordingLog;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameListenersTest {

    /** Writes "<name> <event>" for every notification; the game argument is not used, so tests pass null. */
    private record Named(String name, List<String> calls) implements GameListener {
        @Override
        public void gameOpened(Game game, String openedBy) {
            calls.add(name + " gameOpened " + openedBy);
        }

        @Override
        public void phaseChanged(Game game, Phase from, Phase to) {
            calls.add(name + " phaseChanged");
        }

        @Override
        public void playerJoined(Game game, PlayerSession session, JoinResult result) {
            calls.add(name + " playerJoined");
        }

        @Override
        public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
            calls.add(name + " playerLeft");
        }

        @Override
        public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
            calls.add(name + " playerDied");
        }

        @Override
        public void playerFled(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
            calls.add(name + " playerFled");
        }

        @Override
        public void scoreChanged(Game game) {
            calls.add(name + " scoreChanged");
        }

        @Override
        public void secondPassed(Game game) {
            calls.add(name + " secondPassed");
        }

        @Override
        public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
            calls.add(name + " gameEnded");
        }

        @Override
        public void gameClosed(Game game) {
            calls.add(name + " gameClosed");
        }
    }

    private final List<String> calls = new ArrayList<>();
    private final RecordingLog log = new RecordingLog();

    @Test
    void listenersRunInTheOrderTheyWereAdded() {
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new Named("first", calls));
        listeners.add(new Named("second", calls));

        listeners.gameOpened(null, "Console");

        assertEquals(List.of("first gameOpened Console", "second gameOpened Console"), calls);
    }

    @Test
    void everyNotificationIsForwarded() {
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new Named("l", calls));

        listeners.gameOpened(null, "Staff");
        listeners.phaseChanged(null, Phase.WAITING, Phase.COUNTDOWN);
        listeners.playerJoined(null, null, JoinResult.PLAYING);
        listeners.playerLeft(null, null, LeaveReason.LEFT);
        listeners.playerDied(null, null, Optional.empty());
        listeners.playerFled(null, null, Optional.empty());
        listeners.scoreChanged(null);
        listeners.secondPassed(null);
        listeners.gameEnded(null, Outcome.draw(EndReason.STOPPED), null);
        listeners.gameClosed(null);

        assertEquals(List.of("l gameOpened Staff", "l phaseChanged", "l playerJoined", "l playerLeft", "l playerDied",
                "l playerFled", "l scoreChanged", "l secondPassed", "l gameEnded", "l gameClosed"), calls);
    }

    @Test
    void aListenerThatDoesNotTellAFleeFromADeathHearsItAsADeath() {
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new GameListener() {
            @Override
            public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
                calls.add("died");
            }
        });

        listeners.playerFled(null, null, Optional.empty());

        assertEquals(List.of("died"), calls);
    }

    @Test
    void aFailingListenerIsLoggedAndTheNextOneStillRuns() {
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new GameListener() {
            @Override
            public void phaseChanged(Game game, Phase from, Phase to) {
                throw new IllegalStateException("broken HUD");
            }
        });
        listeners.add(new Named("stats", calls));

        listeners.phaseChanged(null, Phase.RUNNING, Phase.ENDING);

        assertEquals(List.of("stats phaseChanged"), calls);
        List<String> severe = log.messages(Level.SEVERE);
        assertEquals(1, severe.size());
        assertTrue(severe.get(0).contains("phaseChanged"), severe.get(0));
    }

    @Test
    void repeatedFailuresAreThrottledButAnotherListenersFirstFailureStillLogsInFull() {
        GameListeners listeners = new GameListeners(log.logger());
        listeners.add(new GameListener() {
            @Override
            public void secondPassed(Game game) {
                throw new IllegalStateException("broken HUD");
            }
        });
        listeners.add(new GameListener() {
            @Override
            public void scoreChanged(Game game) {
                throw new IllegalStateException("broken score");
            }
        });

        for (int i = 0; i < 250; i++) {
            listeners.secondPassed(null);
        }
        listeners.scoreChanged(null);

        List<String> severe = log.messages(Level.SEVERE);
        assertEquals(4, severe.size(), severe.toString());
        assertTrue(severe.get(0).contains("secondPassed") && !severe.get(0).contains("times"), severe.get(0));
        assertTrue(severe.get(1).contains("failed 100 times in secondPassed") && severe.get(1).contains("broken HUD"),
                severe.get(1));
        assertTrue(severe.get(2).contains("failed 200 times in secondPassed") && severe.get(2).contains("broken HUD"),
                severe.get(2));
        assertTrue(severe.get(3).contains("scoreChanged") && !severe.get(3).contains("times"), severe.get(3));
    }
}
