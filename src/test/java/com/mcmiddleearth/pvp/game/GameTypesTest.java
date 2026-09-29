package com.mcmiddleearth.pvp.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTypesTest {

    private static final PlayerRef ALICE = new PlayerRef(UUID.nameUUIDFromBytes("Alice".getBytes()), "Alice");
    private static final PlayerRef BOB = new PlayerRef(UUID.nameUUIDFromBytes("Bob".getBytes()), "Bob");

    @Test
    void gameIdPrintsTheUuidAndShortensToEightHexDigits() {
        GameId id = new GameId(UUID.fromString("12345678-9abc-def0-1234-56789abcdef0"));

        assertEquals("12345678-9abc-def0-1234-56789abcdef0", id.toString());
        assertEquals("12345678", id.shortId());
        assertNotEquals(GameId.random(), GameId.random());
    }

    @Test
    void joinUnitNeedsAtLeastOnePlayer() {
        assertThrows(IllegalArgumentException.class, () -> new JoinUnit(List.of()));
    }

    @Test
    void joinUnitKeepsItsOwnCopyOfTheMembers() {
        List<PlayerRef> members = new ArrayList<>(List.of(ALICE, BOB));
        JoinUnit party = JoinUnit.party(members);
        members.clear();

        assertEquals(List.of(ALICE, BOB), party.members());
        assertEquals(2, party.size());
        assertEquals(1, JoinUnit.solo(ALICE).size());
        assertThrows(UnsupportedOperationException.class, () -> party.members().add(ALICE));
    }

    @Test
    void joinUnitRejectsTheSamePlayerTwice() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JoinUnit.party(List.of(ALICE, ALICE)));

        assertTrue(ex.getMessage().contains("Alice"));
    }

    @Test
    void joinUnitRejectsTheSameUuidUnderADifferentName() {
        PlayerRef aliceRenamed = new PlayerRef(ALICE.id(), "NotAlice");

        assertThrows(IllegalArgumentException.class, () -> JoinUnit.party(List.of(ALICE, aliceRenamed)));
    }

    @Test
    void endReasonsHaveLowercaseRecordNames() {
        assertEquals("win_condition", EndReason.WIN_CONDITION.recordName());
        assertEquals("time_limit", EndReason.TIME_LIMIT.recordName());
        assertEquals("stopped", EndReason.STOPPED.recordName());
        assertEquals("abandoned", EndReason.ABANDONED.recordName());
    }

    @Test
    void byGameIsTrueOnlyForGameClosedAndShutdown() {
        assertFalse(LeaveReason.LEFT.byGame());
        assertFalse(LeaveReason.QUIT.byGame());
        assertFalse(LeaveReason.KICKED.byGame());
        assertTrue(LeaveReason.GAME_CLOSED.byGame());
        assertTrue(LeaveReason.SHUTDOWN.byGame());
    }

    @Test
    void anOutcomeWithoutWinnerIsADraw() {
        Outcome draw = Outcome.draw(EndReason.STOPPED);
        Outcome team = Outcome.team("red", EndReason.WIN_CONDITION);
        Outcome player = Outcome.player(ALICE.id(), EndReason.TIME_LIMIT);

        assertTrue(draw.draw());
        assertFalse(team.draw());
        assertEquals("red", team.winningTeam());
        assertNull(team.winningPlayer());
        assertFalse(player.draw());
        assertEquals(ALICE.id(), player.winningPlayer());
        assertEquals(EndReason.TIME_LIMIT, player.reason());
    }

    @Test
    void outcomeNeedsAReason() {
        assertThrows(NullPointerException.class, () -> Outcome.draw(null));
    }

    @Test
    void outcomeAllowsAtMostOneWinner() {
        assertThrows(IllegalArgumentException.class,
                () -> new Outcome(EndReason.STOPPED, "red", UUID.randomUUID()));
    }

    @Test
    void phasesAreDeclaredInLifecycleOrder() {
        assertEquals(List.of(Phase.WAITING, Phase.COUNTDOWN, Phase.RUNNING, Phase.ENDING, Phase.CLOSED),
                List.of(Phase.values()));
    }
}
