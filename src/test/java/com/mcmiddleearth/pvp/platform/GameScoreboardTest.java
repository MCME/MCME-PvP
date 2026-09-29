package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.team.Palette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameScoreboardTest {

    private ServerMock server;
    private Scoreboard main;
    private GameScoreboard scoreboard;
    private PlayerMock alice;
    private PlayerMock bob;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        main = server.getScoreboardManager().getMainScoreboard();
        scoreboard = new GameScoreboard(server.getScoreboardManager());
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void addShowsTheGameBoardAndPutsThePlayerInTheTeamOfTheirColor() {
        scoreboard.add(alice, Palette.RED);

        assertSame(scoreboard.board(), alice.getScoreboard());
        assertNotSame(main, scoreboard.board());
        Team red = scoreboard.board().getEntryTeam("Alice");
        assertEquals("red", red.getName());
        assertEquals(NamedTextColor.RED, red.color());
        assertSame(main, bob.getScoreboard());
        assertNull(main.getEntryTeam("Alice"));
    }

    @Test
    void realTeamsBlockFriendlyFireAndOnlyCollideWithOtherTeams() {
        scoreboard.add(alice, Palette.BLUE);

        Team blue = scoreboard.board().getTeam("blue");
        assertFalse(blue.allowFriendlyFire());
        assertEquals(Team.OptionStatus.FOR_OTHER_TEAMS, blue.getOption(Team.Option.COLLISION_RULE));
        assertTrue(blue.canSeeFriendlyInvisibles());
    }

    @Test
    void personalColorsOnlyColorTheName() {
        scoreboard.add(alice, Palette.PERSONAL.get(0));

        Team personal = scoreboard.board().getTeam("p-aqua");
        assertEquals(NamedTextColor.AQUA, personal.color());
        assertTrue(personal.allowFriendlyFire());
        assertEquals(Team.OptionStatus.ALWAYS, personal.getOption(Team.Option.COLLISION_RULE));
        assertFalse(personal.canSeeFriendlyInvisibles());
    }

    @Test
    void aNewColorMovesThePlayerAndNoColorMeansNoTeam() {
        scoreboard.add(alice, Palette.RED);
        scoreboard.add(alice, Palette.BLUE);

        assertEquals("blue", scoreboard.board().getEntryTeam("Alice").getName());
        assertFalse(scoreboard.board().getTeam("red").hasEntry("Alice"));

        scoreboard.add(alice, null);

        assertNull(scoreboard.board().getEntryTeam("Alice"));
        assertSame(scoreboard.board(), alice.getScoreboard());
    }

    @Test
    void removeReturnsThePlayerToTheMainScoreboard() {
        scoreboard.add(alice, Palette.RED);

        scoreboard.remove(alice);

        assertSame(main, alice.getScoreboard());
        assertNull(scoreboard.board().getEntryTeam("Alice"));
    }

    @Test
    void removeLeavesAPlayerWhoAlreadyWatchesAnotherGame() {
        GameScoreboard other = new GameScoreboard(server.getScoreboardManager());
        scoreboard.add(alice, Palette.RED);
        other.add(alice, Palette.RED);

        scoreboard.remove(alice);

        assertSame(other.board(), alice.getScoreboard());
        assertEquals("red", other.board().getEntryTeam("Alice").getName());
    }

    @Test
    void disposeUnregistersTeamsAndObjectives() {
        scoreboard.add(alice, Palette.RED);
        scoreboard.board().registerNewObjective("sidebar", Criteria.DUMMY, Component.text("PvP"));

        scoreboard.dispose();

        assertTrue(scoreboard.board().getTeams().isEmpty());
        assertTrue(scoreboard.board().getObjectives().isEmpty());
        assertSame(main, alice.getScoreboard());
    }

    @Test
    void disposeLeavesAPlayerWhoAlreadyWatchesAnotherGame() {
        GameScoreboard other = new GameScoreboard(server.getScoreboardManager());
        scoreboard.add(alice, Palette.RED);
        other.add(alice, Palette.RED);

        scoreboard.dispose();

        assertSame(other.board(), alice.getScoreboard());
        assertEquals("red", other.board().getEntryTeam("Alice").getName());
    }
}
