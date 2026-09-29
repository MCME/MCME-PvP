package com.mcmiddleearth.pvp.platform.ui;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The HUD state of one game: its boss bar and who sees it, and the sidebar lines it has drawn. */
final class GameHud {

    static final String OBJECTIVE = "pvp_sidebar";

    private final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    private final Set<UUID> barViewers = new HashSet<>();
    private final Set<String> lines = new HashSet<>();
    private boolean frozen;

    /**
     * Draws the sidebar on the game's board. Lines are classic entries with scores (Paper's custom score names
     * do not exist in MockBukkit); a line that is gone is reset, because resetting by entry name is unreliable
     * in MockBukkit.
     */
    void sidebar(Scoreboard board, Component title, Map<String, Integer> wanted) {
        Objective objective = board.getObjective(OBJECTIVE);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(title);
        }
        for (String line : Set.copyOf(lines)) {
            if (!wanted.containsKey(line)) {
                objective.getScore(line).resetScore();
                lines.remove(line);
            }
        }
        for (Map.Entry<String, Integer> line : wanted.entrySet()) {
            objective.getScore(line.getKey()).setScore(line.getValue());
            lines.add(line.getKey());
        }
    }

    /** Updates the boss bar and shows it to exactly {@code viewers} (online ones), hiding it from everyone else. */
    void bar(Set<UUID> viewers, Component name, float progress, BossBar.Color color) {
        bar.name(name).progress(progress).color(color);
        for (UUID viewer : Set.copyOf(barViewers)) {
            if (!viewers.contains(viewer)) {
                hide(viewer);
            }
        }
        for (UUID viewer : viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null && barViewers.add(viewer)) {
                player.showBossBar(bar);
            }
        }
    }

    void hideBar() {
        Set.copyOf(barViewers).forEach(this::hide);
    }

    /** Once true, the sidebar and the bar are left as they are: the result stays as it was at the end. */
    boolean frozen() {
        return frozen;
    }

    void freeze() {
        frozen = true;
    }

    private void hide(UUID viewer) {
        barViewers.remove(viewer);
        Player player = Bukkit.getPlayer(viewer);
        if (player != null) {
            player.hideBossBar(bar);
        }
    }
}
