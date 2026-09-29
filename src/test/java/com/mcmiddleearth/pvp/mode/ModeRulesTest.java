package com.mcmiddleearth.pvp.mode;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.GameSettings;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinCondition;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.team.TeamLayout;
import com.mcmiddleearth.pvp.testing.TestArenas;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeRulesTest {

    /** A win condition with a time limit, standing in for highestAt. */
    private static final WinCondition TIMED = new WinCondition() {
        @Override
        public Optional<Outcome> evaluate(com.mcmiddleearth.pvp.game.Game game) {
            return Optional.empty();
        }

        @Override
        public boolean usesTimeLimit() {
            return true;
        }

        @Override
        public List<Setting> settings() {
            return List.of(Setting.TIME_LIMIT);
        }
    };

    private static ModeRules.Builder complete() {
        return ModeRules.builder()
                .spawnPoints(SpawnPoints.perTeam())
                .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
                .respawn(Respawn.NEVER)
                .midgameJoin(MidgameJoin.JOIN)
                .win(WinConditions.lastTeamStanding());
    }

    private static PvpConfig config(Map<String, Object> tdmDefaults) {
        PvpConfig d = PvpConfig.defaults();
        return new PvpConfig(d.lobby(), d.worlds(), d.mainServer(), d.startLocked(), d.lavaDamage(),
                d.lobbyProtection(), 7, 10, Map.of("tdm", tdmDefaults));
    }

    private static SettingSpec spec(List<SettingSpec> specs, Setting setting) {
        return specs.stream().filter(s -> s.setting().equals(setting)).findFirst().orElseThrow();
    }

    private static Arena withMaxPlayers(Arena arena, int max) {
        return new Arena(arena.schemaVersion(), arena.id(), arena.modeId(), null, arena.title(), max, null,
                arena.spawn(), arena.points(), arena.teamSpawns(), arena.region(), null);
    }

    @Test
    void buildNeedsSpawnsLoadoutRespawnLateJoinsAndAWinCondition() {
        assertThrows(IllegalStateException.class, () -> complete().spawnPoints(null).build());
        assertThrows(IllegalStateException.class, () -> complete().loadout(null).build());
        assertThrows(IllegalStateException.class, () -> complete().respawn(null).build());
        assertThrows(IllegalStateException.class, () -> complete().midgameJoin(null).build());
        assertThrows(IllegalStateException.class, () -> complete().win(null).build());
    }

    @Test
    void optionalDeclarationsHaveDefaults() {
        ModeRules rules = complete().build();

        assertEquals(TeamLayout.none(), rules.teams());
        assertFalse(rules.balanced());
        assertEquals(0, rules.killScore());
        assertEquals(0, rules.spawnProtectionSeconds());
        assertTrue(rules.chestRestock());

        ModeRules custom = complete().teams(TeamLayout.fixed(2)).balanced().killScore(2).spawnProtection(3)
                .noChestRestock().build();
        assertEquals(TeamLayout.fixed(2), custom.teams());
        assertTrue(custom.balanced());
        assertEquals(2, custom.killScore());
        assertEquals(3, custom.spawnProtectionSeconds());
        assertFalse(custom.chestRestock());
    }

    @Test
    void theSettingsFollowTheDeclarations() {
        assertEquals(List.of(Setting.TEAM_COUNT, Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN),
                complete().teams(TeamLayout.configurable(2, 4)).build().settings());
        assertEquals(List.of(Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN),
                complete().teams(TeamLayout.fixed(2)).build().settings());
        assertEquals(List.of(Setting.TIME_LIMIT, Setting.SCORE_GOAL, Setting.MAX_PLAYERS, Setting.MIDGAME_JOIN),
                complete().noTeams().win(TIMED.orFirstTo(Setting.SCORE_GOAL)).build().settings());
    }

    @Test
    void theTeamCountComesFromTheLayout() {
        ModeRules configurable = complete().teams(TeamLayout.configurable(2, 4)).build();
        List<SettingSpec> specs = configurable.settingSpecs("tdm", TestArenas.tdm("A", 0, 4), PvpConfig.defaults());
        GameSettings three = GameSettings.resolve(specs, Map.of("teamCount", 3), "tdm");

        assertEquals(3, configurable.teamCount(three));
        assertEquals(2, complete().teams(TeamLayout.fixed(2)).build().teamCount(three));
        assertEquals(0, complete().noTeams().build().teamCount(three));
    }

    @Test
    void minPlayersIsOnePerTeamOrTwoWithoutTeams() {
        assertEquals(3, complete().teams(TeamLayout.configurable(2, 4)).build().minPlayers(3));
        assertEquals(2, complete().noTeams().build().minPlayers(0));
    }

    @Test
    void theTeamCountRangeStopsAtTheArenasSpawnSets() {
        ModeRules rules = complete().teams(TeamLayout.configurable(2, 4)).build();

        PvpConfig config = PvpConfig.defaults();
        SettingSpec two = spec(rules.settingSpecs("tdm", TestArenas.tdm("A", 0, 2), config), Setting.TEAM_COUNT);
        SettingSpec six = spec(rules.settingSpecs("tdm", TestArenas.tdm("B", 0, 6), config), Setting.TEAM_COUNT);

        assertEquals(new SettingSpec(Setting.TEAM_COUNT, 2, 2, 1, 2), two);
        assertTrue(two.fixed());
        assertEquals(new SettingSpec(Setting.TEAM_COUNT, 2, 4, 1, 2), six);
        assertFalse(six.fixed());
    }

    @Test
    void defaultsComeFromConfigAndAreClampedIntoTheRange() {
        ModeRules rules = complete().teams(TeamLayout.configurable(2, 4)).build();
        PvpConfig config = config(Map.of("teamCount", 3, "maxPlayers", 40, "midgameJoin", false));

        List<SettingSpec> big = rules.settingSpecs("tdm", TestArenas.tdm("B", 0, 4), config);
        List<SettingSpec> small = rules.settingSpecs("tdm", TestArenas.tdm("A", 0, 2), config);

        assertEquals(3, spec(big, Setting.TEAM_COUNT).defaultValue());
        assertEquals(2, spec(small, Setting.TEAM_COUNT).defaultValue());
        assertEquals(new SettingSpec(Setting.MAX_PLAYERS, 2, 16, 1, 16), spec(big, Setting.MAX_PLAYERS));
        assertEquals(new SettingSpec(Setting.MIDGAME_JOIN, 0, 0, 0, false), spec(big, Setting.MIDGAME_JOIN));
    }

    @Test
    void aConfigDefaultTooBigForAnIntEndsAtTheTopOfTheRange() {
        ModeRules rules = complete().teams(TeamLayout.configurable(2, 4)).build();
        PvpConfig config = config(Map.of("teamCount", 4294967297L, "maxPlayers", 4294967297L));

        List<SettingSpec> specs = rules.settingSpecs("tdm", TestArenas.tdm("B", 0, 4), config);

        SettingSpec teamCount = spec(specs, Setting.TEAM_COUNT);
        SettingSpec maxPlayers = spec(specs, Setting.MAX_PLAYERS);
        assertEquals(teamCount.max(), teamCount.defaultValue());
        assertEquals(maxPlayers.max(), maxPlayers.defaultValue());
    }

    @Test
    void unusableConfigValuesFallBackToTheBuiltInDefault() {
        ModeRules rules = complete().teams(TeamLayout.configurable(2, 4)).build();
        PvpConfig config = config(Map.of("teamCount", "lots", "midgameJoin", "maybe"));

        List<SettingSpec> specs = rules.settingSpecs("tdm", TestArenas.tdm("B", 0, 4), config);

        assertEquals(2, spec(specs, Setting.TEAM_COUNT).defaultValue());
        assertEquals(true, spec(specs, Setting.MIDGAME_JOIN).defaultValue());
    }

    @Test
    void maxPlayersFollowsTheArena() {
        ModeRules rules = complete().build();
        Arena arena = TestArenas.tdm("A", 0, 2);

        assertEquals(new SettingSpec(Setting.MAX_PLAYERS, 2, 16, 1, 16),
                spec(rules.settingSpecs("x", arena, PvpConfig.defaults()), Setting.MAX_PLAYERS));
        assertEquals(new SettingSpec(Setting.MAX_PLAYERS, 2, 100, 1, 16),
                spec(rules.settingSpecs("x", withMaxPlayers(arena, 0), PvpConfig.defaults()), Setting.MAX_PLAYERS));
        assertEquals(new SettingSpec(Setting.MAX_PLAYERS, 2, 2, 1, 2),
                spec(rules.settingSpecs("x", withMaxPlayers(arena, 1), PvpConfig.defaults()), Setting.MAX_PLAYERS));
    }

    @Test
    void timeLimitAndScoreGoalHaveTheirOwnRangesAndDefaults() {
        ModeRules rules = complete().noTeams().win(TIMED.orFirstTo(Setting.SCORE_GOAL)).build();
        Arena arena = TestArenas.ffa("F", 0, 2);

        List<SettingSpec> builtIn = rules.settingSpecs("tdm", arena, config(Map.of()));
        assertEquals(new SettingSpec(Setting.TIME_LIMIT, 1, 60, 1, 10), spec(builtIn, Setting.TIME_LIMIT));
        assertEquals(new SettingSpec(Setting.SCORE_GOAL, 0, 100, 1, 0), spec(builtIn, Setting.SCORE_GOAL));

        PvpConfig config = config(Map.of("timeLimit", 90, "scoreGoal", "7"));
        List<SettingSpec> configured = rules.settingSpecs("tdm", arena, config);
        assertEquals(60, spec(configured, Setting.TIME_LIMIT).defaultValue());
        assertEquals(7, spec(configured, Setting.SCORE_GOAL).defaultValue());
    }
}
