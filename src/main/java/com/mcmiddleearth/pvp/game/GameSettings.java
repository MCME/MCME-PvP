package com.mcmiddleearth.pvp.game;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The settings of one game, fixed when it opens: every setting its mode offers, with its value (an Integer or a
 * Boolean). Built from the mode's {@link SettingSpec}s, either from staff input ({@link #resolve}) or as the defaults.
 */
public final class GameSettings {

    private static final Set<String> TRUE_WORDS = Set.of("true", "yes", "on");
    private static final Set<String> FALSE_WORDS = Set.of("false", "no", "off");

    private final Map<String, Object> values;

    private GameSettings(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    /**
     * Checks staff input against the specs. Keys match spec ids ignoring case, and a missing key takes the spec's
     * default. Numbers may come as numbers or as text; switches as booleans or as true/false/yes/no/on/off.
     *
     * @throws PvpException for an unknown key, a value of the wrong kind, or a number outside the spec's range
     */
    public static GameSettings resolve(List<SettingSpec> specs, Map<String, ?> input, String modeId) {
        Map<String, SettingSpec> byKey = new HashMap<>();
        specs.forEach(spec -> byKey.put(spec.setting().id().toLowerCase(Locale.ROOT), spec));
        Map<String, Object> values = defaultValues(specs);
        input.forEach((key, raw) -> {
            SettingSpec spec = byKey.get(key.toLowerCase(Locale.ROOT));
            if (spec == null) {
                throw new PvpException(Msg.ERROR_SETTING_UNKNOWN, Map.of("setting", key, "mode", modeId,
                        "settings", specs.stream().map(s -> s.setting().id()).collect(Collectors.joining(", "))));
            }
            Object value = spec.setting() instanceof IntSetting ? intValue(spec, raw) : boolValue(spec, raw);
            values.put(spec.setting().id(), value);
        });
        return new GameSettings(values);
    }

    /** Every spec's default value. */
    public static GameSettings defaults(List<SettingSpec> specs) {
        return new GameSettings(defaultValues(specs));
    }

    /** @throws IllegalArgumentException when this game has no such setting */
    public int get(IntSetting setting) {
        if (values.get(setting.id()) instanceof Integer value) {
            return value;
        }
        throw new IllegalArgumentException("this game has no setting " + setting.id());
    }

    /** @throws IllegalArgumentException when this game has no such setting */
    public boolean get(BoolSetting setting) {
        if (values.get(setting.id()) instanceof Boolean value) {
            return value;
        }
        throw new IllegalArgumentException("this game has no setting " + setting.id());
    }

    public boolean has(Setting setting) {
        return values.containsKey(setting.id());
    }

    /** Setting id to value (Integer or Boolean), sorted by id; this is what match records store. */
    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public String toString() {
        return "GameSettings" + values;
    }

    private static Map<String, Object> defaultValues(List<SettingSpec> specs) {
        Map<String, Object> values = new TreeMap<>();
        specs.forEach(spec -> values.put(spec.setting().id(), spec.defaultValue()));
        return values;
    }

    /**
     * A whole number from staff input: a {@code Number} (a {@code Float} or {@code Double} is rounded) or text holding
     * an integer; empty for anything else. Package-private so the engine reads raw input the same way.
     */
    static OptionalLong wholeNumber(Object raw) {
        if (raw instanceof Float || raw instanceof Double) {
            double number = ((Number) raw).doubleValue();
            return Double.isNaN(number) ? OptionalLong.empty() : OptionalLong.of(Math.round(number));
        }
        if (raw instanceof Number number) {
            return OptionalLong.of(number.longValue());
        }
        if (raw instanceof String text) {
            try {
                return OptionalLong.of(Long.parseLong(text.trim()));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }
        return OptionalLong.empty();
    }

    private static Integer intValue(SettingSpec spec, Object raw) {
        long value = wholeNumber(raw).orElseThrow(() -> invalid(spec, raw));
        if (value < spec.min() || value > spec.max()) {
            throw new PvpException(Msg.ERROR_SETTING_RANGE, Map.of("setting", spec.setting().id(),
                    "min", String.valueOf(spec.min()), "max", String.valueOf(spec.max())));
        }
        return (int) value;
    }

    private static Boolean boolValue(SettingSpec spec, Object raw) {
        if (raw instanceof Boolean flag) {
            return flag;
        }
        if (raw instanceof String text) {
            String word = text.trim().toLowerCase(Locale.ROOT);
            if (TRUE_WORDS.contains(word)) {
                return true;
            }
            if (FALSE_WORDS.contains(word)) {
                return false;
            }
        }
        throw invalid(spec, raw);
    }

    private static PvpException invalid(SettingSpec spec, Object raw) {
        return new PvpException(Msg.ERROR_SETTING_INVALID,
                Map.of("setting", spec.setting().id(), "value", String.valueOf(raw)));
    }
}
