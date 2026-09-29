package com.mcmiddleearth.pvp.mode;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.BoolSetting;
import com.mcmiddleearth.pvp.game.GameSettings;
import com.mcmiddleearth.pvp.game.IntSetting;
import com.mcmiddleearth.pvp.game.Setting;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.loadout.LoadoutPolicy;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.mode.rules.Respawn;
import com.mcmiddleearth.pvp.mode.rules.SpawnPoints;
import com.mcmiddleearth.pvp.mode.rules.WinCondition;
import com.mcmiddleearth.pvp.team.TeamLayout;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What a mode declares: teams, spawns, kits, respawns, late joining and how it is won. The settings a mode offers
 * are derived from these declarations, so menus and commands can never offer a setting the mode does not use.
 */
public final class ModeRules {

    private final TeamLayout teams;
    private final boolean balanced;
    private final SpawnPoints spawnPoints;
    private final LoadoutPolicy loadout;
    private final Respawn respawn;
    private final MidgameJoin midgameJoin;
    private final WinCondition win;
    private final int killScore;
    private final int spawnProtectionSeconds;
    private final boolean chestRestock;

    private ModeRules(Builder builder) {
        this.teams = builder.teams;
        this.balanced = builder.balanced;
        this.spawnPoints = builder.spawnPoints;
        this.loadout = builder.loadout;
        this.respawn = builder.respawn;
        this.midgameJoin = builder.midgameJoin;
        this.win = builder.win;
        this.killScore = builder.killScore;
        this.spawnProtectionSeconds = builder.spawnProtectionSeconds;
        this.chestRestock = builder.chestRestock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TeamLayout teams() {
        return teams;
    }

    /** Teams are balanced by party size and skill at the countdown; otherwise units go round-robin. */
    public boolean balanced() {
        return balanced;
    }

    public SpawnPoints spawnPoints() {
        return spawnPoints;
    }

    public LoadoutPolicy loadout() {
        return loadout;
    }

    public Respawn respawn() {
        return respawn;
    }

    public MidgameJoin midgameJoin() {
        return midgameJoin;
    }

    public WinCondition win() {
        return win;
    }

    /** Points per kill, default 0. */
    public int killScore() {
        return killScore;
    }

    /**
     * Seconds of spawn protection after a respawn, default 0. The game's once-a-second heartbeat counts them
     * down, so N seconds last between N−1 and N.
     */
    public int spawnProtectionSeconds() {
        return spawnProtectionSeconds;
    }

    /** Default true: clicking a chest refills arrows (1.1.2). */
    public boolean chestRestock() {
        return chestRestock;
    }

    /**
     * The settings this mode offers, in {@link Setting#ALL} order: TEAM_COUNT for a configurable team layout,
     * TIME_LIMIT and SCORE_GOAL when the win condition reads them, and always MAX_PLAYERS and MIDGAME_JOIN.
     */
    public List<Setting> settings() {
        return Setting.ALL.stream().filter(this::offers).toList();
    }

    /** Configurable: the TEAM_COUNT setting; Fixed: its count; None: 0. */
    public int teamCount(GameSettings settings) {
        return switch (teams) {
            case TeamLayout.None none -> 0;
            case TeamLayout.Fixed fixed -> fixed.count();
            case TeamLayout.Configurable range ->
                    settings.has(Setting.TEAM_COUNT) ? settings.get(Setting.TEAM_COUNT) : range.min();
        };
    }

    /** With teams: one player per team; without: 2. */
    public int minPlayers(int teamCount) {
        return teams.hasTeams() ? teamCount : 2;
    }

    /**
     * One spec per setting of {@link #settings()}, with this arena's range and a default from config.yml
     * ({@code modes.<modeId>.<setting>}), clamped into the range.
     */
    public List<SettingSpec> settingSpecs(String modeId, Arena arena, PvpConfig config) {
        return settings().stream().map(setting -> spec(setting, modeId, arena, config)).toList();
    }

    private boolean offers(Setting setting) {
        if (setting.equals(Setting.TEAM_COUNT)) {
            return teams instanceof TeamLayout.Configurable;
        }
        if (setting.equals(Setting.TIME_LIMIT) || setting.equals(Setting.SCORE_GOAL)) {
            return win.settings().contains(setting);
        }
        return setting.equals(Setting.MAX_PLAYERS) || setting.equals(Setting.MIDGAME_JOIN);
    }

    private SettingSpec spec(Setting setting, String modeId, Arena arena, PvpConfig config) {
        Optional<Object> configured = config.modeDefault(modeId, setting.id());
        if (setting instanceof BoolSetting) {
            return new SettingSpec(setting, 0, 0, 0, configured.flatMap(ModeRules::asBoolean).orElse(true));
        }
        IntSetting number = (IntSetting) setting;
        int min = number.min();
        int max = number.max();
        int fallback = number.min();
        if (setting.equals(Setting.TEAM_COUNT) && teams instanceof TeamLayout.Configurable range) {
            min = range.min();
            max = Math.max(range.min(), Math.min(range.max(), spawnPoints.supportedTeams(arena)));
            fallback = range.min();
        } else if (setting.equals(Setting.TIME_LIMIT)) {
            fallback = 10;
        } else if (setting.equals(Setting.SCORE_GOAL)) {
            fallback = 0;
        } else if (setting.equals(Setting.MAX_PLAYERS)) {
            max = arena.maxPlayers() > 0 ? Math.max(2, arena.maxPlayers()) : 100;
            fallback = arena.maxPlayers() > 0 ? arena.maxPlayers() : 16;
        }
        int value = configured.flatMap(ModeRules::asInt).orElse(fallback);
        return new SettingSpec(setting, min, max, number.step(), Math.max(min, Math.min(max, value)));
    }

    private static Optional<Integer> asInt(Object value) {
        if (value instanceof Number number) {
            long rounded = Math.round(number.doubleValue());
            return Optional.of((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded)));
        }
        try {
            return value instanceof String text ? Optional.of(Integer.parseInt(text.trim())) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> asBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return Optional.of(flag);
        }
        String word = value instanceof String text ? text.trim().toLowerCase(Locale.ROOT) : "";
        return switch (word) {
            case "true", "yes", "on" -> Optional.of(true);
            case "false", "no", "off" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    /** Collects a mode's declarations; {@link #build()} checks that the required ones are there. */
    public static final class Builder {

        private TeamLayout teams = TeamLayout.none();
        private boolean balanced;
        private SpawnPoints spawnPoints;
        private LoadoutPolicy loadout;
        private Respawn respawn;
        private MidgameJoin midgameJoin;
        private WinCondition win;
        private int killScore;
        private int spawnProtectionSeconds;
        private boolean chestRestock = true;

        private Builder() {
        }

        public Builder teams(TeamLayout layout) {
            this.teams = layout;
            return this;
        }

        public Builder balanced() {
            this.balanced = true;
            return this;
        }

        /** {@code teams(TeamLayout.none())}. */
        public Builder noTeams() {
            return teams(TeamLayout.none());
        }

        public Builder spawnPoints(SpawnPoints points) {
            this.spawnPoints = points;
            return this;
        }

        public Builder loadout(LoadoutPolicy policy) {
            this.loadout = policy;
            return this;
        }

        public Builder respawn(Respawn respawn) {
            this.respawn = respawn;
            return this;
        }

        public Builder midgameJoin(MidgameJoin policy) {
            this.midgameJoin = policy;
            return this;
        }

        public Builder win(WinCondition win) {
            this.win = win;
            return this;
        }

        public Builder killScore(int points) {
            this.killScore = points;
            return this;
        }

        public Builder spawnProtection(int seconds) {
            this.spawnProtectionSeconds = seconds;
            return this;
        }

        public Builder noChestRestock() {
            this.chestRestock = false;
            return this;
        }

        /** @throws IllegalStateException when spawnPoints, loadout, respawn, midgameJoin or win is missing */
        public ModeRules build() {
            require(spawnPoints, "spawnPoints");
            require(loadout, "loadout");
            require(respawn, "respawn");
            require(midgameJoin, "midgameJoin");
            require(win, "win");
            return new ModeRules(this);
        }

        private static void require(Object value, String name) {
            if (value == null) {
                throw new IllegalStateException("ModeRules needs " + name);
            }
        }
    }
}
