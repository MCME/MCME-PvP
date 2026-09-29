package com.mcmiddleearth.pvp.team;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The fixed team and personal colors. Teams are used in this order: a 3-team game is red, blue and green. */
public final class Palette {

    public static final TeamSpec RED = new TeamSpec("red", "Red", NamedTextColor.RED, 0xB02E26, false);
    public static final TeamSpec BLUE = new TeamSpec("blue", "Blue", NamedTextColor.BLUE, 0x3C44AA, false);
    public static final TeamSpec GREEN = new TeamSpec("green", "Green", NamedTextColor.GREEN, 0x5E7C16, false);
    public static final TeamSpec YELLOW = new TeamSpec("yellow", "Yellow", NamedTextColor.YELLOW, 0xFED83D, false);
    public static final TeamSpec AQUA = new TeamSpec("aqua", "Aqua", NamedTextColor.AQUA, 0x3AB3DA, false);
    public static final TeamSpec GOLD = new TeamSpec("gold", "Gold", NamedTextColor.GOLD, 0xF9801D, false);

    /** Team colors, in the order games use them. */
    public static final List<TeamSpec> TEAMS = List.of(RED, BLUE, GREEN, YELLOW, AQUA, GOLD);

    /** Personal colors for modes without teams, in 1.1.2's Free For All rotation order. */
    public static final List<TeamSpec> PERSONAL = List.of(
            new TeamSpec("p-aqua", "Aqua", NamedTextColor.AQUA, 0x3AB3DA, true),
            new TeamSpec("p-blue", "Blue", NamedTextColor.BLUE, 0x3C44AA, true),
            new TeamSpec("p-dark-aqua", "Dark Aqua", NamedTextColor.DARK_AQUA, 0x169C9C, true),
            new TeamSpec("p-dark-green", "Dark Green", NamedTextColor.DARK_GREEN, 0x5E7C16, true),
            new TeamSpec("p-dark-purple", "Dark Purple", NamedTextColor.DARK_PURPLE, 0x8932B8, true),
            new TeamSpec("p-dark-red", "Dark Red", NamedTextColor.DARK_RED, 0x8E1B1B, true),
            new TeamSpec("p-gold", "Gold", NamedTextColor.GOLD, 0xF9801D, true),
            new TeamSpec("p-green", "Green", NamedTextColor.GREEN, 0x80C71F, true),
            new TeamSpec("p-light-purple", "Light Purple", NamedTextColor.LIGHT_PURPLE, 0xC74EBD, true),
            new TeamSpec("p-red", "Red", NamedTextColor.RED, 0xB02E26, true),
            new TeamSpec("p-yellow", "Yellow", NamedTextColor.YELLOW, 0xFED83D, true));

    private Palette() {
    }

    /** The first {@code count} team colors. */
    public static List<TeamSpec> teams(int count) {
        if (count < 0 || count > TEAMS.size()) {
            throw new IllegalArgumentException("team count " + count);
        }
        return TEAMS.subList(0, count);
    }

    /** A team or personal color by id. */
    public static Optional<TeamSpec> byId(String id) {
        return Stream.concat(TEAMS.stream(), PERSONAL.stream())
                .filter(spec -> spec.id().equals(id))
                .findFirst();
    }
}
