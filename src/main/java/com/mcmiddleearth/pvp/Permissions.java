package com.mcmiddleearth.pvp;

/** Permission nodes checked in code. plugin.yml declares them, plus 1.1.2's nodes as parents. */
public final class Permissions {

    /** Join, leave and spectate games; rules, stats, team chat. Default: everyone. */
    public static final String PLAY = "pvp.play";
    /** Stay on the server while PvP is locked. */
    public static final String JOIN_LOCKED = "pvp.join.locked";
    /** Open, count down and stop games; lock and unlock PvP; send everyone to the main server. */
    public static final String STAFF_RUN = "pvp.staff.run";
    /** Send one player to the main server. */
    public static final String STAFF_KICK = "pvp.staff.kick";
    /** List, inspect and re-import arenas. */
    public static final String STAFF_ARENA = "pvp.staff.arena";
    /** Everything; also shows the "PvP Staff" chat prefix. */
    public static final String ADMIN = "pvp.admin";

    private Permissions() {
    }
}
