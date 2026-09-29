package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What a button does, checked again when it is clicked: the clicker must still hold {@code permission}
 * (nullable: none needed), {@code check} must still pass (it returns the refusal, such as "That game has ended"),
 * and only then does the handler run. A handler refuses by throwing {@link PvpException}. {@code singleUse} marks a
 * destructive action, which the Paper layer lets run only once.
 */
public record UiAction(String permission, Supplier<Optional<PvpException>> check, Handler handler, boolean singleUse) {

    private static final Supplier<Optional<PvpException>> ALWAYS_VALID = Optional::empty;
    private static final Handler NOTHING = (viewer, values) -> {
    };

    public UiAction {
        Objects.requireNonNull(check, "check");
        Objects.requireNonNull(handler, "handler");
    }

    /** The work behind a button. It runs on the server thread and may throw {@link PvpException}. */
    @FunctionalInterface
    public interface Handler {
        void handle(Viewer viewer, FormValues values);
    }

    /** An action anyone may run, always valid and reusable; narrow it with the methods below. */
    public static UiAction of(Handler handler) {
        return new UiAction(null, ALWAYS_VALID, handler, false);
    }

    /**
     * An action that does nothing: the button only closes its screen. Never chain {@link #requires} or
     * {@link #validWhile} onto it: the click handling recognizes a {@code none()} action by its handler alone and
     * returns before either would run, so a permission or check added here would silently never be enforced.
     */
    public static UiAction none() {
        return new UiAction(null, ALWAYS_VALID, NOTHING, false);
    }

    public UiAction requires(String node) {
        Objects.requireNonNull(node, "node");
        return new UiAction(node, check, handler, singleUse);
    }

    /** Adds another still-valid check: if this action already has one, both apply, in order. */
    public UiAction validWhile(Supplier<Optional<PvpException>> stillValid) {
        Objects.requireNonNull(stillValid, "stillValid");
        Supplier<Optional<PvpException>> combined = check == ALWAYS_VALID ? stillValid : () -> check.get().or(stillValid);
        return new UiAction(permission, combined, handler, singleUse);
    }

    public UiAction once() {
        return new UiAction(permission, check, handler, true);
    }

    /** This action with fixed form values: a form submitted without its inputs (the chat version of a form). */
    public UiAction withValues(FormValues fixed) {
        Objects.requireNonNull(fixed, "fixed");
        return new UiAction(permission, check, (viewer, ignored) -> handler.handle(viewer, fixed), singleUse);
    }

    /**
     * True for {@link #none()}, so chat can leave out buttons that would do nothing there. Compares only the
     * handler: a permission or check chained onto {@link #none()} would still match here, and the click handling
     * would then skip it unenforced, so never chain one onto it.
     */
    public boolean isNone() {
        return handler == NOTHING;
    }

    /** Whether {@code viewer} currently holds this action's permission (true when it needs none). */
    public boolean allows(Viewer viewer) {
        return permission == null || viewer.has(permission);
    }

    /**
     * Runs the click-time checks and then the handler, exactly as a click does, and returns the refusal if there is
     * one: no permission, a failed still-valid check, or the {@link PvpException} of the handler or the check.
     */
    public Optional<PvpException> attempt(Viewer viewer, FormValues values) {
        if (!allows(viewer)) {
            return Optional.of(new PvpException(Msg.ERROR_NO_PERMISSION));
        }
        try {
            Optional<PvpException> refusal = check.get();
            if (refusal.isPresent()) {
                return refusal;
            }
            handler.handle(viewer, values);
            return Optional.empty();
        } catch (PvpException e) {
            return Optional.of(e);
        }
    }
}
