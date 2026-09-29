package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.team.Palette;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSessionTest {

    private static PlayerSession session(String name, int joinOrder) {
        return new PlayerSession(new PlayerRef(UUID.nameUUIDFromBytes(name.getBytes()), name), joinOrder);
    }

    @Test
    void aNewSessionIsAPresentAlivePlayerWithoutAColor() {
        PlayerSession alice = session("Alice", 0);

        assertEquals(Role.PLAYER, alice.role());
        assertTrue(alice.alive());
        assertTrue(alice.present());
        assertFalse(alice.participated());
        assertEquals(Optional.empty(), alice.color());
        assertEquals(Optional.empty(), alice.team());
        assertEquals(Optional.empty(), alice.leftReason());
    }

    @Test
    void killDeathRatioIsTheKillsWhileThereAreNoDeaths() {
        PlayerSession alice = session("Alice", 0);
        alice.addKill();
        alice.addKill();
        alice.addKill();
        assertEquals(3.0, alice.killDeathRatio());

        alice.addDeath();
        alice.addDeath();
        assertEquals(1.5, alice.killDeathRatio());
    }

    @Test
    void teamMembersArePresentPlayersInJoinOrder() {
        GameTeam red = new GameTeam(Palette.RED);
        PlayerSession alice = session("Alice", 0);
        PlayerSession bob = session("Bob", 1);
        PlayerSession carol = session("Carol", 2);
        carol.joinTeam(red);
        alice.joinTeam(red);
        bob.joinTeam(red);
        assertEquals(List.of(alice, bob, carol), red.members());

        bob.leave(LeaveReason.LEFT);
        alice.eliminate();

        assertEquals(List.of(alice, carol), red.members());
        assertEquals(1, red.aliveCount());
    }

    @Test
    void aMemberWhoSpectatesKeepsTheTeamButIsNoLongerListed() {
        GameTeam red = new GameTeam(Palette.RED);
        PlayerSession alice = session("Alice", 0);
        alice.joinTeam(red);

        alice.spectate();

        assertEquals(List.of(), red.members());
        assertEquals(Optional.of(red), alice.team());
        assertEquals(Optional.empty(), alice.color());
        assertFalse(alice.alive());
    }

    @Test
    void theColorIsTheTeamsOrThePersonalOne() {
        PlayerSession alice = session("Alice", 0);
        PlayerSession bob = session("Bob", 1);
        alice.joinTeam(new GameTeam(Palette.BLUE));
        bob.personalColor(Palette.PERSONAL.get(0));

        assertEquals(Optional.of(Palette.BLUE), alice.color());
        assertEquals(Optional.of(Palette.PERSONAL.get(0)), bob.color());

        bob.clearPlacement();
        assertEquals(Optional.empty(), bob.color());
    }

    @Test
    void joiningAnotherTeamLeavesTheFirst() {
        GameTeam red = new GameTeam(Palette.RED);
        GameTeam blue = new GameTeam(Palette.BLUE);
        PlayerSession alice = session("Alice", 0);

        alice.joinTeam(red);
        alice.joinTeam(blue);

        assertEquals(List.of(), red.members());
        assertEquals(List.of(alice), blue.members());

        alice.clearPlacement();
        assertEquals(List.of(), blue.members());
        assertEquals(Optional.empty(), alice.team());
    }

    @Test
    void leavingKeepsTheReasonUntilThePlayerComesBack() {
        PlayerSession alice = session("Alice", 0);
        alice.leave(LeaveReason.QUIT);
        assertFalse(alice.present());
        assertEquals(Optional.of(LeaveReason.QUIT), alice.leftReason());

        alice.reactivate(new PlayerRef(alice.id(), "Alice2"));

        assertTrue(alice.present());
        assertEquals(Optional.empty(), alice.leftReason());
        assertEquals("Alice2", alice.name());
    }

    @Test
    void reactivateKeepsKillsDeathsTeamAndParticipation() {
        GameTeam red = new GameTeam(Palette.RED);
        PlayerSession alice = session("Alice", 0);
        alice.joinTeam(red);
        alice.markParticipated();
        alice.addKill();
        alice.addDeath();

        alice.leave(LeaveReason.LEFT);
        alice.reactivate(new PlayerRef(alice.id(), "Alice2"));

        assertEquals(1, alice.kills());
        assertEquals(1, alice.deaths());
        assertEquals(Optional.of(red), alice.team());
        assertTrue(alice.participated());
    }

    @Test
    void eliminateSpectateAndLeaveEachClearProtection() {
        PlayerSession alice = session("Alice", 0);
        alice.protectFor(5);
        alice.eliminate();
        assertFalse(alice.isProtected());

        PlayerSession bob = session("Bob", 1);
        bob.protectFor(5);
        bob.spectate();
        assertFalse(bob.isProtected());

        PlayerSession carol = session("Carol", 2);
        carol.protectFor(5);
        carol.leave(LeaveReason.LEFT);
        assertFalse(carol.isProtected());
    }

    @Test
    void nullsAndAForeignIdAreRefusedWithoutChangingTheSession() {
        GameTeam red = new GameTeam(Palette.RED);
        PlayerSession alice = session("Alice", 0);
        PlayerSession bob = session("Bob", 1);
        alice.joinTeam(red);

        assertThrows(NullPointerException.class, () -> alice.joinTeam(null));
        assertEquals(List.of(alice), red.members());

        assertThrows(NullPointerException.class, () -> alice.leave(null));
        assertTrue(alice.present());

        assertThrows(IllegalArgumentException.class, () -> alice.reactivate(new PlayerRef(bob.id(), "Someone")));
    }

    @Test
    void anEliminatedPlayerIsNotAliveUntilPlayingAgain() {
        PlayerSession alice = session("Alice", 0);

        alice.eliminate();
        assertFalse(alice.alive());
        assertEquals(Role.PLAYER, alice.role());

        alice.play();
        assertTrue(alice.alive());
    }

    @Test
    void anEliminationLastsThroughLeavingComingBackAndWatching() {
        PlayerSession alice = session("Alice", 0);
        alice.eliminate();

        alice.leave(LeaveReason.QUIT);
        assertTrue(alice.eliminated());
        alice.reactivate(new PlayerRef(alice.id(), "Alice"));
        assertTrue(alice.eliminated());
        alice.spectate();

        assertTrue(alice.eliminated());
        assertFalse(alice.alive());
        assertEquals(Role.SPECTATOR, alice.role());
    }

    @Test
    void spawnProtectionCountsDownBySecond() {
        PlayerSession alice = session("Alice", 0);
        alice.protectFor(2);

        assertTrue(alice.isProtected());
        alice.tickProtection();
        assertTrue(alice.isProtected());
        alice.tickProtection();
        assertFalse(alice.isProtected());
    }

    @Test
    void teamScoresAddUp() {
        GameTeam red = new GameTeam(Palette.RED);
        red.addScore(2);
        red.addScore(3);

        assertEquals(5, red.score());
        assertEquals("red", red.id());
    }
}
