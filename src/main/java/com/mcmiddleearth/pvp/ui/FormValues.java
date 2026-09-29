package com.mcmiddleearth.pvp.ui;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a form sent back, by input key: an Integer for a slider, a Boolean for a toggle, a String for a choice.
 * A key the client did not send is absent; {@link #require(String)} refuses it instead of guessing a default.
 */
public record FormValues(Map<String, Object> values) {

    public FormValues {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static FormValues empty() {
        return new FormValues(Map.of());
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    /**
     * The value of {@code key}. A missing value is refused like an invalid one ({@code error.setting-invalid}),
     * because a modified client can leave fields out and a form must never run on values nobody chose.
     */
    public Object require(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new PvpException(Msg.ERROR_SETTING_INVALID, Map.of("setting", key, "value", "missing"));
        }
        return value;
    }
}
