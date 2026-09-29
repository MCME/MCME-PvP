package com.mcmiddleearth.pvp.command;

import com.mcmiddleearth.pvp.arena.Arena;
import com.mcmiddleearth.pvp.mode.GameMode;
import com.mcmiddleearth.pvp.stats.PlayerStats;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * The screens behind commands that show choices. {@code TextMenus} answers them in plain chat; {@code UiMenus}
 * gives players dialogs and clickable chat, and keeps the plain version for the console.
 */
public interface Menus {

    /** {@code /pvp} without arguments. */
    void mainMenu(Player player);

    /** {@code /pvp games [page]}; pages start at 1. */
    void games(CommandSender sender, int page);

    /** {@code /pvpadmin start} from a player without settings: the arena picker (no arena) or that arena's settings form. */
    void startGame(Player player, Optional<Arena> arena);

    /** {@code /pvp rules [mode]}: one mode's rules, or the list of modes. */
    void rules(CommandSender sender, Optional<GameMode> mode);

    /** {@code /pvp stats [player]} for a player who has stats. */
    void stats(CommandSender sender, PlayerStats stats);
}
