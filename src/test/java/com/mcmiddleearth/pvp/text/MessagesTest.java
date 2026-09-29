package com.mcmiddleearth.pvp.text;

import com.mcmiddleearth.pvp.PvpException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    private static final String YAML = """
            theme:
              accent: "gold"
              muted: "#123456"
            greet: "<accent>Hi</accent> <muted>there"
            game:
              full: "Full (<max> players)"
              joined: "<player> joined"
            number: 5
            """;

    private final Messages messages = Messages.fromYaml(YAML, null);

    @Test
    void themeEntriesBecomeStylingTags() {
        Component greet = messages.get("greet");

        assertEquals(NamedTextColor.GOLD, colorOf(greet, "Hi"));
        assertEquals(TextColor.color(0x123456), colorOf(greet, "there"));
        assertEquals("Hi there", messages.plain("greet"));
    }

    @Test
    void themeColorLooksUpARawColorByItsThemeName() {
        assertEquals(Optional.of(NamedTextColor.GOLD), messages.themeColor("accent"));
        assertEquals(Optional.of(TextColor.color(0x123456)), messages.themeColor("muted"));
        assertEquals(Optional.empty(), messages.themeColor("no-such-name"));
    }

    @Test
    void mapArgumentsAreInsertedAsTextNotAsTags() {
        Component full = messages.get("game.full", Map.of("max", "<red>8"));

        assertEquals("Full (<red>8 players)", plain(full));
    }

    @Test
    void tagResolverArgumentsCanCarryStyledComponents() {
        Component joined = messages.get("game.joined",
                Placeholder.component("player", Component.text("Alice", NamedTextColor.AQUA)));

        assertEquals("Alice joined", plain(joined));
        assertEquals(NamedTextColor.AQUA, colorOf(joined, "Alice"));
    }

    @Test
    void lookupTakesTheFileThenTheFallbackThenTheKey() {
        Messages fallback = Messages.fromYaml("a: \"fallback a\"\nb: \"fallback b\"\n", null);
        Messages own = Messages.fromYaml("a: \"own a\"\n", fallback);

        assertEquals("own a", own.plain("a"));
        assertEquals("fallback b", own.plain("b"));
        assertEquals("c.missing", own.plain("c.missing"));
        assertEquals(Set.of("a", "b"), own.keys());
    }

    @Test
    void aMissingKeyIsShownAsTheKeyItself() {
        assertEquals(Component.text("error.not-there"), messages.get("error.not-there"));
        assertEquals("error.not-there", messages.legacy("error.not-there"));
        assertFalse(messages.has("error.not-there"));
    }

    @Test
    void legacyGivesSectionCodesForTheProxy() {
        Messages tick = Messages.fromYaml("theme:\n  accent: \"gold\"\ntick: \"<accent>Game begins in <seconds>\"\n", null);

        assertEquals("§6Game begins in 5", tick.legacy("tick", Placeholder.unparsed("seconds", "5")));
    }

    @Test
    void plainDropsAllFormatting() {
        Messages tick = Messages.fromYaml("theme:\n  accent: \"gold\"\ntick: \"<accent><bold>Game begins in <seconds>\"\n", null);

        assertEquals("Game begins in 5", tick.plain("tick", Placeholder.unparsed("seconds", "5")));
    }

    @Test
    void anInvalidThemeColorKeepsTheFallbacksColor() {
        Messages fallback = Messages.fromYaml("theme:\n  accent: \"gold\"\n  muted: \"gray\"\n", null);
        Messages own = Messages.fromYaml(
                "theme:\n  accent: \"not-a-color\"\n  muted: \"#00FF00\"\nx: \"<accent>a</accent><muted>b\"\n", fallback);

        assertEquals(NamedTextColor.GOLD, colorOf(own.get("x"), "a"));
        assertEquals(TextColor.color(0x00ff00), colorOf(own.get("x"), "b"));
    }

    @Test
    void onlyStringLeavesOutsideTheThemeAreMessages() {
        assertTrue(messages.has("game.full"));
        assertFalse(messages.has("game"), "a section is not a message");
        assertFalse(messages.has("number"), "a number is not a message");
        assertFalse(messages.has("theme.accent"), "theme entries are colors");
        assertEquals(Set.of("greet", "game.full", "game.joined"), messages.keys());
    }

    @Test
    void errorShowsTheExceptionsKeyWithItsArguments() {
        PvpException full = new PvpException(Msg.ERROR_GAME_FULL, Map.of("max", "8"));

        assertEquals("That game is full (8 players).", plain(Messages.bundled().error(full)));
    }

    @Test
    void theBundledFileAppliesItsTheme() {
        assertEquals("§cYou don't have permission to do that.", Messages.bundled().legacy(Msg.ERROR_NO_PERMISSION));
    }

    @Test
    void loadReadsAFileOnTopOfTheFallback(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("messages.yml");
        Files.writeString(file, "error:\n  no-permission: \"<danger>Nope.\"\n");

        Messages loaded = Messages.load(file, Messages.bundled());

        assertEquals("§cNope.", loaded.legacy(Msg.ERROR_NO_PERMISSION));
        assertEquals("You're not in a game.", loaded.plain(Msg.ERROR_NOT_IN_GAME));
    }

    @Test
    void aBrokenFileFailsWithAClearMessage(@TempDir Path dir) throws IOException {
        // YamlConfiguration.loadConfiguration(Reader) (used by fromYaml) swallows a parse failure and logs it
        // through Bukkit.getLogger(), so load() must check the text strictly itself before handing it to fromYaml.
        Path file = dir.resolve("messages.yml");
        Files.writeString(file, "error:\n  no-permission: \"<danger>unterminated\n bad: [x\n");

        IOException e = assertThrows(IOException.class, () -> Messages.load(file, Messages.bundled()));

        assertTrue(e.getMessage().contains("messages.yml has a YAML error"), e.getMessage());
    }

    @Test
    void aStylingArgumentNamedColorWinsOverTheStandardTag() {
        // team-chat.format writes <color>…</color> for the team color, which is also a standard MiniMessage tag.
        Component line = Messages.bundled().get(Msg.TEAM_CHAT_FORMAT,
                Placeholder.styling("color", NamedTextColor.RED),
                Placeholder.unparsed("player", "Alice"),
                Placeholder.unparsed("message", "hi"));

        assertEquals("[TEAM] Alice: hi", plain(line));
        assertEquals(NamedTextColor.RED, colorOf(line, "Alice"));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * The color {@code text} is shown in: the color of the one text part that contains it, else its nearest
     * parent's. MiniMessage may merge neighbouring text into one part, so parts are matched by containment.
     */
    private static TextColor colorOf(Component root, String text) {
        List<TextColor> found = new ArrayList<>();
        collect(root, text, null, found);
        assertEquals(1, found.size(), "\"" + text + "\" found " + found.size() + " times in " + root);
        return found.getFirst();
    }

    private static void collect(Component component, String text, TextColor inherited, List<TextColor> found) {
        TextColor color = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent part && part.content().contains(text)) {
            found.add(color);
        }
        for (Component child : component.children()) {
            collect(child, text, color, found);
        }
    }
}
