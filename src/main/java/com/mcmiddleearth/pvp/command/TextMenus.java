package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.BoolSetting;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The screens of {@link Menus} as plain chat lines: what the console, and any other sender that is not a player,
 * gets. Refusals ({@code PvpException}) are left to the calling command.
 */
public final class TextMenus implements Menus {

    /** Games per page of /pvp games. */
    static final int PAGE_SIZE = 8;

    private final PvpServices services;
    private final Messages messages;

    public TextMenus(PvpServices services) {
        this.services = services;
        this.messages = services.messages();
    }

    /** The open games, then the command overview. */
    @Override
    public void mainMenu(Player player) {
        games(player, 1);
        player.sendMessage(messages.get(Msg.HELP));
    }

    @Override
    public void games(CommandSender sender, int page) {
        List<Game> games = services.games().games();
        sender.sendMessage(messages.get(Msg.GAMES_HEADER));
        if (games.isEmpty()) {
            sender.sendMessage(messages.get(Msg.GAMES_NONE));
            return;
        }
        int pages = (games.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int shown = Math.max(1, Math.min(page, pages));
        for (Game game : games.subList((shown - 1) * PAGE_SIZE, Math.min(games.size(), shown * PAGE_SIZE))) {
            sender.sendMessage(messages.get(Msg.GAMES_ENTRY, Map.of(
                    "arena", game.arena().id(),
                    "mode", modeName(game.mode().id()),
                    "count", String.valueOf(game.playerCount()),
                    "max", String.valueOf(game.maxPlayers()),
                    "status", messages.plain(Msg.phase(game.phase().name())))));
        }
        if (pages > 1) {
            sender.sendMessage(messages.get(Msg.UI_CHAT_PAGE,
                    Map.of("page", String.valueOf(shown), "pages", String.valueOf(pages))));
            if (shown < pages) {
                sender.sendMessage(messages.get(Msg.UI_CHAT_PAGE_NEXT, Map.of("next", String.valueOf(shown + 1))));
            }
        }
    }

    /** Without an arena: the playable arenas and which game holds a busy one. With one: its settings, as usage. */
    @Override
    public void startGame(Player player, Optional<Arena> arena) {
        if (arena.isPresent()) {
            List<SettingSpec> specs = services.games().settingsFor(arena.get());
            // settingsFor already refused an arena whose mode isn't found, so the fallback below never fires.
            String modeId = services.modes().get(arena.get().modeId()).map(GameMode::id).orElse(arena.get().modeId());
            player.sendMessage(messages.get(Msg.ADMIN_START_USAGE, Map.of(
                    "arena", arena.get().id(),
                    "mode", modeName(modeId),
                    "settings", specs.stream().map(TextMenus::describe).collect(Collectors.joining(", ")))));
            return;
        }
        // An arena only needs a mode id to count as "playable"; skip one whose id this build doesn't have a mode for.
        List<Arena> known = services.arenas().playable().stream()
                .filter(candidate -> services.modes().get(candidate.modeId()).isPresent())
                .toList();
        player.sendMessage(messages.get(Msg.ADMIN_ARENA_LIST_HEADER, Map.of("count", String.valueOf(known.size()))));
        for (Arena candidate : known) {
            String modeId = services.modes().get(candidate.modeId()).orElseThrow().id();
            Optional<Game> holder = services.games().blockerFor(candidate);
            player.sendMessage(holder.isPresent()
                    ? messages.get(Msg.UI_PICK_BLOCKED,
                            Map.of("arena", candidate.id(), "holder", holder.get().arena().id()))
                    : messages.get(Msg.ADMIN_ARENA_LIST_ENTRY,
                            Map.of("arena", candidate.id(), "mode", modeName(modeId))));
        }
    }

    @Override
    public void rules(CommandSender sender, Optional<GameMode> mode) {
        if (mode.isPresent()) {
            String id = mode.get().id();
            sender.sendMessage(messages.get(Msg.RULES_HEADER, Map.of("mode", modeName(id))));
            sender.sendMessage(messages.get(Msg.modeRules(id)));
            return;
        }
        sender.sendMessage(messages.get(Msg.RULES_LIST_HEADER));
        for (GameMode each : services.modes().all()) {
            sender.sendMessage(messages.get(Msg.RULES_LIST_ENTRY,
                    Map.of("mode", modeName(each.id()), "id", each.id())));
        }
    }

    @Override
    public void stats(CommandSender sender, PlayerStats stats) {
        sender.sendMessage(messages.get(Msg.STATS_HEADER, Map.of("player", stats.name())));
        sender.sendMessage(messages.get(Msg.STATS_KILLS, Map.of(
                "kills", String.valueOf(stats.kills()),
                "deaths", String.valueOf(stats.deaths()),
                // 1.1.2 showed the lifetime K/D with two decimals ("#0.00").
                "kd", String.format(Locale.ROOT, "%.2f", stats.killDeathRatio()))));
        sender.sendMessage(messages.get(Msg.STATS_GAMES, Map.of(
                "played", String.valueOf(stats.gamesPlayed()),
                "won", String.valueOf(stats.gamesWon()),
                "lost", String.valueOf(stats.gamesLost()),
                "spectated", String.valueOf(stats.gamesSpectated()))));
    }

    /** One setting as it can be typed, with its range: "teamCount=2 (2-4)", "midgameJoin=true (true/false)". */
    static String describe(SettingSpec spec) {
        String range = spec.setting() instanceof BoolSetting ? "true/false" : spec.min() + "-" + spec.max();
        return spec.setting().id() + "=" + spec.defaultValue() + " (" + range + ")";
    }

    private String modeName(String modeId) {
        return messages.plain(Msg.modeName(modeId));
    }
}
