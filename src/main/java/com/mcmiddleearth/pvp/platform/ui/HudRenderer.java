package com.mcmiddleearth.pvp.platform.ui;

import com.mcmiddleearth.pvp.PvpServices;
import com.mcmiddleearth.pvp.game.EndReason;
import com.mcmiddleearth.pvp.game.Game;
import com.mcmiddleearth.pvp.game.GameId;
import com.mcmiddleearth.pvp.game.GameListener;
import com.mcmiddleearth.pvp.game.GameTeam;
import com.mcmiddleearth.pvp.game.JoinResult;
import com.mcmiddleearth.pvp.game.LeaveReason;
import com.mcmiddleearth.pvp.game.Outcome;
import com.mcmiddleearth.pvp.game.Phase;
import com.mcmiddleearth.pvp.game.PlayerSession;
import com.mcmiddleearth.pvp.game.Role;
import com.mcmiddleearth.pvp.platform.ActionBarHolds;
import com.mcmiddleearth.pvp.platform.PaperGameContextFactory;
import com.mcmiddleearth.pvp.stats.MatchRecord;
import com.mcmiddleearth.pvp.text.Messages;
import com.mcmiddleearth.pvp.text.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The live game state on screen, per game. The sidebar, on the game's own scoreboard, has the mode and
 * the waiting count, countdown or time left in its title, and as lines each team with the players it has left, or
 * (without teams) the top five by score. A boss bar shows the countdown and the time limit to the game's players and
 * spectators. While the game runs, the action bar shows each player their kills and deaths, "out" or "spectating".
 * Titles mark the last five seconds of the countdown, the start, elimination and the result.
 */
public final class HudRenderer implements GameListener {

    private static final int TOP = 5;
    private static final int COUNTDOWN_TITLES = 5;
    private static final Title.Times SHORT = Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(250));
    private static final Title.Times LONG = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofSeconds(1));

    private final PvpServices services;
    private final Messages messages;
    private final PaperGameContextFactory contexts;
    private final ActionBarHolds holds;
    private final TitleSender titles;
    private final Map<GameId, GameHud> huds = new HashMap<>();

    public HudRenderer(PvpServices services, PaperGameContextFactory contexts, ActionBarHolds holds) {
        this(services, contexts, holds, Player::showTitle);
    }

    HudRenderer(PvpServices services, PaperGameContextFactory contexts, ActionBarHolds holds, TitleSender titles) {
        this.services = services;
        this.messages = services.messages();
        this.contexts = contexts;
        this.holds = holds;
        this.titles = titles;
    }

    @Override
    public void gameOpened(Game game, String openedBy) {
        refresh(game);
    }

    @Override
    public void phaseChanged(Game game, Phase from, Phase to) {
        if (to == Phase.CLOSED) {
            close(game);
            return;
        }
        refresh(game);
        if (to == Phase.COUNTDOWN) {
            countdownTitle(game);
        } else if (to == Phase.RUNNING) {
            game.audience().forEach(id -> title(id, Msg.TITLE_FIGHT, Map.of(), SHORT));
        }
    }

    @Override
    public void playerJoined(Game game, PlayerSession session, JoinResult result) {
        refresh(game);
    }

    @Override
    public void playerLeft(Game game, PlayerSession session, LeaveReason reason) {
        if (game.phase() == Phase.ENDING) {
            // The result title may still be up; the player is off to the lobby or to another game.
            Player player = Bukkit.getPlayer(session.id());
            if (player != null) {
                titles.clear(player);
            }
        }
        refresh(game);
    }

    @Override
    public void playerDied(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        if (victim.present() && !victim.alive()) {
            title(victim.id(), Msg.TITLE_ELIMINATED, Map.of(), LONG);
        }
        refresh(game);
    }

    /**
     * A fleeing player is already headed to the lobby, so {@link #playerDied}'s default "You're out" title would
     * only flash there and confuse; show no title, just refresh what the others see.
     */
    @Override
    public void playerFled(Game game, PlayerSession victim, Optional<PlayerSession> killer) {
        refresh(game);
    }

    @Override
    public void scoreChanged(Game game) {
        refresh(game);
    }

    @Override
    public void secondPassed(Game game) {
        refresh(game);
        if (game.phase() == Phase.COUNTDOWN) {
            countdownTitle(game);
        } else if (game.phase() == Phase.RUNNING) {
            actionBars(game);
        }
    }

    /**
     * Victory, defeat or draw for each participant still in the game, as the match record has it; none when staff
     * stopped the game or the server is shutting down, since chat already says so.
     */
    @Override
    public void gameEnded(Game game, Outcome outcome, MatchRecord record) {
        if (outcome.reason() != EndReason.STOPPED && outcome.reason() != EndReason.SHUTDOWN) {
            for (MatchRecord.Participant participant : record.participants()) {
                String key = switch (participant.result()) {
                    case MatchRecord.WON -> Msg.TITLE_VICTORY;
                    case MatchRecord.LOST -> Msg.TITLE_DEFEAT;
                    case MatchRecord.DRAW -> Msg.TITLE_DRAW;
                    default -> null;
                };
                UUID id = UUID.fromString(participant.uuid());
                if (key != null && game.session(id).map(PlayerSession::present).orElse(false)) {
                    title(id, key, Map.of(), LONG);
                }
            }
        }
        refresh(game);
    }

    @Override
    public void gameClosed(Game game) {
        close(game);
    }

    private void refresh(Game game) {
        if (game.phase() == Phase.CLOSED) {
            return;
        }
        GameHud hud = huds.computeIfAbsent(game.id(), id -> new GameHud());
        if (hud.frozen()) {
            return;
        }
        contexts.scoreboard(game.id()).ifPresent(board -> hud.sidebar(board.board(), sidebarTitle(game), lines(game)));
        boolean countdown = game.phase() == Phase.COUNTDOWN;
        boolean clock = game.phase() == Phase.RUNNING && game.timeLimitSeconds() > 0;
        if (countdown || clock) {
            int total = countdown ? services.config().countdownSeconds() : game.timeLimitSeconds();
            Component name = countdown
                    ? messages.get(Msg.HUD_BOSSBAR_COUNTDOWN, Map.of("seconds", String.valueOf(game.secondsLeft())))
                    : messages.get(Msg.HUD_BOSSBAR_TIME, Map.of("mode", modeName(game), "time", time(game.secondsLeft())));
            hud.bar(Set.copyOf(game.audience()), name, Math.clamp((float) game.secondsLeft() / total, 0f, 1f),
                    countdown ? BossBar.Color.YELLOW : BossBar.Color.GREEN);
        } else {
            hud.hideBar();
        }
        if (game.phase() == Phase.ENDING) {
            // The result stays as it was at the end while its players move on to other games.
            hud.freeze();
        }
    }

    private Component sidebarTitle(Game game) {
        Component mode = messages.get(Msg.HUD_SIDEBAR_TITLE, Map.of("mode", modeName(game)));
        Optional<Component> status = switch (game.phase()) {
            case WAITING -> Optional.of(messages.get(Msg.HUD_SIDEBAR_WAITING,
                    Map.of("count", String.valueOf(game.playerCount()), "max", String.valueOf(game.maxPlayers()))));
            case COUNTDOWN -> Optional.of(messages.get(Msg.HUD_SIDEBAR_COUNTDOWN,
                    Map.of("seconds", String.valueOf(game.secondsLeft()))));
            case RUNNING -> game.timeLimitSeconds() > 0
                    ? Optional.of(messages.get(Msg.HUD_SIDEBAR_TIME, Map.of("time", time(game.secondsLeft()))))
                    : Optional.empty();
            case ENDING, CLOSED -> Optional.empty();
        };
        return status.map(text -> Component.join(JoinConfiguration.spaces(), mode, text)).orElse(mode);
    }

    /** Each team with the players it has left, or the five best present players by score; nothing while waiting. */
    private Map<String, Integer> lines(Game game) {
        Map<String, Integer> lines = new LinkedHashMap<>();
        if (game.phase() == Phase.WAITING) {
            return lines;
        }
        if (!game.teams().isEmpty()) {
            for (GameTeam team : game.teams()) {
                String entry = LegacyComponentSerializer.legacySection()
                        .serialize(Component.text(team.spec().name(), team.spec().color()));
                lines.put(entry, team.aliveCount());
            }
            return lines;
        }
        // Player names as entries take the player's scoreboard-team color, like their name tag.
        game.scores().ranking().stream()
                .filter(session -> session.present() && session.role() == Role.PLAYER)
                .limit(TOP)
                .forEach(session -> lines.put(session.name(), session.score()));
        return lines;
    }

    private void actionBars(Game game) {
        for (PlayerSession session : game.sessions()) {
            Player player = session.present() ? Bukkit.getPlayer(session.id()) : null;
            if (player == null || holds.held(session.id())) {
                continue;
            }
            if (session.role() == Role.SPECTATOR) {
                player.sendActionBar(messages.get(Msg.HUD_ACTIONBAR_SPECTATING, Map.of("arena", game.arena().id())));
            } else if (session.alive()) {
                player.sendActionBar(messages.get(Msg.HUD_ACTIONBAR_SCORE,
                        Map.of("kills", String.valueOf(session.kills()), "deaths", String.valueOf(session.deaths()))));
            } else {
                player.sendActionBar(messages.get(Msg.HUD_ACTIONBAR_OUT));
            }
        }
    }

    private void countdownTitle(Game game) {
        int seconds = game.secondsLeft();
        if (seconds >= 1 && seconds <= COUNTDOWN_TITLES) {
            game.audience().forEach(id -> title(id, Msg.TITLE_COUNTDOWN, Map.of("seconds", String.valueOf(seconds)), SHORT));
        }
    }

    private void title(UUID id, String key, Map<String, String> args, Title.Times times) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            titles.show(player, Title.title(messages.get(key, args), Component.empty(), times));
        }
    }

    private void close(Game game) {
        GameHud hud = huds.remove(game.id());
        if (hud != null) {
            hud.hideBar();
        }
    }

    private String modeName(Game game) {
        return messages.plain(Msg.modeName(game.mode().id()));
    }

    /** "9:59": minutes and seconds. */
    private static String time(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
