package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeLogic;
import com.mcmiddleearth.pvp.mode.ModeRules;
import com.mcmiddleearth.pvp.mode.loadout.Kits;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A Free For All variant for platform tests, played on {@code TestArenas.ffa(...).withMode(id)} arenas: its own id,
 * optional spawn protection, optionally no chest restock, and a log of the interactions every game playing this mode
 * instance hands it.
 */
public final class ProbeMode implements GameMode {

    private final String id;
    private final ModeRules rules;
    private final List<String> interactions = new ArrayList<>();

    public ProbeMode(String id, int spawnProtectionSeconds, boolean chestRestock) {
        this.id = id;
        ModeRules.Builder builder = ModeRules.builder()
                .noTeams()
                .spawnPoints(SpawnPoints.numbered("PlayerSpawn"))
                .loadout(LoadoutPolicy.fixed(Kits.STANDARD))
                .respawn(Respawn.AT_SPAWN)
                .midgameJoin(MidgameJoin.JOIN)
                .win(WinConditions.highestAt(Setting.TIME_LIMIT))
                .killScore(1)
                .spawnProtection(spawnProtectionSeconds);
        if (!chestRestock) {
            builder = builder.noChestRestock();
        }
        this.rules = builder.build();
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
        return Optional.of(new ModeLogic() {
            @Override
            public void onInteract(Game game, PlayerSession player, NamedPoint block) {
                interactions.add(player.name() + " " + block.world() + " " + (int) block.x() + " " + (int) block.y()
                        + " " + (int) block.z());
            }
        });
    }

    /** Every interaction handed to this mode instance, by any game that plays it. */
    public List<String> interactions() {
        return interactions;
    }
}
