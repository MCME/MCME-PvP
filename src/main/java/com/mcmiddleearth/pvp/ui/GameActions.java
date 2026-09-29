package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.JoinReplies;
import com.mcmiddleearth.pvp.game.JoinResult;
import com.mcmiddleearth.pvp.game.JoinUnit;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.Map;
import java.util.Optional;

/**
 * The game buttons the menus and chat lines share. They call the same {@code GameManager} methods as the commands,
 * and each checks at click time that its game is still there, so a button for a closed game answers "That game has
 * ended" and does nothing.
 */
final class GameActions {

    private final PvpServices services;
    private final UiNavigator navigator;

    GameActions(PvpServices services, UiNavigator navigator) {
        this.services = services;
        this.navigator = navigator;
    }

    /** Join, with the refusal and the answers of {@code /pvp join}. */
    UiAction join(Game game) {
        return UiAction.of((viewer, values) -> {
                    if (services.lock().keepsOut(viewer::has)) {
                        throw new PvpException(Msg.ERROR_SERVER_LOCKED);
                    }
                    String spectateReason = JoinReplies.spectateReason(game, viewer.id());
                    if (services.games().join(JoinUnit.solo(viewer.ref()), game) == JoinResult.SPECTATING) {
                        navigator.message(viewer, services.messages().get(spectateReason));
                    }
                })
                .requires(Permissions.PLAY)
                .validWhile(() -> joinable(game));
    }

    /** Spectate, with the refusal of {@code /pvp spectate}. */
    UiAction spectate(Game game) {
        return UiAction.of((viewer, values) -> {
                    if (services.lock().keepsOut(viewer::has)) {
                        throw new PvpException(Msg.ERROR_SERVER_LOCKED);
                    }
                    services.games().spectate(viewer.ref(), game);
                })
                .requires(Permissions.PLAY)
                .validWhile(() -> joinable(game));
    }

    /**
     * Leave: refused with "that game has ended" once {@code game} has closed, or "you already left that game" when
     * the viewer is no longer in it (they left it, or they are in another one), and only then valid while
     * {@code viewer} is still in {@code game}.
     */
    UiAction leave(Viewer viewer, Game game) {
        return UiAction.of((clicker, values) -> services.games().leave(viewer.id(), LeaveReason.LEFT))
                .requires(Permissions.PLAY)
                .validWhile(() -> stillOpen(game))
                .validWhile(() -> services.games().gameOf(viewer.id()).filter(current -> current == game).isPresent()
                        ? Optional.empty() : Optional.of(new PvpException(Msg.ERROR_LEFT_THAT_GAME)));
    }

    UiAction countdown(Game game) {
        return UiAction.of((viewer, values) -> {
                    services.games().startCountdown(game);
                    navigator.log(viewer.name() + " starts the countdown on " + game.arena().id() + " from a menu");
                    navigator.message(viewer, services.messages().get(Msg.ADMIN_COUNTDOWN,
                            Map.of("arena", game.arena().id())));
                })
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> stillOpen(game));
    }

    /** Stop is destructive: single use, and the lists put it behind a confirmation. */
    UiAction stop(Game game) {
        return UiAction.of((viewer, values) -> {
                    services.games().stop(game);
                    navigator.log(viewer.name() + " stops the game on " + game.arena().id() + " from a menu");
                    navigator.message(viewer, services.messages().get(Msg.ADMIN_STOPPED,
                            Map.of("arena", game.arena().id())));
                })
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> stillOpen(game))
                .once();
    }

    /** Empty while the manager still has the game open. */
    Optional<PvpException> stillOpen(Game game) {
        return services.games().games().contains(game) ? Optional.empty() : ended();
    }

    /**
     * Empty while the game still accepts joins. A join may still be refused (a full waiting game) or end in
     * spectating (a full running game, or a mode whose late joiners only watch), so this is not the same as
     * offering Join.
     */
    Optional<PvpException> joinable(Game game) {
        return services.games().games().contains(game) && game.acceptsJoins() ? Optional.empty() : ended();
    }

    /** Whether a game line offers Join: the viewer would play there, by the same rule as joining itself. */
    boolean offersJoin(Game game) {
        return game.canJoinAsPlayer(1);
    }

    /**
     * Whether a game line offers Spectate, and so whether the arrival notice and the Spectate list show the game
     * at all: it still accepts joins.
     */
    boolean offersSpectate(Game game) {
        return game.acceptsJoins();
    }

    private static Optional<PvpException> ended() {
        return Optional.of(new PvpException(Msg.ERROR_GAME_ENDED));
    }
}
