package com.mcmiddleearth.pvp.testing;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A test-only Adventure click-callback provider, named in
 * {@code src/test/resources/META-INF/services/net.kyori.adventure.text.event.ClickCallback$Provider}. The real
 * provider lives in the Paper server, so in tests {@code ClickEvent.callback(...)} would otherwise become a
 * suggest-command that can never fire. This one turns each callback into {@code /test-callback <id>} and lets a
 * test fire it with {@link #click}. Adventure creates the provider through ServiceLoader once per JVM and gives
 * tests no handle on it, which is why the captured callbacks live in static maps (test code only).
 *
 * <p>If a later MockBukkit ships its own {@code ClickCallback.Provider}, Adventure will likely refuse two
 * providers and the callback tests will fail when this class loads; this one can go then.
 */
public final class CapturingClickCallbackProvider implements ClickCallback.Provider {

    private static final String PREFIX = "/test-callback ";
    private static final Map<String, ClickCallback<Audience>> CALLBACKS = new ConcurrentHashMap<>();
    private static final Map<String, ClickCallback.Options> OPTIONS = new ConcurrentHashMap<>();

    /**
     * Forgets every captured callback. Tests that render real click callbacks call it from their teardown: each
     * callback keeps its plugin and mock server alive for the rest of the test run.
     */
    public static void reset() {
        CALLBACKS.clear();
        OPTIONS.clear();
    }

    @Override
    public ClickEvent<?> create(ClickCallback<Audience> callback, ClickCallback.Options options) {
        String id = UUID.randomUUID().toString();
        CALLBACKS.put(id, callback);
        OPTIONS.put(id, options);
        return ClickEvent.runCommand(PREFIX + id);
    }

    /** Fires the callback behind {@code event} as if {@code clicker} clicked it; false for any other click event. */
    public static boolean click(ClickEvent<?> event, Audience clicker) {
        Optional<String> id = id(event);
        if (id.isEmpty() || !CALLBACKS.containsKey(id.get())) {
            return false;
        }
        CALLBACKS.get(id.get()).accept(clicker);
        return true;
    }

    /** The options the callback behind {@code event} was created with. */
    public static Optional<ClickCallback.Options> options(ClickEvent<?> event) {
        return id(event).map(OPTIONS::get);
    }

    private static Optional<String> id(ClickEvent<?> event) {
        if (event != null && event.payload() instanceof ClickEvent.Payload.Text text
                && text.value().startsWith(PREFIX)) {
            return Optional.of(text.value().substring(PREFIX.length()));
        }
        return Optional.empty();
    }
}
