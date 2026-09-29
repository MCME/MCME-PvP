package com.mcmiddleearth.pvp;

import com.mcmiddleearth.pvp.arena.ArenaRegistry;
import com.mcmiddleearth.pvp.arena.ArenaStore;
import com.mcmiddleearth.pvp.arena.LegacyMapImporter;
import com.mcmiddleearth.pvp.arena.LoadReport;
import com.mcmiddleearth.pvp.command.Menus;
import com.mcmiddleearth.pvp.command.PvpCommands;
import com.mcmiddleearth.pvp.command.TextMenus;
import com.mcmiddleearth.pvp.config.ConfigMigrator;
import com.mcmiddleearth.pvp.config.PvpConfig;
import com.mcmiddleearth.pvp.game.GameListeners;
import com.mcmiddleearth.pvp.game.GameManager;
import com.mcmiddleearth.pvp.integration.ProxyMessenger;
import com.mcmiddleearth.pvp.integration.PvpExpansion;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.mode.ModeRegistry;
import com.mcmiddleearth.pvp.mode.ffa.FreeForAll;
import com.mcmiddleearth.pvp.mode.tdm.TeamDeathmatch;
import com.mcmiddleearth.pvp.platform.ActionBarHolds;
import com.mcmiddleearth.pvp.platform.CrashRecovery;
import com.mcmiddleearth.pvp.platform.GameEventRouter;
import com.mcmiddleearth.pvp.platform.LobbyProtection;
import com.mcmiddleearth.pvp.platform.LobbyRespawn;
import com.mcmiddleearth.pvp.platform.LockListener;
import com.mcmiddleearth.pvp.platform.Locations;
import com.mcmiddleearth.pvp.platform.PaperGameContextFactory;
import com.mcmiddleearth.pvp.platform.PaperLobby;
import com.mcmiddleearth.pvp.platform.ServerLock;
import com.mcmiddleearth.pvp.platform.SupplyChests;
import com.mcmiddleearth.pvp.platform.TextAnnouncer;
import com.mcmiddleearth.pvp.platform.WorldSetup;
import com.mcmiddleearth.pvp.platform.ui.ChatRenderer;
import com.mcmiddleearth.pvp.platform.ui.DialogRenderer;
import com.mcmiddleearth.pvp.platform.ui.DialogTracker;
import com.mcmiddleearth.pvp.platform.ui.HudRenderer;
import com.mcmiddleearth.pvp.platform.ui.PaperUiCallbacks;
import com.mcmiddleearth.pvp.platform.ui.UiAnnouncer;
import com.mcmiddleearth.pvp.platform.ui.UiMenus;
import com.mcmiddleearth.pvp.platform.ui.UiService;
import com.mcmiddleearth.pvp.stats.JsonStatsStore;
import com.mcmiddleearth.pvp.stats.JsonlMatchHistoryStore;
import com.mcmiddleearth.pvp.stats.LegacyStatsImporter;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import com.mcmiddleearth.pvp.stats.StatsRecorder;
import com.mcmiddleearth.pvp.stats.StatsService;
import com.mcmiddleearth.pvp.storage.AsyncWriter;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.ui.UiScreens;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** MCME-PvP 3: the composition root. Every service is created here and passed on by constructor. */
public class PvpPlugin extends JavaPlugin {

    private AsyncWriter writer;
    private PvpServices services;
    private PvpExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PvpConfig config = loadConfig();
        Messages messages = loadMessages();
        Path data = getDataFolder().toPath();
        writer = new AsyncWriter(getLogger());

        ModeRegistry modes = new ModeRegistry(List.of(new TeamDeathmatch(), new FreeForAll()));
        Function<String, Optional<String>> modeForLegacyName = gmType -> modes.byLegacyName(gmType).map(GameMode::id);
        ArenaRegistry arenas = new ArenaRegistry(new ArenaStore(data.resolve("arenas"), Json.pretty()),
                new LegacyMapImporter(modeForLegacyName, Clock.systemUTC()), data.resolve("maps"), modeForLegacyName,
                getLogger());
        logReport(arenas.load());

        StatsService stats = new StatsService(
                new JsonStatsStore(data.resolve("players"), Json.pretty(), writer, getLogger()),
                new LegacyStatsImporter(data.resolve("stats"), Json.pretty(), getLogger()));

        WorldSetup.apply(config, getLogger());
        if (Locations.location(config.lobby()).isEmpty()) {
            getLogger().warning("The lobby's world " + config.lobby().world()
                    + " isn't loaded: players who leave a game stay where they are; add it to worlds in "
                    + "config.yml, or correct lobby.world.");
        }
        PaperLobby lobby = new PaperLobby(config.lobby());
        PaperGameContextFactory contexts = new PaperGameContextFactory(this, lobby);
        GameListeners listeners = new GameListeners(getLogger());
        GameManager games = new GameManager(arenas, modes, config, contexts, listeners,
                id -> stats.find(id).map(PlayerStats::killDeathRatio).orElse(0.0));
        ProxyMessenger proxy = new ProxyMessenger(this, config.mainServer());
        proxy.register();
        services = new PvpServices(config, messages, arenas, modes, games, stats, lobby,
                new ServerLock(config.startLocked()), proxy);
        ActionBarHolds holds = new ActionBarHolds(getServer()::getCurrentTick);
        SupplyChests supplyChests = new SupplyChests(services, this, holds);

        listeners.add(new StatsRecorder(stats, new JsonlMatchHistoryStore(data.resolve("matches"), Json.compact(), writer)));
        listeners.add(new TextAnnouncer(services));
        DialogTracker dialogs = new DialogTracker();
        PaperUiCallbacks callbacks = new PaperUiCallbacks(this, messages, dialogs);
        UiService ui = new UiService(new DialogRenderer(callbacks, messages), new ChatRenderer(callbacks, messages),
                dialogs, messages, getLogger());
        UiScreens screens = new UiScreens(services, ui);
        UiAnnouncer uiAnnouncer = new UiAnnouncer(services, screens, ui, this::isEnabled);
        listeners.add(dialogs);
        listeners.add(new HudRenderer(services, contexts, holds));
        listeners.add(uiAnnouncer);
        listeners.add(supplyChests);

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(dialogs, this);
        plugins.registerEvents(uiAnnouncer, this);
        plugins.registerEvents(new GameEventRouter(services, this), this);
        plugins.registerEvents(supplyChests, this);
        plugins.registerEvents(new LobbyProtection(services), this);
        plugins.registerEvents(new LobbyRespawn(services, lobby), this);
        plugins.registerEvents(new CrashRecovery(services, lobby, contexts.marker()), this);
        LockListener lockListener = new LockListener(services, this);
        plugins.registerEvents(lockListener, this);
        lockListener.start();

        Menus menus = new UiMenus(screens, ui, new TextMenus(services));
        new PvpCommands(services, menus).register(this);

        if (plugins.isPluginEnabled("PlaceholderAPI")) {
            expansion = new PvpExpansion(games, getPluginMeta().getVersion());
            if (!expansion.register()) {
                getLogger().warning("Could not register the PlaceholderAPI expansion, so its placeholders are unavailable.");
            }
        }
    }

    @Override
    public void onDisable() {
        RuntimeException failure = null;
        try {
            try {
                if (services != null) {
                    services.games().closeAll();
                }
            } catch (RuntimeException e) {
                failure = e;
            }
            try {
                if (expansion != null) {
                    expansion.unregister();
                }
            } catch (RuntimeException e) {
                failure = firstOf(failure, e);
            }
        } finally {
            // Drains the queued saves even when a game failed to close.
            if (writer != null) {
                writer.close();
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Keeps the first failure and attaches any later one to it, so neither is lost. */
    private static RuntimeException firstOf(RuntimeException first, RuntimeException next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    /** The shared services, for tests. */
    public PvpServices services() {
        return services;
    }

    /**
     * config.yml, migrated from 1.1.2/2.0.0 first. A file with a YAML error is left exactly as it is: Bukkit reads it
     * as empty, and migrating that would save the defaults over every setting. The plugin then runs on the defaults.
     */
    private PvpConfig loadConfig() {
        Optional<String> error = ConfigMigrator.yamlError(getDataFolder().toPath().resolve("config.yml"));
        if (error.isPresent()) {
            getLogger().severe("config.yml has a YAML error, so it is left as it is and the default settings are used"
                    + " until it is fixed and the server restarts: " + error.get());
            return PvpConfig.defaults();
        }
        if (ConfigMigrator.migrate(getConfig())) {
            saveConfig();
            getLogger().info("config.yml migrated to version " + ConfigMigrator.CURRENT_VERSION);
        }
        return PvpConfig.from(getConfig());
    }

    private Messages loadMessages() {
        Path file = getDataFolder().toPath().resolve("messages.yml");
        if (Files.notExists(file)) {
            saveResource("messages.yml", false);
        }
        try {
            return Messages.load(file, Messages.bundled());
        } catch (IOException e) {
            getLogger().warning("messages.yml could not be loaded, so the built-in texts are used: " + e.getMessage());
            return Messages.bundled();
        }
    }

    private void logReport(LoadReport report) {
        getLogger().info("Arenas: " + report.summary());
        // One line per reason, not per arena: until more modes exist, many imported maps share the same reason.
        Map<String, List<String>> byReason = new TreeMap<>();
        report.unassigned().forEach((id, reason) -> byReason.computeIfAbsent(reason, key -> new ArrayList<>()).add(id));
        byReason.forEach((reason, ids) -> getLogger().info("Unassigned arenas (" + reason + "): " + String.join(", ", ids)));
        // Unused map keys are news only when the map is imported; the arena file keeps the list after that.
        report.unknownProperties().forEach((id, keys) -> {
            if (report.imported().contains(id)) {
                getLogger().info("Arena " + id + " was imported; its map file also had keys 3.0 does not use: " + keys);
            }
        });
        report.failed().forEach((file, error) -> getLogger().warning("Could not load " + file + ": " + error));
    }
}
