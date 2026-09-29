package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.Permissions;
import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.SettingSpec;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Registers {@code /pvp}, {@code /pvpadmin} and {@code /t} with Paper's Brigadier API. This class only parses:
 * permissions are {@code requires} checks, suggestions come from the registries, and every executor calls
 * {@link PlayerCommands}, {@link AdminCommands} or {@link TeamChatCommand}.
 */
public final class PvpCommands {

    private final PvpServices services;
    private final Menus menus;

    public PvpCommands(PvpServices services, Menus menus) {
        this.services = services;
        this.menus = menus;
    }

    /** Registers the commands; Paper builds them when the server (re)loads its command tree. */
    public void register(JavaPlugin plugin) {
        PlayerCommands player = new PlayerCommands(services, menus);
        AdminCommands admin = new AdminCommands(services, menus, plugin.getLogger());
        TeamChatCommand teamChat = new TeamChatCommand(services);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(pvp(player), "PvP games: join, leave, spectate, rules and stats", List.of());
            commands.register(pvpAdmin(admin), "PvP staff: start and stop games, kick, lock, arenas", List.of());
            commands.register(teamChat(teamChat), "Chat with your PvP team", List.of());
        });
    }

    private LiteralCommandNode<CommandSourceStack> pvp(PlayerCommands player) {
        return Commands.literal("pvp")
                .requires(has(Permissions.PLAY))
                .executes(ctx -> done(() -> player.root(sender(ctx))))
                .then(Commands.literal("join")
                        .executes(ctx -> done(() -> player.join(sender(ctx), Optional.empty())))
                        .then(word("arena", this::openArenas)
                                .executes(ctx -> done(() -> player.join(sender(ctx), Optional.of(text(ctx, "arena")))))))
                .then(Commands.literal("leave")
                        .executes(ctx -> done(() -> player.leave(sender(ctx)))))
                .then(Commands.literal("spectate")
                        .executes(ctx -> done(() -> player.usage(sender(ctx), "/pvp spectate <arena>")))
                        .then(word("arena", this::openArenas)
                                .executes(ctx -> done(() -> player.spectate(sender(ctx), text(ctx, "arena"))))))
                .then(Commands.literal("games")
                        .executes(ctx -> done(() -> player.games(sender(ctx), 1)))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> done(() -> player.games(sender(ctx),
                                        IntegerArgumentType.getInteger(ctx, "page"))))))
                .then(Commands.literal("rules")
                        .executes(ctx -> done(() -> player.rules(sender(ctx), Optional.empty())))
                        .then(word("mode", this::modes)
                                .executes(ctx -> done(() -> player.rules(sender(ctx), Optional.of(text(ctx, "mode")))))))
                .then(Commands.literal("stats")
                        .executes(ctx -> done(() -> player.stats(sender(ctx), Optional.empty())))
                        .then(playerWord()
                                .executes(ctx -> done(() -> player.stats(sender(ctx), Optional.of(text(ctx, "player")))))))
                .then(Commands.literal("lobby")
                        .executes(ctx -> done(() -> player.lobby(sender(ctx)))))
                .build();
    }

    private LiteralCommandNode<CommandSourceStack> pvpAdmin(AdminCommands admin) {
        return Commands.literal("pvpadmin")
                .requires(source -> isStaff(source.getSender()))
                .executes(ctx -> done(() -> admin.help(sender(ctx))))
                .then(Commands.literal("start")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.start(sender(ctx), Optional.empty(), Optional.empty())))
                        .then(word("arena", this::playableArenas)
                                .executes(ctx -> done(() -> admin.start(sender(ctx), Optional.of(text(ctx, "arena")),
                                        Optional.empty())))
                                .then(Commands.argument("settings", StringArgumentType.greedyString())
                                        .suggests(this::suggestSettings)
                                        .executes(ctx -> done(() -> admin.start(sender(ctx), Optional.of(text(ctx, "arena")),
                                                Optional.of(text(ctx, "settings"))))))))
                .then(Commands.literal("countdown")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin countdown <arena>")))
                        .then(word("arena", this::waitingArenas)
                                .executes(ctx -> done(() -> admin.countdown(sender(ctx), text(ctx, "arena"))))))
                .then(Commands.literal("stop")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin stop <arena>")))
                        .then(word("arena", this::gameArenas)
                                .executes(ctx -> done(() -> admin.stop(sender(ctx), text(ctx, "arena"))))))
                .then(Commands.literal("kick")
                        .requires(has(Permissions.STAFF_KICK))
                        .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin kick <player>")))
                        .then(playerWord()
                                .executes(ctx -> done(() -> admin.kick(sender(ctx), text(ctx, "player"))))))
                .then(Commands.literal("kickall")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.kickAll(sender(ctx)))))
                .then(Commands.literal("lock")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.lock(sender(ctx)))))
                .then(Commands.literal("unlock")
                        .requires(has(Permissions.STAFF_RUN))
                        .executes(ctx -> done(() -> admin.unlock(sender(ctx)))))
                .then(Commands.literal("arena")
                        .requires(has(Permissions.STAFF_ARENA))
                        .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin arena list|info <id>|import <id>")))
                        .then(Commands.literal("list")
                                .executes(ctx -> done(() -> admin.arenaList(sender(ctx)))))
                        .then(Commands.literal("info")
                                .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin arena info <id>")))
                                .then(word("id", this::allArenas)
                                        .executes(ctx -> done(() -> admin.arenaInfo(sender(ctx), text(ctx, "id"))))))
                        .then(Commands.literal("import")
                                .executes(ctx -> done(() -> admin.usage(sender(ctx), "/pvpadmin arena import <id>")))
                                .then(word("id", this::allArenas)
                                        .executes(ctx -> done(() -> admin.arenaImport(sender(ctx), text(ctx, "id")))))))
                .build();
    }

    private LiteralCommandNode<CommandSourceStack> teamChat(TeamChatCommand teamChat) {
        return Commands.literal("t")
                .requires(has(Permissions.PLAY))
                .executes(ctx -> done(() -> teamChat.usage(sender(ctx))))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> done(() -> teamChat.send(sender(ctx), text(ctx, "message")))))
                .build();
    }

    /** A one-word argument that suggests {@code candidates}. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> word(String name,
                                                                            Supplier<Stream<String>> candidates) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> suggest(builder, candidates.get()));
    }

    /** Completes the last word of the greedy settings text with the settings of the arena typed before it. */
    private CompletableFuture<Suggestions> suggestSettings(CommandContext<CommandSourceStack> ctx,
                                                           SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        List<SettingSpec> specs = services.arenas().get(text(ctx, "arena"))
                .filter(arena -> arena.playable() && services.modes().get(arena.modeId()).isPresent())
                .map(services.games()::settingsFor)
                .orElse(List.of());
        SuggestionsBuilder last = builder.createOffset(builder.getStart() + typed.lastIndexOf(' ') + 1);
        SettingsArgument.suggestions(typed, specs).forEach(last::suggest);
        return last.buildFuture();
    }

    /** The candidates that start with what was typed, in any case: Brigadier leaves filtering to the provider. */
    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Stream<String> candidates) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        candidates.filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(typed)).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private Stream<String> openArenas() {
        return services.games().games().stream().filter(Game::acceptsJoins).map(game -> game.arena().id());
    }

    private Stream<String> waitingArenas() {
        return services.games().games().stream().filter(game -> game.phase() == Phase.WAITING)
                .map(game -> game.arena().id());
    }

    private Stream<String> gameArenas() {
        return services.games().games().stream().map(game -> game.arena().id());
    }

    private Stream<String> playableArenas() {
        return services.arenas().playable().stream().map(Arena::id);
    }

    private Stream<String> allArenas() {
        return services.arenas().all().stream().map(Arena::id);
    }

    private Stream<String> modes() {
        return services.modes().all().stream().map(GameMode::id);
    }

    /**
     * A {@code player} argument that suggests online players the sender can see: a vanished player stays out of
     * a player sender's suggestions, while console and other non-player senders see every online player.
     */
    private static RequiredArgumentBuilder<CommandSourceStack, String> playerWord() {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> suggest(builder, visiblePlayers(ctx.getSource().getSender())));
    }

    private static Stream<String> visiblePlayers(CommandSender sender) {
        Predicate<Player> visible = sender instanceof Player viewer ? viewer::canSee : player -> true;
        return Bukkit.getOnlinePlayers().stream().filter(visible).map(Player::getName);
    }

    private static boolean isStaff(CommandSender sender) {
        return sender.hasPermission(Permissions.STAFF_RUN) || sender.hasPermission(Permissions.STAFF_KICK)
                || sender.hasPermission(Permissions.STAFF_ARENA) || sender.hasPermission(Permissions.ADMIN);
    }

    private static Predicate<CommandSourceStack> has(String permission) {
        return source -> source.getSender().hasPermission(permission);
    }

    private static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    private static String text(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    /** Runs a command body; the bodies answer the sender themselves, so every command "succeeds". */
    private static int done(Runnable body) {
        body.run();
        return Command.SINGLE_SUCCESS;
    }
}
