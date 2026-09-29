package com.mcmiddleearth.pvp.mode.tdm;

import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.team.TeamLayout;

import java.util.Set;

/** Two to four teams, no respawns; the last team with players left wins. */
public final class TeamDeathmatch implements GameMode {

    private final ModeRules rules = ModeRules.builder()
            .teams(TeamLayout.configurable(2, 4)).balanced()
            .spawnPoints(SpawnPoints.perTeam())
            .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
            .respawn(Respawn.NEVER)
            .midgameJoin(MidgameJoin.SMALLEST_TEAM_WHILE_FULL)
            .win(WinConditions.lastTeamStanding())
            .build();

    @Override
    public String id() {
        return "tdm";
    }

    @Override
    public Set<String> legacyNames() {
        return Set.of("TeamDeathmatch");
    }

    @Override
    public ModeRules rules() {
        return rules;
    }
}
