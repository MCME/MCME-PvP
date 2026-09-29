package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.text.Msg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The {@code setting=value ...} part of {@code /pvpadmin start}. Parsing only splits the pairs; which settings
 * exist and which values are valid is decided by {@code GameSettings.resolve} when the game opens.
 */
public final class SettingsArgument {

    private SettingsArgument() {
    }

    /** "teamCount=3 timeLimit=12" as an ordered map; a blank text is no settings. */
    public static Map<String, String> parse(String text) {
        Map<String, String> settings = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (String pair : text.trim().split("\\s+")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            if (equals < 0) {
                // A bare word: name it, rather than echo an "=" that was never typed.
                throw new PvpException(Msg.ERROR_SETTING_FORM, Map.of("word", pair));
            }
            if (equals == 0) {
                throw new PvpException(Msg.ERROR_SETTING_INVALID,
                        Map.of("setting", pair.substring(0, equals), "value", pair.substring(equals + 1)));
            }
            if (equals == pair.length() - 1) {
                // A name with nothing after the "=": the same message as a bare word, naming the part before it.
                throw new PvpException(Msg.ERROR_SETTING_FORM, Map.of("word", pair.substring(0, equals)));
            }
            String setting = pair.substring(0, equals);
            // Settings match ignoring case when the game opens, so the last spelling would win silently.
            if (!seen.add(setting.toLowerCase(Locale.ROOT))) {
                throw new PvpException(Msg.ERROR_SETTING_TWICE, Map.of("setting", setting));
            }
            settings.put(setting, pair.substring(equals + 1));
        }
        return settings;
    }

    /**
     * Completions for the word being typed at the end of {@code typed}: {@code id=default} for every setting
     * whose id starts with that word and that is not set yet.
     */
    public static List<String> suggestions(String typed, List<SettingSpec> specs) {
        String last = typed.substring(typed.lastIndexOf(' ') + 1).toLowerCase(Locale.ROOT);
        List<String> used = new ArrayList<>();
        for (String word : typed.trim().split("\\s+")) {
            int equals = word.indexOf('=');
            if (equals > 0) {
                used.add(word.substring(0, equals).toLowerCase(Locale.ROOT));
            }
        }
        List<String> result = new ArrayList<>();
        for (SettingSpec spec : specs) {
            String id = spec.setting().id();
            if (id.toLowerCase(Locale.ROOT).startsWith(last) && !used.contains(id.toLowerCase(Locale.ROOT))) {
                result.add(id + "=" + spec.defaultValue());
            }
        }
        return result;
    }
}
