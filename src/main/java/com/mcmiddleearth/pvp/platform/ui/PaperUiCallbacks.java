package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import com.mcmiddleearth.pvp.ui.Choice;
import com.mcmiddleearth.pvp.ui.FormValues;
import com.mcmiddleearth.pvp.ui.Input;
import com.mcmiddleearth.pvp.ui.Slider;
import com.mcmiddleearth.pvp.ui.Toggle;
import com.mcmiddleearth.pvp.ui.UiAction;
import com.mcmiddleearth.pvp.ui.Viewer;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Paper's click callbacks for UI actions. On Paper 26.2 both callback kinds already arrive on the server
 * thread — {@code handleCustomClickAction} runs after {@code ensureRunningOnSameThread} — so the thread hand-off in
 * {@code clicked} is a guard, not the normal path. A click is refused, and answered with {@code error.button-stale},
 * when it isn't from the player the button was made for, when the button is older than {@link #LIFETIME} or already
 * spent (a single-use button), or once this plugin is disabled. A click that gets past those checks lets the action
 * check the live player's permission and its still-valid state before it runs. Every refusal is sent to the player
 * as chat.
 */
public final class PaperUiCallbacks implements UiCallbacks {

    /** How long a button works after it was shown. */
    static final Duration LIFETIME = Duration.ofMinutes(10);
    /**
     * Paper's own lifetime for the callback, deliberately well beyond {@link #LIFETIME}: Paper drops a callback
     * that outlives it with no answer at all to the player, since by then it no longer calls us. An hour
     * comfortably covers a realistic late click, while still freeing a disabled plugin's callbacks (after a
     * {@code /reload}, say) far sooner than Adventure's own 12-hour default would.
     */
    static final Duration PAPER_LIFETIME = Duration.ofHours(1);

    private final Plugin plugin;
    private final Messages messages;
    private final DialogTracker tracker;
    private final Clock clock;

    public PaperUiCallbacks(Plugin plugin, Messages messages, DialogTracker tracker) {
        this(plugin, messages, tracker, Clock.systemUTC());
    }

    PaperUiCallbacks(Plugin plugin, Messages messages, DialogTracker tracker, Clock clock) {
        this.plugin = plugin;
        this.messages = messages;
        this.tracker = tracker;
        this.clock = clock;
    }

    @Override
    public ClickEvent<?> chat(Viewer viewer, UiAction action) {
        return ClickEvent.callback(chatCallback(viewer, action), options(action));
    }

    @Override
    public DialogAction dialog(Viewer viewer, UiAction action, List<Input> inputs) {
        return DialogAction.customClick(dialogCallback(viewer, action, inputs), options(action));
    }

    /** The callback behind a chat button; tests call it directly. */
    ClickCallback<Audience> chatCallback(Viewer viewer, UiAction action) {
        Ticket ticket = new Ticket(viewer.id(), clock.instant(), action.singleUse());
        return audience -> clicked(audience, ticket, action, FormValues.empty(), false);
    }

    /** The callback behind a dialog button; tests call it with a fake response. */
    DialogActionCallback dialogCallback(Viewer viewer, UiAction action, List<Input> inputs) {
        Ticket ticket = new Ticket(viewer.id(), clock.instant(), action.singleUse());
        // The response is read at once, on the calling thread; it is a snapshot of what the client sent.
        return (response, audience) -> clicked(audience, ticket, action, read(response, inputs), true);
    }

    static ClickCallback.Options options(UiAction action) {
        return ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(PAPER_LIFETIME)
                .build();
    }

    /**
     * The submitted values of {@code inputs}: sliders rounded to whole numbers, toggles as booleans, choices as the
     * option id. A value the client left out stays absent, so the form refuses it instead of using a default.
     */
    static FormValues read(DialogResponseView response, List<Input> inputs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Input input : inputs) {
            Object value = switch (input) {
                case Slider slider -> {
                    Float number = response.getFloat(slider.key());
                    yield number == null ? null : Math.round(number);
                }
                case Toggle toggle -> response.getBoolean(toggle.key());
                case Choice choice -> response.getText(choice.key());
            };
            if (value != null) {
                values.put(input.key(), value);
            }
        }
        return new FormValues(values);
    }

    private void clicked(Audience audience, Ticket ticket, UiAction action, FormValues values, boolean dialog) {
        if (!(audience instanceof Player player)) {
            return;
        }
        if (action.isNone()) {
            // Close, No or Escape only close the dialog: nothing to refuse, even once the plugin has been disabled
            // meanwhile (a /reload leaves stale callbacks alive) — the client has already closed it. The tracker
            // entry is still cheap to clear here; once disabled, nothing reads it again anyway.
            if (dialog) {
                closeTracker(player);
            }
            return;
        }
        if (!plugin.isEnabled()) {
            // Paper keeps a callback alive for PAPER_LIFETIME after a /reload or a plugin-manager disable, so a
            // click can still arrive with the services it would run against already gone. runTask() below would
            // throw on a disabled plugin, so this must be checked first.
            player.sendMessage(messages.get(Msg.ERROR_BUTTON_STALE));
            return;
        }
        Runnable click = () -> handle(player, ticket, action, values, dialog);
        Server server = plugin.getServer();
        if (server.isPrimaryThread()) {
            click.run();
        } else {
            // A guard, not the normal path: both callback kinds already arrive on the server thread on Paper 26.2.
            server.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {   // may have left between this click and the next tick running it
                    click.run();
                }
            });
        }
    }

    /** Clears the tracker on the server thread; off it, only while the plugin can still schedule a task. */
    private void closeTracker(Player player) {
        Server server = plugin.getServer();
        if (server.isPrimaryThread()) {
            tracker.closed(player.getUniqueId());
        } else if (plugin.isEnabled()) {
            server.getScheduler().runTask(plugin, () -> tracker.closed(player.getUniqueId()));
        }
    }

    private void handle(Player player, Ticket ticket, UiAction action, FormValues values, boolean dialog) {
        if (dialog) {
            // Every dialog carries DialogRenderer's CLOSE after-action, so the client has already closed it by
            // the time this callback runs. Closing the tracker here, before the handler, matters: a handler that
            // opens the next screen calls tracker.opened(...) again, and closing it after the handler would
            // immediately undo that. A none() action never reaches here: clicked() already closed the tracker
            // and returned for it.
            tracker.closed(player.getUniqueId());
        }
        if (!ticket.admit(player.getUniqueId(), clock.instant())) {
            player.sendMessage(messages.get(Msg.ERROR_BUTTON_STALE));
            return;
        }
        try {
            action.attempt(Viewers.of(player), values).ifPresent(refusal -> player.sendMessage(messages.error(refusal)));
        } catch (RuntimeException e) {
            // A handler bug. Paper would only log a failed packet and the player would get no answer at all.
            plugin.getLogger().log(Level.SEVERE, "A menu button failed for " + player.getName(), e);
            player.sendMessage(messages.get(Msg.ERROR_BUTTON_FAILED));
        }
    }

    /** One shown button: whose it is, when it was shown, and whether its single use is spent. */
    private static final class Ticket {

        private final UUID viewer;
        private final Instant shown;
        private final boolean singleUse;
        private boolean used;

        Ticket(UUID viewer, Instant shown, boolean singleUse) {
            this.viewer = viewer;
            this.shown = shown;
            this.singleUse = singleUse;
        }

        /** True when this click may go ahead; a single-use button is spent by it. */
        boolean admit(UUID clicker, Instant now) {
            if (!clicker.equals(viewer) || now.isAfter(shown.plus(LIFETIME)) || (singleUse && used)) {
                return false;
            }
            used = true;
            return true;
        }
    }
}
