package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.arena.NamedPoint;
import com.mcmiddleearth.pvp.mode.rules.Respawn;

import java.util.Optional;
import java.util.UUID;

/** Deaths, hits, respawns and interactions of players, and deaths of owned entities. */
final class Combat {

    /** Leaving within this many ticks (10 seconds) of a hit by another player counts as a death. */
    private static final long FLEE_TICKS = 200;

    private final Game game;
    private final Roster roster;
    private final SpawnCursor spawns;
    private final Lifecycle lifecycle;

    Combat(Game game, Roster roster, SpawnCursor spawns, Lifecycle lifecycle) {
        this.game = game;
        this.roster = roster;
        this.spawns = spawns;
        this.lifecycle = lifecycle;
    }

    /** See {@link Game#handleDeath}. {@code killerId} is nullable. */
    void death(UUID victimId, UUID killerId) {
        if (game.phase() != Phase.RUNNING) {
            return;
        }
        Optional<PlayerSession> found = roster.present(victimId).filter(PlayerSession::alive);
        if (found.isEmpty()) {
            return;
        }
        PlayerSession victim = found.get();
        count(victim, creditedKiller(victim, killerId), false);
    }

    /** See {@link Game#handleHit}. */
    void hit(UUID victimId, UUID attackerId) {
        if (game.phase() != Phase.RUNNING || victimId.equals(attackerId)
                || roster.present(attackerId).filter(attacker -> attacker.role() == Role.PLAYER).isEmpty()) {
            return;
        }
        roster.present(victimId)
                .filter(PlayerSession::alive)
                .ifPresent(victim -> victim.hitBy(attackerId, game.context().tasks().currentTick()));
    }

    /**
     * Leaving under fire, before the leave itself: a present, living player who leaves or quits a running game within
     * {@link #FLEE_TICKS} of a hit by another player counts as killed by that player. Any other way out (a kick, a
     * sweep, a closing game) never counts.
     */
    void leaving(PlayerSession session, LeaveReason reason) {
        if (game.phase() != Phase.RUNNING || !session.alive()
                || (reason != LeaveReason.LEFT && reason != LeaveReason.QUIT)) {
            return;
        }
        session.hitWithin(game.context().tasks().currentTick(), FLEE_TICKS)
                .ifPresent(attacker -> count(session, creditedKiller(session, attacker), true));
    }

    /** See {@link Game#respawnPoint}. */
    Optional<NamedPoint> respawnPoint(UUID player) {
        Optional<PlayerSession> found = roster.present(player);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        PlayerSession session = found.get();
        // A living player in the fight respawns at the next spawn, the same whenever the respawn gives them a kit.
        boolean backInTheFight = session.alive()
                && (game.phase() == Phase.COUNTDOWN || game.phase() == Phase.RUNNING);
        NamedPoint point = backInTheFight
                ? spawns.next(session.team().map(GameTeam::spec).orElse(null))
                : game.arena().spawn();
        return Optional.of(point);
    }

    /**
     * See {@link Game#handleRespawn}: one tick later, when the respawned player is back in the world. A hit from
     * before the respawn is forgotten at once.
     */
    void respawn(UUID player) {
        roster.present(player).ifPresent(PlayerSession::clearHit);
        game.context().tasks().after(1, () -> roster.present(player).ifPresent(this::afterRespawn));
    }

    /** {@code killerId} is nullable. The entity is forgotten in every phase; the hook runs only while RUNNING. */
    void entityDeath(UUID entity, UUID killerId) {
        if (!game.entities().owns(entity)) {
            return;
        }
        game.entities().forget(entity);
        if (game.phase() != Phase.RUNNING) {
            return;
        }
        Optional<PlayerSession> killer = killerId == null
                ? Optional.empty()
                : roster.present(killerId).filter(session -> session.role() == Role.PLAYER);
        game.logic().onEntityKilled(game, entity, killer);
        lifecycle.checkWin();
    }

    /** See {@link Game#handleInteract}. */
    void interact(UUID player, NamedPoint block) {
        if (game.phase() == Phase.RUNNING) {
            roster.present(player)
                    .filter(PlayerSession::alive)
                    .ifPresent(session -> game.logic().onInteract(game, session, block));
        }
    }

    /** Whom {@link #death} would credit now; empty when the death wouldn't count. {@code killerId} is nullable. */
    Optional<PlayerSession> creditFor(UUID victimId, UUID killerId) {
        if (game.phase() != Phase.RUNNING) {
            return Optional.empty();
        }
        return roster.present(victimId)
                .filter(PlayerSession::alive)
                .flatMap(victim -> creditedKiller(victim, killerId));
    }

    /**
     * A counted death, the same whether the player died or fled: the victim's death (and their last hit forgotten),
     * the credited killer's kill and kill score, the elimination under {@link Respawn#NEVER} and the mode's onDeath;
     * then playerDied (playerFled for a flee) and scoreChanged fire, and the win check runs.
     */
    private void count(PlayerSession victim, Optional<PlayerSession> killer, boolean fled) {
        victim.clearHit();
        // Score events wait until playerDied has fired, so listeners never see "game over" before the death itself.
        lifecycle.holdingScoreEvents(() -> {
            victim.addDeath();
            killer.ifPresent(credited -> {
                credited.addKill();
                if (game.rules().killScore() > 0) {
                    game.scores().addScore(credited, game.rules().killScore());
                }
            });
            if (game.rules().respawn() == Respawn.NEVER) {
                victim.eliminate();
            }
            game.logic().onDeath(game, victim, killer);
        });
        if (fled) {
            game.listener().playerFled(game, victim, killer);
        } else {
            game.listener().playerDied(game, victim, killer);
        }
        game.listener().scoreChanged(game);
        lifecycle.checkWin();
    }

    /** The killer earns the kill when they play in this game, are not the victim, and are not on the victim's team. */
    private Optional<PlayerSession> creditedKiller(PlayerSession victim, UUID killerId) {
        if (killerId == null || killerId.equals(victim.id())) {
            return Optional.empty();
        }
        return roster.present(killerId)
                .filter(killer -> killer.role() == Role.PLAYER)
                .filter(killer -> game.teams().isEmpty() || killer.team().isEmpty()
                        || !killer.team().equals(victim.team()));
    }

    private void afterRespawn(PlayerSession session) {
        PlayerActions players = game.context().players();
        Phase phase = game.phase();
        if (session.alive() && (phase == Phase.COUNTDOWN || phase == Phase.RUNNING)) {
            players.resetForPlay(session.id());
            players.giveKit(session.id(), game.rules().loadout().kitFor(session), session.color().orElse(null));
            if (phase == Phase.RUNNING && game.rules().spawnProtectionSeconds() > 0) {
                session.protectFor(game.rules().spawnProtectionSeconds());
            }
            game.logic().onRespawn(game, session);
        } else if (!session.alive()) {
            players.setSpectator(session.id(), true);
        } else if (phase == Phase.WAITING) {
            players.resetForPlay(session.id());
        }
    }
}
