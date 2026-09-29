package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeLogic;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;
import com.mcmiddleearth.pvp.team.TeamLayout;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** A mode with any rules and optional hooks, for engine tests. Arenas get it with {@code arena.withMode(id)}. */
public final class TestMode implements GameMode {

    private final String id;
    private final ModeRules rules;
    private final Supplier<? extends ModeLogic> logic;

    public TestMode(String id, ModeRules rules) {
        this(id, rules, null);
    }

    /** {@code logic} makes one hook object per game; null for none. */
    public TestMode(String id, ModeRules rules, Supplier<? extends ModeLogic> logic) {
        this.id = id;
        this.rules = rules;
        this.logic = logic;
    }

    /**
     * Two fixed teams on the arena's team spawns, the standard kit, no respawns, last team standing, and late
     * joiners spectate.
     */
    public static ModeRules.Builder twoTeams() {
        return ModeRules.builder()
                .teams(TeamLayout.fixed(2))
                .spawnPoints(SpawnPoints.perTeam())
                .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
                .respawn(Respawn.NEVER)
                .midgameJoin(MidgameJoin.SPECTATE)
                .win(WinConditions.lastTeamStanding());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Set<String> legacyNames() {
        return Set.of();
    }

    @Override
    public ModeRules rules() {
        return rules;
    }

    @Override
    public Optional<ModeLogic> newLogic() {
        return logic == null ? Optional.empty() : Optional.of(logic.get());
    }
}
