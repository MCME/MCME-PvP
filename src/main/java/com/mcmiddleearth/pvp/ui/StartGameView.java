package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.BoolSetting;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.IntSetting;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The staff Start game screens: a mode picker, then that mode's arena picker (skipped when only one mode has a
 * free arena), each naming the busy arenas of what it offers. The settings form is generated from the arena's
 * setting specs: a slider per whole-number setting, a toggle per on/off setting, and a text line for a setting
 * with a single possible value. Start opens the game through {@code GameManager.open}, which checks every value
 * again, so values from a modified client that are out of range are refused; missing ones are refused before that.
 */
public final class StartGameView {

    private final PvpServices services;
    private final Messages messages;
    private final UiNavigator navigator;

    public StartGameView(PvpServices services, UiNavigator navigator) {
        this.services = services;
        this.messages = services.messages();
        this.navigator = navigator;
    }

    /**
     * The first screen: staff pick a mode, one button per mode that has a free arena, labelled with its free
     * count; skipped in favor of {@link #arenaPicker} when only one mode qualifies. A mode with arenas that are
     * all busy gets no button, but is still named in the text, so it isn't simply missing. A notice when nothing
     * at all is free.
     */
    public Screen picker() {
        Map<GameMode, Integer> withFree = modesWithFreeArenas();
        if (withFree.isEmpty()) {
            return noneFreeNotice();
        }
        if (withFree.size() == 1) {
            return arenaPicker(withFree.keySet().iterator().next().id());
        }
        List<Button> buttons = new ArrayList<>();
        withFree.forEach((mode, count) -> buttons.add(Button.of(messages.get(Msg.UI_START_MODE,
                        Map.of("mode", UiText.modeName(messages, mode), "count", String.valueOf(count))),
                openModePicker(mode.id()))));
        List<Component> body = new ArrayList<>();
        body.add(messages.get(Msg.UI_PICK_MODE_BODY));
        for (GameMode busyMode : fullyBusyModes()) {
            body.add(messages.get(Msg.UI_PICK_MODE_BUSY, Map.of("mode", UiText.modeName(messages, busyMode))));
        }
        return new MenuScreen(messages.get(Msg.UI_PICK_TITLE), body, buttons, 1, UiText.close(messages));
    }

    /**
     * The second screen: {@code modeId}'s free arenas, sorted by id, three to a row and named by id alone (the
     * body names the mode instead, since a mode name no longer fits a column next to the arena id); the busy
     * arenas of that mode are named in the text below it. Back returns to the mode picker, or closes when that
     * picker would only have offered this one mode — in which case step 1 was skipped, so a fully busy mode is
     * also named here, or it would go unmentioned anywhere. Also the target of the settings form's Back button,
     * for the arena's own mode.
     *
     * @throws PvpException {@link Msg#ERROR_BUTTON_STALE} when the mode no longer has a free arena
     */
    private MenuScreen arenaPicker(String modeId) {
        GameMode mode = services.modes().get(modeId).orElseThrow(() -> new PvpException(Msg.ERROR_BUTTON_STALE));
        List<Arena> free = freeArenasOf(mode.id());
        if (free.isEmpty()) {
            throw new PvpException(Msg.ERROR_BUTTON_STALE);
        }
        List<Button> buttons = free.stream()
                .map(arena -> Button.of(messages.get(Msg.UI_PICK_ENTRY, Map.of("arena", arena.id())),
                        openForm(arena.id())))
                .toList();
        boolean otherModeIsFree = modesWithFreeArenas().keySet().stream()
                .anyMatch(other -> !other.id().equalsIgnoreCase(mode.id()));
        UiAction backToModePicker = UiAction.of((clicker, values) -> navigator.show(clicker, picker()))
                .requires(Permissions.STAFF_RUN);
        Button exit = otherModeIsFree ? UiText.button(messages, Msg.UI_BACK, backToModePicker) : UiText.close(messages);
        List<Component> body = new ArrayList<>();
        body.add(messages.get(Msg.UI_PICK_ARENA_BODY, Map.of("mode", UiText.modeName(messages, mode))));
        body.addAll(busyLinesOf(mode.id()));
        if (!otherModeIsFree) {
            // Step 1 was skipped, so this is the only screen shown: a mode with arenas that are all busy still
            // needs naming here, or it would simply go unmentioned.
            for (GameMode busyMode : fullyBusyModes()) {
                body.add(messages.get(Msg.UI_PICK_MODE_BUSY, Map.of("mode", UiText.modeName(messages, busyMode))));
            }
        }
        return new MenuScreen(messages.get(Msg.UI_PICK_TITLE), body, buttons, 3, exit);
    }

    /** Every registered mode that currently has a free arena, with its free count, in registration order. */
    private Map<GameMode, Integer> modesWithFreeArenas() {
        Map<GameMode, Integer> withFree = new LinkedHashMap<>();
        for (GameMode mode : services.modes().all()) {
            int count = freeArenasOf(mode.id()).size();
            if (count > 0) {
                withFree.put(mode, count);
            }
        }
        return withFree;
    }

    /** Registered modes that have at least one arena, all of them busy right now, in registration order. */
    private List<GameMode> fullyBusyModes() {
        List<GameMode> busy = new ArrayList<>();
        for (GameMode mode : services.modes().all()) {
            if (hasAnyArena(mode.id()) && freeArenasOf(mode.id()).isEmpty()) {
                busy.add(mode);
            }
        }
        return busy;
    }

    /** Whether any playable arena has {@code modeId}, free or busy. */
    private boolean hasAnyArena(String modeId) {
        return services.arenas().playable().stream().anyMatch(arena -> arena.modeId().equalsIgnoreCase(modeId));
    }

    /** The playable arenas of {@code modeId} with no blocker, in {@code services.arenas().playable()}'s id order. */
    private List<Arena> freeArenasOf(String modeId) {
        List<Arena> free = new ArrayList<>();
        for (Arena arena : services.arenas().playable()) {
            if (arena.modeId().equalsIgnoreCase(modeId) && services.games().blockerFor(arena).isEmpty()) {
                free.add(arena);
            }
        }
        return free;
    }

    /** One line per busy arena of {@code modeId}, naming the game that blocks it. */
    private List<Component> busyLinesOf(String modeId) {
        List<Component> lines = new ArrayList<>();
        for (Arena arena : services.arenas().playable()) {
            if (!arena.modeId().equalsIgnoreCase(modeId)) {
                continue;
            }
            services.games().blockerFor(arena).ifPresent(holder -> lines.add(messages.get(Msg.UI_PICK_BLOCKED,
                    Map.of("arena", arena.id(), "holder", holder.arena().id()))));
        }
        return lines;
    }

    /** The notice for when nothing anywhere is free: every busy arena named, exactly as before modes were picked. */
    private NoticeScreen noneFreeNotice() {
        List<Component> body = new ArrayList<>();
        body.add(messages.get(Msg.UI_PICK_NONE));
        for (Arena arena : services.arenas().playable()) {
            if (services.modes().get(arena.modeId()).isEmpty()) {
                continue;   // an arena of a mode this build does not have
            }
            services.games().blockerFor(arena).ifPresent(holder -> body.add(messages.get(Msg.UI_PICK_BLOCKED,
                    Map.of("arena", arena.id(), "holder", holder.arena().id()))));
        }
        return new NoticeScreen(messages.get(Msg.UI_PICK_TITLE), body, UiText.close(messages));
    }

    /**
     * The second screen: the settings form of {@code arena}. Throws {@link PvpException} when the arena is busy or
     * cannot be played.
     */
    public FormScreen form(Arena arena) {
        // Unplayable before busy, as opening checks: waiting won't make an unplayable arena startable.
        List<SettingSpec> specs = services.games().settingsFor(arena);
        Optional<PvpException> blocked = free(arena.id());
        if (blocked.isPresent()) {
            throw blocked.get();
        }
        GameMode mode = services.modes().get(arena.modeId())
                .orElseThrow(() -> new PvpException(Msg.ERROR_ARENA_UNASSIGNED,
                        Map.of("arena", arena.id(), "reason", "unknown mode " + arena.modeId())));
        String modeName = UiText.modeName(messages, mode);
        List<Component> body = new ArrayList<>();
        List<Input> inputs = new ArrayList<>();
        List<SettingSpec> fixed = new ArrayList<>();
        for (SettingSpec spec : specs) {
            Component label = messages.get(Msg.setting(spec.setting().id()));
            switch (spec.setting()) {
                case IntSetting setting when spec.fixed() -> {
                    body.add(messages.get(Msg.UI_FORM_FIXED,
                            Placeholder.component("setting", label),
                            Placeholder.unparsed("value", String.valueOf(spec.defaultValue()))));
                    fixed.add(spec);
                }
                case IntSetting setting -> inputs.add(new Slider(setting.id(), label, spec.min(), spec.max(),
                        spec.step(), (Integer) spec.defaultValue()));
                case BoolSetting setting -> inputs.add(new Toggle(setting.id(), label, (Boolean) spec.defaultValue()));
            }
        }
        Button start = new Button(messages.get(Msg.UI_FORM_START), Component.empty(),
                start(arena.id(), mode.id(), List.copyOf(inputs), List.copyOf(fixed)), ButtonStyle.PRIMARY);
        Button back = UiText.button(messages, Msg.UI_BACK,
                UiAction.of((clicker, values) -> navigator.show(clicker, arenaPicker(mode.id())))
                        .requires(Permissions.STAFF_RUN));
        return new FormScreen(messages.get(Msg.UI_FORM_TITLE, Map.of("mode", modeName, "arena", arena.id())),
                body, inputs, start, back);
    }

    private UiAction openForm(String arenaId) {
        return UiAction.of((clicker, values) -> navigator.show(clicker, form(arena(arenaId))))
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> free(arenaId));
    }

    private UiAction openModePicker(String modeId) {
        return UiAction.of((clicker, values) -> navigator.show(clicker, arenaPicker(modeId)))
                .requires(Permissions.STAFF_RUN);
    }

    private UiAction start(String arenaId, String modeId, List<Input> inputs, List<SettingSpec> fixed) {
        return UiAction.of((clicker, values) -> {
                    Map<String, Object> chosen = new LinkedHashMap<>();
                    for (Input input : inputs) {
                        chosen.put(input.key(), values.require(input.key()));
                    }
                    // A setting shown as text is sent with the value that was shown, not recomputed: a stale form
                    // can't silently open with a different default, and a client still can't set it.
                    for (SettingSpec spec : fixed) {
                        chosen.put(spec.setting().id(), spec.defaultValue());
                    }
                    Game game = services.games().open(arenaId, chosen, clicker.name());
                    navigator.log(clicker.name() + " opens " + game.mode().id() + " on " + game.arena().id()
                            + " from a menu");
                    navigator.message(clicker, messages.get(Msg.ADMIN_OPENED,
                            Map.of("mode", UiText.modeName(messages, game.mode()), "arena", game.arena().id())));
                })
                .requires(Permissions.STAFF_RUN)
                .validWhile(() -> free(arenaId))
                .validWhile(() -> sameMode(arenaId, modeId));
    }

    /** Empty while the arena exists and no game holds its space; otherwise the refusal. */
    private Optional<PvpException> free(String arenaId) {
        Optional<Arena> arena = services.arenas().get(arenaId);
        if (arena.isEmpty()) {
            return Optional.of(new PvpException(Msg.ERROR_UNKNOWN_ARENA, Map.of("arena", arenaId)));
        }
        return services.games().blockerFor(arena.get()).map(holder -> new PvpException(Msg.ERROR_ARENA_BLOCKED,
                Map.of("arena", arenaId, "holder", holder.arena().id())));
    }

    /** Empty while the arena still resolves to the mode {@code modeId} names; otherwise the stale-button refusal. */
    private Optional<PvpException> sameMode(String arenaId, String modeId) {
        boolean unchanged = services.arenas().get(arenaId)
                .flatMap(candidate -> services.modes().get(candidate.modeId()))
                .map(current -> current.id().equals(modeId))
                .orElse(false);
        return unchanged ? Optional.empty() : Optional.of(new PvpException(Msg.ERROR_BUTTON_STALE));
    }

    private Arena arena(String arenaId) {
        return services.arenas().get(arenaId)
                .orElseThrow(() -> new PvpException(Msg.ERROR_UNKNOWN_ARENA, Map.of("arena", arenaId)));
    }
}
