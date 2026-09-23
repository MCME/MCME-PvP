package com.mcmiddleearth.mcme.pvp.Util;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScoreboardTeamsTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        scoreboard = server.getScoreboardManager().getNewScoreboard();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registersAMissingTeam() {
        Team team = ScoreboardTeams.getOrRegister(scoreboard, "collision");
        assertNotNull(team);
        assertSame(team, scoreboard.getTeam("collision"));
    }

    @Test
    void reusesATeamThatAlreadyExists() {
        Team existing = scoreboard.registerNewTeam("collision");
        assertSame(existing, ScoreboardTeams.getOrRegister(scoreboard, "collision"));
    }
}
