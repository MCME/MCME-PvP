package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.mode.rules.MidgameJoin;
import com.mcmiddleearth.pvp.team.TeamSpec;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Joining, spectating and leaving one game, and placing its players at the countdown.
 */
final class Joins {

    private final Game game;
    private final Roster roster;
    private final TeamPlacement placement;
    private final SpawnCursor spawns;

    Joins(Game game, Roster roster, TeamPlacement placement, SpawnCursor spawns) {
        this.game = game;
        this.roster = roster;
        this.placement = placement;
        this.spawns = spawns;
    }

    JoinResult add(JoinUnit unit) {
        return switch (game.phase()) {
            case WAITING -> addWaiting(unit);
            case COUNTDOWN, RUNNING -> addMidgame(unit);
            case ENDING, CLOSED -> throw new PvpException(Msg.ERROR_GAME_ENDED);
        };
    }

    void addSpectator(PlayerRef player) {
        if (game.phase() == Phase.ENDING || game.phase() == Phase.CLOSED) {
            throw new PvpException(Msg.ERROR_GAME_ENDED);
        }
        spectate(player);
    }

    boolean canJoinAsPlayer(int unitSize) {
        return switch (game.phase()) {
            case WAITING -> hasRoom(unitSize);
            case COUNTDOWN, RUNNING -> playsMidgame(unitSize);
            case ENDING, CLOSED -> false;
        };
    }

    /**
     * Takes the player out and sends them to the lobby, without the phase follow-up (the game does that). Every
     * platform call runs even when an earlier one throws, and onLeave and playerLeft fire even when a platform
     * step fails; the first failure is rethrown at the end, so a player whose client cannot be reached still
     * finishes leaving on the game's side.
     */
    void remove(PlayerSession session, LeaveReason reason) {
        UUID player = session.id();
        session.leave(reason);
        roster.leaveUnit(player);
        // Also for QUIT: Paper still has the player object during the quit event.
        PlayerActions players = game.context().players();
        RuntimeException failure = null;
        failure = Steps.attempt(failure, () -> players.setSpectator(player, false));
        failure = Steps.attempt(failure, () -> players.leaveScoreboard(player));
        failure = Steps.attempt(failure, () -> players.sendToLobby(player));
        game.logic().onLeave(game, session);
        game.listener().playerLeft(game, session, reason);
        if (failure != null) {
            throw failure;
        }
    }

    /** At the countdown: teams (or personal colors) for everyone waiting, then each player to a spawn with a kit. */
    void placeForCountdown() {
        placement.placeAll(roster.playingUnits(), roster.players());
        for (PlayerSession session : roster.players()) {
            session.markParticipated();
            equip(session);
        }
    }

    /**
     * A cancelled countdown: teams and colors are gone; every remaining player is put back as joining while WAITING
     * leaves them.
     */
    void clearPlacement() {
        placement.clear(roster.all());
        roster.players().forEach(session -> resetToWaiting(session.id()));
    }

    private JoinResult addWaiting(JoinUnit unit) {
        if (!hasRoom(unit.size())) {
            throw new PvpException(Msg.ERROR_GAME_FULL, Map.of("max", String.valueOf(game.maxPlayers())));
        }
        List<PlayerSession> joined = new ArrayList<>();
        for (PlayerRef member : unit.members()) {
            PlayerSession session = roster.activate(member);
            session.play();
            resetToWaiting(member.id());
            joined.add(session);
        }
        roster.addUnit(unit);
        joined.forEach(session -> game.listener().playerJoined(game, session, JoinResult.PLAYING));
        return JoinResult.PLAYING;
    }

    /**
     * Where joining while WAITING puts one member (the arena spawn, reset, on the board without a color), also used to
     * put a player back when a countdown is cancelled.
     */
    private void resetToWaiting(UUID player) {
        PlayerActions players = game.context().players();
        players.teleport(player, game.arena().spawn());
        players.resetForPlay(player);
        players.joinScoreboard(player, null);
    }

    /**
     * A unit joining during the countdown or a running game. A member eliminated from this game earlier only comes
     * back to watch, whatever {@link #playsMidgame} says: elimination is final. The others play on the smallest team
     * when {@link #playsMidgame} allows it, otherwise they spectate too.
     */
    private JoinResult addMidgame(JoinUnit unit) {
        List<PlayerRef> joining = new ArrayList<>();
        for (PlayerRef member : unit.members()) {
            if (roster.session(member.id()).filter(PlayerSession::eliminated).isPresent()) {
                spectate(member);
            } else {
                joining.add(member);
            }
        }
        if (joining.isEmpty() || !playsMidgame(joining.size())) {
            joining.forEach(this::spectate);
            return JoinResult.SPECTATING;
        }
        // Every member who plays goes to the team that is smallest now, so a party stays together.
        Optional<GameTeam> team = placement.midgameTeam();
        List<PlayerSession> joined = new ArrayList<>();
        for (PlayerRef member : joining) {
            PlayerSession session = roster.activate(member);
            session.play();
            session.markParticipated();
            team.ifPresentOrElse(session::joinTeam, () -> {
                // A player who left and came back keeps the personal color they had, as 1.1.2 did.
                if (session.color().isEmpty()) {
                    session.personalColor(placement.nextPersonalColor());
                }
            });
            equip(session);
            joined.add(session);
        }
        // Remembered too, so the party stays together if the countdown is cancelled and started again.
        roster.addUnit(unit);
        joined.forEach(session -> game.listener().playerJoined(game, session, JoinResult.PLAYING));
        return JoinResult.PLAYING;
    }

    /**
     * Whether a unit joining now plays: there is room, and the mode's mid-game rule (or the midgameJoin setting) lets
     * it.
     */
    private boolean playsMidgame(int unitSize) {
        if (!hasRoom(unitSize)) {
            return false;
        }
        boolean allowed = !game.settings().has(Setting.MIDGAME_JOIN) || game.settings().get(Setting.MIDGAME_JOIN);
        MidgameJoin policy = allowed ? game.rules().midgameJoin() : MidgameJoin.SPECTATE;
        return switch (policy) {
            case JOIN -> true;
            // 1.1.2's Team Deathmatch rule: only while no team has fewer alive players than it started with;
            // during the countdown nobody can be eliminated yet, so it never closes there.
            case SMALLEST_TEAM_WHILE_FULL -> game.phase() == Phase.COUNTDOWN || game.teams().stream()
                    .allMatch(team -> team.aliveCount() >= team.startingSize());
            case SPECTATE -> false;
        };
    }

    private boolean hasRoom(int unitSize) {
        return game.playerCount() + unitSize <= game.maxPlayers();
    }

    /** One member becomes a spectator at the arena spawn. */
    private void spectate(PlayerRef member) {
        PlayerSession session = roster.activate(member);
        session.spectate();
        PlayerActions players = game.context().players();
        players.teleport(member.id(), game.arena().spawn());
        players.resetForPlay(member.id());
        players.setSpectator(member.id(), true);
        players.joinScoreboard(member.id(), null);
        game.listener().playerJoined(game, session, JoinResult.SPECTATING);
    }

    /** The steps every player gets at the countdown and on a mid-game join: spawn, reset, kit, scoreboard color. */
    private void equip(PlayerSession session) {
        PlayerActions players = game.context().players();
        TeamSpec color = session.color().orElse(null);
        NamedPoint spawn = spawns.next(session.team().map(GameTeam::spec).orElse(null));
        players.teleport(session.id(), spawn);
        players.resetForPlay(session.id());
        players.giveKit(session.id(), game.rules().loadout().kitFor(session), color);
        players.joinScoreboard(session.id(), color);
    }
}
