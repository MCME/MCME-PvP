package com.mcmiddleearth.pvp.mode.ffa;

import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;

import java.util.Set;

/** Everyone for themselves with respawns; +1 per kill; highest score at the time limit (or first to the goal) wins. */
public final class FreeForAll implements GameMode {

    private final ModeRules rules = ModeRules.builder()
            .noTeams()
            .spawnPoints(SpawnPoints.numbered("PlayerSpawn"))
            .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
            .respawn(Respawn.AT_SPAWN)
            .midgameJoin(MidgameJoin.JOIN)
            .win(WinConditions.highestAt(Setting.TIME_LIMIT).orFirstTo(Setting.SCORE_GOAL))
            .killScore(1)
            .build();

    @Override
    public String id() {
        return "ffa";
    }

    @Override
    public Set<String> legacyNames() {
        return Set.of("FreeForAll");
    }

    @Override
    public ModeRules rules() {
        return rules;
    }
}
