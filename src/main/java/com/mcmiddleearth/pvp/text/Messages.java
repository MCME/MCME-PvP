package com.mcmiddleearth.pvp.text;

import com.mcmiddleearth.pvp.PvpException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The texts of messages.yml, turned into Adventure components with MiniMessage. Nested keys are flattened with
 * dots ({@code error.no-permission}). The {@code theme} section holds colors; each becomes a styling tag of the
 * same name ({@code <accent>…</accent>}) in every message, so restyling is a config change. A key the file lacks
 * comes from the fallback, and a key nobody has is shown as the key itself: visible, never an exception.
 */
public final class Messages {

    private static final String THEME = "theme";
    /** MiniMessage tag names: lowercase letters, digits, '_' and '-'. */
    private static final Pattern TAG_NAME = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-f]{6}");

    /** Message key to MiniMessage text; the fallback's keys included. */
    private final Map<String, String> texts;
    /** Theme name to color; the fallback's names included. */
    private final Map<String, TextColor> theme;
    private final TagResolver themeTags;

    private Messages(Map<String, String> texts, Map<String, TextColor> theme) {
        this.texts = Map.copyOf(texts);
        this.theme = Map.copyOf(theme);
        this.themeTags = TagResolver.resolver(this.theme.entrySet().stream()
                .map(entry -> Placeholder.styling(entry.getKey(), entry.getValue()))
                .toList());
    }

    /** The messages.yml inside the plugin jar, without a fallback. */
    public static Messages bundled() {
        try (InputStream in = Messages.class.getResourceAsStream("/messages.yml")) {
            if (in == null) {
                throw new IllegalStateException("messages.yml is missing from the plugin jar");
            }
            return fromYaml(new String(in.readAllBytes(), StandardCharsets.UTF_8), null);
        } catch (IOException e) {
            throw new UncheckedIOException("messages.yml could not be read from the plugin jar", e);
        }
    }

    /** Messages from YAML text; keys and theme colors it lacks come from {@code fallback} (may be null). */
    public static Messages fromYaml(String yaml, Messages fallback) {
        YamlConfiguration file = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        Map<String, String> texts = new HashMap<>(fallback == null ? Map.of() : fallback.texts);
        Map<String, TextColor> theme = new HashMap<>(fallback == null ? Map.of() : fallback.theme);
        for (String key : file.getKeys(true)) {
            if (!file.isString(key)) {
                continue;
            }
            String value = file.getString(key);
            if (key.startsWith(THEME + ".")) {
                String name = key.substring(THEME.length() + 1).toLowerCase(Locale.ROOT);
                TextColor color = parseColor(value);
                // An invalid color or name keeps the fallback's value, if there is one.
                if (color != null && TAG_NAME.matcher(name).matches()) {
                    theme.put(name, color);
                }
            } else if (!key.equals(THEME)) {
                texts.put(key, value);
            }
        }
        return new Messages(texts, theme);
    }

    /**
     * Messages from a UTF-8 file on top of {@code fallback} (may be null).
     *
     * @throws IOException if the file cannot be read, or has a YAML error: checked strictly here, because Bukkit's
     *         own lenient loader would otherwise read such a file as empty.
     */
    public static Messages load(Path file, Messages fallback) throws IOException {
        String yaml = Files.readString(file, StandardCharsets.UTF_8);
        try {
            new YamlConfiguration().loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new IOException(file.getFileName() + " has a YAML error: " + e.getMessage(), e);
        }
        return fromYaml(yaml, fallback);
    }

    /** The message with the theme tags and {@code resolvers} applied. */
    public Component get(String key, TagResolver... resolvers) {
        String text = texts.get(key);
        if (text == null) {
            return Component.text(key);
        }
        return MiniMessage.miniMessage().deserialize(text, themeTags, TagResolver.resolver(resolvers));
    }

    /** The message with each entry of {@code args} as an unparsed placeholder: player input never becomes tags. */
    public Component get(String key, Map<String, String> args) {
        return get(key, args.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new));
    }

    /** The text of a refused request, for the player who made it. */
    public Component error(PvpException e) {
        return get(e.messageKey(), e.args());
    }

    /** The message as legacy text with § codes, for the proxy's cross-server broadcast. */
    public String legacy(String key, TagResolver... resolvers) {
        return LegacyComponentSerializer.legacySection().serialize(get(key, resolvers));
    }

    /** The message without any formatting. */
    public String plain(String key, TagResolver... resolvers) {
        return PlainTextComponentSerializer.plainText().serialize(get(key, resolvers));
    }

    public boolean has(String key) {
        return texts.containsKey(key);
    }

    /**
     * The raw color of theme entry {@code name} (for example "danger"); empty when the theme has none by that
     * name.
     */
    public Optional<TextColor> themeColor(String name) {
        return Optional.ofNullable(theme.get(name));
    }

    /** Every message key, the fallback's included, sorted. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(new TreeSet<>(texts.keySet()));
    }

    /** A named color such as {@code gold}, or {@code #rrggbb}; null when it is neither. */
    private static TextColor parseColor(String value) {
        String color = value.trim().toLowerCase(Locale.ROOT);
        if (HEX_COLOR.matcher(color).matches()) {
            return TextColor.fromHexString(color);
        }
        return NamedTextColor.NAMES.value(color);
    }
}
