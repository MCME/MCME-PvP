package com.mcmiddleearth.pvp.testing;

import com.mcmiddleearth.pvp.ui.ChatMenu;
import com.mcmiddleearth.pvp.ui.Screen;
import com.mcmiddleearth.pvp.ui.UiNavigator;
import com.mcmiddleearth.pvp.ui.Viewer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A {@link UiNavigator} that records what it was asked to show, for view and platform tests. */
public final class RecordingNavigator implements UiNavigator {

    /** One call: who it was for and what (a Screen, a ChatMenu, a message Component or a command String). */
    public record Shown(UUID viewer, Object content) {
    }

    private final List<Shown> log = new ArrayList<>();
    private final List<String> serverLog = new ArrayList<>();

    @Override
    public void show(Viewer viewer, Screen screen) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(screen, "screen");
        log.add(new Shown(viewer.id(), screen));
    }

    @Override
    public void show(Viewer viewer, ChatMenu menu) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(menu, "menu");
        log.add(new Shown(viewer.id(), menu));
    }

    @Override
    public void message(Viewer viewer, Component message) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(message, "message");
        log.add(new Shown(viewer.id(), message));
    }

    @Override
    public void perform(Viewer viewer, String command) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(command, "command");
        log.add(new Shown(viewer.id(), command));
    }

    @Override
    public void log(String message) {
        Objects.requireNonNull(message, "message");
        serverLog.add(message);
    }

    public List<Shown> log() {
        return List.copyOf(log);
    }

    /** Every message passed to {@link #log(String)}, oldest first. */
    public List<String> serverLog() {
        return List.copyOf(serverLog);
    }

    public void clear() {
        log.clear();
        serverLog.clear();
    }

    /** Everything recorded for one viewer, in order. */
    public List<Object> shownTo(UUID viewer) {
        return log.stream().filter(shown -> shown.viewer().equals(viewer)).map(Shown::content).toList();
    }

    /** The last screen shown, across every viewer. */
    public Screen lastScreen() {
        return last(Screen.class);
    }

    public ChatMenu lastMenu() {
        return last(ChatMenu.class);
    }

    /** The plain text of every message, across every viewer, in order. */
    public List<String> messages() {
        return log.stream().map(Shown::content).filter(Component.class::isInstance)
                .map(content -> UiTesting.plain((Component) content)).toList();
    }

    /** Every command run, across every viewer, in order. */
    public List<String> commands() {
        return log.stream().map(Shown::content).filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private <T> T last(Class<T> type) {
        for (int i = log.size() - 1; i >= 0; i--) {
            if (type.isInstance(log.get(i).content())) {
                return type.cast(log.get(i).content());
            }
        }
        throw new AssertionError("nothing of type " + type.getSimpleName() + " was shown; log: " + log);
    }
}
