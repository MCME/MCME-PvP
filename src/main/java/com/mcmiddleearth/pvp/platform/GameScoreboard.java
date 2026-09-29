package com.mcmiddleearth.pvp.platform;

import com.mcmiddleearth.pvp.team.TeamSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.List;

/**
 * The scoreboard of one game: a Bukkit {@link Scoreboard} of its own, shown to the game's players and spectators,
 * with one scoreboard team per color so name tags and the tab list show it. Two games can both have a "red" team,
 * and the server's main scoreboard is never changed.
 */
public final class GameScoreboard {

    private final Scoreboard board;
    private final Scoreboard main;

    GameScoreboard(ScoreboardManager manager) {
        this.board = manager.getNewScoreboard();
        this.main = manager.getMainScoreboard();
    }

    /** The game's board; the HUD draws the sidebar objective on it. */
    public Scoreboard board() {
        return board;
    }

    /** Shows this board to the player and puts them in the scoreboard team of {@code color} (null: no team). */
    public void add(Player player, TeamSpec color) {
        leaveTeams(player.getName());
        if (color != null) {
            team(color).addEntry(player.getName());
        }
        player.setScoreboard(board);
    }

    /** Takes the player out of this board's teams and, if they are looking at it, back to the main scoreboard. */
    public void remove(Player player) {
        leaveTeams(player.getName());
        if (player.getScoreboard() == board) {
            player.setScoreboard(main);
        }
    }

    /** Unregisters every team and objective of this board and moves anyone still looking at it to the main board. */
    public void dispose() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == board) {
                player.setScoreboard(main);
            }
        }
        List.copyOf(board.getTeams()).forEach(Team::unregister);
        List.copyOf(board.getObjectives()).forEach(Objective::unregister);
    }

    private void leaveTeams(String entry) {
        Team current = board.getEntryTeam(entry);
        if (current != null) {
            current.removeEntry(entry);
        }
    }

    private Team team(TeamSpec color) {
        Team team = board.getTeam(color.id());
        if (team != null) {
            return team;
        }
        team = board.registerNewTeam(color.id());
        team.color(color.color());
        team.displayName(Component.text(color.name(), color.color()));
        // Real teams protect their members and let them pass through each other; personal colors (modes without
        // teams) only color the name tag.
        team.setAllowFriendlyFire(color.personal());
        team.setOption(Team.Option.COLLISION_RULE,
                color.personal() ? Team.OptionStatus.ALWAYS : Team.OptionStatus.FOR_OTHER_TEAMS);
        // Personal colors (crowded FFA) don't share a team identity, so members shouldn't see each other invisible.
        team.setCanSeeFriendlyInvisibles(!color.personal());
        return team;
    }
}
