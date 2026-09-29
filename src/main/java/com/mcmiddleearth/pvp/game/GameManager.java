package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.team.TeamLayout;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Opens, tracks and closes games, and is the only way into and out of them: joins, spectating, leaving, quits and
 * kicks all go through here, so a player is in at most one game. Arenas are reserved by physical space.
 */
public final class GameManager {

    private final ArenaRegistry arenas;
    private final ModeRegistry modes;
    private final PvpConfig config;
    private final GameContextFactory contexts;
    private final GameListener listener;
    private final ToDoubleFunction<UUID> skillOf;
    private final List<Game> games = new ArrayList<>();
    private final Map<UUID, Game> gameOfPlayer = new HashMap<>();

    public GameManager(ArenaRegistry arenas, ModeRegistry modes, PvpConfig config, GameContextFactory contexts,
                       GameListener listener, ToDoubleFunction<UUID> skillOf) {
        this.arenas = arenas;
        this.modes = modes;
        this.config = config;
        this.contexts = contexts;
        this.listener = listener;
        this.skillOf = skillOf;
    }

    /**
     * Opens a game in WAITING on the arena.
     *
     * @throws PvpException for an unknown, unplayable or blocked arena, more teams than the arena has spawns for,
     *                      invalid settings, too few spawns, or a max players too small for the teams it needs
     */
    public Game open(String arenaId, Map<String, ?> settings, String openedBy) {
        Arena arena = arenas.get(arenaId)
                .orElseThrow(() -> new PvpException(Msg.ERROR_UNKNOWN_ARENA, Map.of("arena", arenaId)));
        GameMode mode = modeOf(arena);
        Optional<Game> blocker = blockerFor(arena);
        if (blocker.isPresent()) {
            throw new PvpException(Msg.ERROR_ARENA_BLOCKED,
                    Map.of("arena", arena.id(), "holder", blocker.get().arena().id()));
        }
        refuseTeamsWithoutSpawns(arena, mode, settings);
        List<SettingSpec> specs = settingsFor(arena);
        GameSettings resolved = GameSettings.resolve(specs, settings, mode.id());
        refuseTooFewMaxPlayers(specs, mode, resolved);
        mode.rules().spawnPoints().validate(arena, mode.rules().teamCount(resolved));
        GameId id = GameId.random();
        GameContext context = contexts.create(id, arena);
        Game game;
        try {
            game = new Game(id, arena, mode, resolved, context, listener, config, skillOf, this::forget);
            game.begin();
        } catch (RuntimeException e) {
            // Nothing knows this game yet: give its context back, so no scoreboard or task is left behind.
            try {
                context.dispose();
            } catch (RuntimeException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
        games.add(game);
        listener.gameOpened(game, openedBy);
        return game;
    }

    /**
     * Joins the unit to the game. A member whose own game has ended and only shows its result leaves it first,
     * without the leave messages, once every check has passed, so a refused join never takes anyone out of it.
     *
     * @throws PvpException when a member is in a game that has not ended, the game no longer accepts joins, or a
     *                      waiting game has no room for the unit
     */
    public JoinResult join(JoinUnit unit, Game game) {
        unit.members().forEach(this::requireFree);
        requireJoinable(game);
        requireRoom(unit, game);
        unit.members().forEach(this::leaveEndedGame);
        unit.members().forEach(member -> gameOfPlayer.put(member.id(), game));
        try {
            return game.add(unit);
        } finally {
            forgetAbsent(unit.members(), game);
        }
    }

    /**
     * Makes the player a spectator of the game. A player whose own game has ended leaves it first, without the
     * leave messages, once the checks have passed.
     *
     * @throws PvpException when the player is in a game that has not ended, or the game no longer accepts joins
     */
    public void spectate(PlayerRef player, Game game) {
        requireFree(player);
        requireJoinable(game);
        leaveEndedGame(player);
        gameOfPlayer.put(player.id(), game);
        try {
            game.addSpectator(player);
        } finally {
            forgetAbsent(List.of(player), game);
        }
    }

    /**
     * The entries are recorded before the game takes the members in, so a listener that takes one out again is heard;
     * afterwards a member who isn't present (taken out again, or a join step failed before them) is forgotten again.
     * A member whose own join step failed is the exception: their session already exists and is present, so they
     * stay in the game, present and recorded, even though listeners were never told they joined. That is what lets
     * them still leave afterwards and have their partial state cleaned up, so never turn this into a rollback.
     */
    private void forgetAbsent(List<PlayerRef> members, Game game) {
        for (PlayerRef member : members) {
            if (game.session(member.id()).filter(PlayerSession::present).isEmpty()) {
                gameOfPlayer.remove(member.id(), game);
            }
        }
    }

    /** Takes the player out of their game (leave, quit, kick). False when they are not in a game. */
    public boolean leave(UUID player, LeaveReason reason) {
        Game game = gameOfPlayer.remove(player);
        if (game == null) {
            return false;
        }
        game.remove(player, reason);
        return true;
    }

    /** @throws PvpException when the game is not waiting or has too few players */
    public void startCountdown(Game game) {
        game.startCountdown();
    }

    /** Staff stop: a running game shows its result for endingSeconds first; any other game closes at once. */
    public void stop(Game game) {
        switch (game.phase()) {
            case WAITING, COUNTDOWN, ENDING -> game.close();
            case RUNNING -> game.end(Outcome.draw(EndReason.STOPPED));
            case CLOSED -> {
            }
        }
    }

    /**
     * Plugin disable: every running game gets its match record with {@link EndReason#SHUTDOWN}, then every game
     * closes at once, reading the same reason for a game that never ran and so never had an outcome. A game that
     * fails does not stop the others; the first failure is rethrown at the end.
     */
    public void closeAll() {
        List<Game> open = List.copyOf(games);
        RuntimeException failure = null;
        for (Game game : open) {
            try {
                if (game.phase() == Phase.RUNNING) {
                    game.end(Outcome.draw(EndReason.SHUTDOWN));
                }
            } catch (RuntimeException e) {
                failure = firstOf(failure, e);
            }
        }
        for (Game game : open) {
            try {
                game.close(LeaveReason.SHUTDOWN);
            } catch (RuntimeException e) {
                failure = firstOf(failure, e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static RuntimeException firstOf(RuntimeException first, RuntimeException next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    public Optional<Game> gameOf(UUID player) {
        return Optional.ofNullable(gameOfPlayer.get(player));
    }

    /** The open game on this arena, ignoring case. */
    public Optional<Game> gameOn(String arenaId) {
        return games.stream().filter(game -> game.arena().id().equalsIgnoreCase(arenaId)).findFirst();
    }

    /** Open games, oldest first. */
    public List<Game> games() {
        return List.copyOf(games);
    }

    /**
     * The first open game that holds the arena's space: the same arena, or one in the same world where either arena
     * has no region (it then reserves the whole world) or the regions overlap.
     */
    public Optional<Game> blockerFor(Arena arena) {
        return games.stream().filter(game -> blocks(game.arena(), arena)).findFirst();
    }

    /** @throws PvpException when the arena is not playable */
    public List<SettingSpec> settingsFor(Arena arena) {
        GameMode mode = modeOf(arena);
        return mode.rules().settingSpecs(mode.id(), arena, config);
    }

    /** The only game that accepts joins, when exactly one does (for {@code /pvp join} without an arena). */
    public Optional<Game> soleOpenGame() {
        List<Game> joinable = games.stream().filter(Game::acceptsJoins).toList();
        return joinable.size() == 1 ? Optional.of(joinable.get(0)) : Optional.empty();
    }

    private GameMode modeOf(Arena arena) {
        if (!arena.playable()) {
            String reason = arena.unassignedReason() == null ? "" : arena.unassignedReason();
            throw new PvpException(Msg.ERROR_ARENA_UNASSIGNED, Map.of("arena", arena.id(), "reason", reason));
        }
        return modes.get(arena.modeId()).orElseThrow(() -> new PvpException(Msg.ERROR_ARENA_UNASSIGNED,
                Map.of("arena", arena.id(), "reason", "unknown mode " + arena.modeId())));
    }

    /**
     * Too many teams for the arena: a team count the mode allows but the arena has no spawn sets
     * for gets error.arena-teams. The settings range stops at the arena's spawn sets (that is the menus' slider), so
     * without this check the refusal would be the range error "between 2 and 2". Counts outside the mode's own range
     * are left to the range error.
     */
    private static void refuseTeamsWithoutSpawns(Arena arena, GameMode mode, Map<String, ?> settings) {
        if (!(mode.rules().teams() instanceof TeamLayout.Configurable layout)) {
            return;
        }
        int supported = mode.rules().spawnPoints().supportedTeams(arena);
        for (Map.Entry<String, ?> entry : settings.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(Setting.TEAM_COUNT.id())) {
                continue;
            }
            OptionalLong count = GameSettings.wholeNumber(entry.getValue());
            if (count.isPresent() && count.getAsLong() > supported && count.getAsLong() <= layout.max()) {
                throw new PvpException(Msg.plural(Msg.ERROR_ARENA_TEAMS, Msg.ERROR_ARENA_TEAMS_ONE, supported),
                        Map.of("arena", arena.id(), "max", String.valueOf(supported)));
            }
        }
    }

    /**
     * A max players setting below one player per team could never start: the countdown always needs every team to have
     * a player, so it would stay refused forever and staff would have to stop the game by hand. Only checked when the
     * mode offers a max players setting; when even the arena's max players is too small, the team count is refused
     * instead (when the mode has one).
     */
    private static void refuseTooFewMaxPlayers(List<SettingSpec> specs, GameMode mode, GameSettings resolved) {
        Optional<SettingSpec> maxPlayers = spec(specs, Setting.MAX_PLAYERS);
        if (maxPlayers.isEmpty()) {
            return;
        }
        int minNeeded = mode.rules().minPlayers(mode.rules().teamCount(resolved));
        Optional<SettingSpec> teamCount = spec(specs, Setting.TEAM_COUNT);
        if (minNeeded > maxPlayers.get().max() && teamCount.isPresent()) {
            // No max players setting could hold one player per team: the team count is what has to change.
            throw new PvpException(Msg.ERROR_SETTING_RANGE, Map.of("setting", Setting.TEAM_COUNT.id(),
                    "min", String.valueOf(teamCount.get().min()), "max", String.valueOf(maxPlayers.get().max())));
        }
        if (resolved.get(Setting.MAX_PLAYERS) < minNeeded) {
            throw new PvpException(Msg.ERROR_SETTING_RANGE, Map.of("setting", Setting.MAX_PLAYERS.id(),
                    "min", String.valueOf(minNeeded), "max", String.valueOf(maxPlayers.get().max())));
        }
    }

    private static Optional<SettingSpec> spec(List<SettingSpec> specs, Setting setting) {
        return specs.stream().filter(s -> s.setting().equals(setting)).findFirst();
    }

    private static boolean blocks(Arena held, Arena wanted) {
        if (held.id().equalsIgnoreCase(wanted.id())) {
            return true;
        }
        if (!held.world().equalsIgnoreCase(wanted.world())) {
            return false;
        }
        return held.region() == null || wanted.region() == null || held.region().overlaps(wanted.region());
    }

    /** Free to join: in no game, or in one that has ended and only shows its result until it closes. */
    private void requireFree(PlayerRef player) {
        Game current = gameOfPlayer.get(player.id());
        if (current != null && current.phase() != Phase.ENDING) {
            throw new PvpException(Msg.ERROR_ALREADY_IN_GAME, Map.of("player", player.name()));
        }
    }

    private void requireJoinable(Game game) {
        if (!games.contains(game) || !game.acceptsJoins()) {
            throw new PvpException(Msg.ERROR_GAME_ENDED);
        }
    }

    /** The one refusal of joining that the other checks leave to the game itself: a waiting game without room. */
    private static void requireRoom(JoinUnit unit, Game game) {
        if (game.phase() == Phase.WAITING && !game.canJoinAsPlayer(unit.size())) {
            throw new PvpException(Msg.ERROR_GAME_FULL, Map.of("max", String.valueOf(game.maxPlayers())));
        }
    }

    /**
     * Takes the player out of a game that has ended, without the leave messages: its result and stats are already
     * recorded, and its other players are still reading the summary.
     */
    private void leaveEndedGame(PlayerRef player) {
        leave(player.id(), LeaveReason.GAME_CLOSED);
    }

    /** When a game closes, it and every player entry pointing to it are forgotten. */
    private void forget(Game game) {
        games.remove(game);
        gameOfPlayer.values().removeIf(game::equals);
    }
}
