package com.mcmiddleearth.pvp.text;

/**
 * The keys of messages.yml. Keys built from ids (modes, settings, phases) have helper methods;
 * {@code MsgTest} checks that every constant exists in the bundled file.
 */
public final class Msg {

    // error
    public static final String ERROR_NO_PERMISSION = "error.no-permission";
    public static final String ERROR_PLAYERS_ONLY = "error.players-only";
    public static final String ERROR_UNKNOWN_ARENA = "error.unknown-arena";
    public static final String ERROR_ARENA_UNASSIGNED = "error.arena-unassigned";
    public static final String ERROR_ARENA_BLOCKED = "error.arena-blocked";
    public static final String ERROR_ARENA_TEAMS = "error.arena-teams";
    public static final String ERROR_ARENA_TEAMS_ONE = "error.arena-teams-one";
    public static final String ERROR_ARENA_SPAWNS = "error.arena-spawns";
    public static final String ERROR_SETTING_UNKNOWN = "error.setting-unknown";
    public static final String ERROR_SETTING_INVALID = "error.setting-invalid";
    public static final String ERROR_SETTING_FORM = "error.setting-form";
    public static final String ERROR_SETTING_TWICE = "error.setting-twice";
    public static final String ERROR_SETTING_RANGE = "error.setting-range";
    public static final String ERROR_ALREADY_IN_GAME = "error.already-in-game";
    public static final String ERROR_NOT_IN_GAME = "error.not-in-game";
    public static final String ERROR_LEFT_THAT_GAME = "error.left-that-game";
    public static final String ERROR_NO_GAME_ON_ARENA = "error.no-game-on-arena";
    public static final String ERROR_NO_OPEN_GAME = "error.no-open-game";
    public static final String ERROR_CHOOSE_GAME = "error.choose-game";
    public static final String ERROR_GAME_FULL = "error.game-full";
    public static final String ERROR_GAME_ENDED = "error.game-ended";
    public static final String ERROR_COUNTDOWN_PHASE = "error.countdown-phase";
    public static final String ERROR_NOT_ENOUGH_PLAYERS = "error.not-enough-players";
    public static final String ERROR_UNKNOWN_PLAYER = "error.unknown-player";
    public static final String ERROR_UNKNOWN_MODE = "error.unknown-mode";
    public static final String ERROR_SERVER_LOCKED = "error.server-locked";
    public static final String ERROR_NO_TEAM = "error.no-team";
    public static final String ERROR_BUTTON_STALE = "error.button-stale";
    public static final String ERROR_BUTTON_FAILED = "error.button-failed";

    // join
    public static final String JOIN_JOINED = "join.joined";
    public static final String JOIN_TEAM = "join.team";
    public static final String JOIN_SPECTATING = "join.spectating";
    public static final String JOIN_MIDGAME_SPECTATING = "join.midgame-spectating";
    public static final String JOIN_FULL_SPECTATING = "join.full-spectating";

    // leave
    public static final String LEAVE_LEFT = "leave.left";
    public static final String LEAVE_KICKED = "leave.kicked";

    // lobby
    public static final String LOBBY_TELEPORTED = "lobby.teleported";

    // game
    public static final String GAME_CREATED = "game.created";
    public static final String GAME_PROXY_ANNOUNCEMENT = "game.proxy-announcement";
    public static final String GAME_PLAYER_JOINED = "game.player-joined";
    public static final String GAME_PLAYER_JOINED_TEAM = "game.player-joined-team";
    public static final String GAME_PLAYER_LEFT = "game.player-left";
    public static final String GAME_COUNTDOWN_STARTED = "game.countdown-started";
    public static final String GAME_COUNTDOWN_STARTED_ONE = "game.countdown-started-one";
    public static final String GAME_COUNTDOWN_TICK = "game.countdown-tick";
    public static final String GAME_COUNTDOWN_CANCELLED = "game.countdown-cancelled";
    public static final String GAME_STARTED = "game.started";
    public static final String GAME_TIME_WARNING = "game.time-warning";
    public static final String GAME_DEATH = "game.death";
    public static final String GAME_FLED = "game.fled";
    public static final String GAME_FLED_ALONE = "game.fled-alone";
    public static final String GAME_ELIMINATED = "game.eliminated";
    public static final String GAME_REJOIN_ELIMINATED = "game.rejoin-eliminated";
    public static final String GAME_STOPPED = "game.stopped";
    public static final String GAME_SHUTDOWN = "game.shutdown";
    public static final String GAME_ABANDONED = "game.abandoned";
    public static final String GAME_BOUNDARY = "game.boundary";
    public static final String GAME_RESTOCKING = "game.restocking";
    public static final String GAME_RESTOCKED = "game.restocked";

    // time
    public static final String TIME_SECOND = "time.second";
    public static final String TIME_SECONDS = "time.seconds";

    // summary
    public static final String SUMMARY_HEADER = "summary.header";
    public static final String SUMMARY_TEAM_WON = "summary.team-won";
    public static final String SUMMARY_PLAYER_WON = "summary.player-won";
    public static final String SUMMARY_PLAYER_WON_ONE = "summary.player-won-one";
    public static final String SUMMARY_DRAW = "summary.draw";
    public static final String SUMMARY_BEST_KD = "summary.best-kd";
    public static final String SUMMARY_MOST_KILLS = "summary.most-kills";
    public static final String SUMMARY_MOST_DEATHS = "summary.most-deaths";
    public static final String SUMMARY_ENTRY = "summary.entry";
    public static final String SUMMARY_SEPARATOR = "summary.separator";

    // title
    public static final String TITLE_COUNTDOWN = "title.countdown";
    public static final String TITLE_FIGHT = "title.fight";
    public static final String TITLE_VICTORY = "title.victory";
    public static final String TITLE_DEFEAT = "title.defeat";
    public static final String TITLE_DRAW = "title.draw";
    public static final String TITLE_ELIMINATED = "title.eliminated";

    // hud
    public static final String HUD_SIDEBAR_TITLE = "hud.sidebar-title";
    public static final String HUD_SIDEBAR_ARENA = "hud.sidebar-arena";
    public static final String HUD_SIDEBAR_WAITING = "hud.sidebar-waiting";
    public static final String HUD_SIDEBAR_COUNTDOWN = "hud.sidebar-countdown";
    public static final String HUD_SIDEBAR_TIME = "hud.sidebar-time";
    public static final String HUD_SIDEBAR_TEAM_ALIVE = "hud.sidebar-team-alive";
    public static final String HUD_SIDEBAR_TEAM_SCORE = "hud.sidebar-team-score";
    public static final String HUD_SIDEBAR_PLAYER = "hud.sidebar-player";
    public static final String HUD_BOSSBAR_COUNTDOWN = "hud.bossbar-countdown";
    public static final String HUD_BOSSBAR_TIME = "hud.bossbar-time";
    public static final String HUD_ACTIONBAR_SCORE = "hud.actionbar-score";
    public static final String HUD_ACTIONBAR_OUT = "hud.actionbar-out";
    public static final String HUD_ACTIONBAR_SPECTATING = "hud.actionbar-spectating";
    public static final String HUD_ACTIONBAR_LOCKED = "hud.actionbar-locked";

    // stats
    public static final String STATS_HEADER = "stats.header";
    public static final String STATS_KILLS = "stats.kills";
    public static final String STATS_GAMES = "stats.games";
    public static final String STATS_NONE = "stats.none";

    // rules
    public static final String RULES_HEADER = "rules.header";
    public static final String RULES_LIST_HEADER = "rules.list-header";
    public static final String RULES_LIST_ENTRY = "rules.list-entry";

    // games
    public static final String GAMES_HEADER = "games.header";
    public static final String GAMES_NONE = "games.none";
    public static final String GAMES_ENTRY = "games.entry";

    // team-chat
    public static final String TEAM_CHAT_FORMAT = "team-chat.format";

    // admin
    public static final String ADMIN_OPENED = "admin.opened";
    public static final String ADMIN_COUNTDOWN = "admin.countdown";
    public static final String ADMIN_STOPPED = "admin.stopped";
    public static final String ADMIN_KICKED = "admin.kicked";
    public static final String ADMIN_KICKED_ALL = "admin.kicked-all";
    public static final String ADMIN_KICKED_ALL_ONE = "admin.kicked-all-one";
    public static final String ADMIN_KICKED_ALL_NOTICE = "admin.kicked-all-notice";
    public static final String ADMIN_LOCKED = "admin.locked";
    public static final String ADMIN_UNLOCKED = "admin.unlocked";
    public static final String ADMIN_LOCKED_NOTICE = "admin.locked-notice";
    public static final String ADMIN_ARENA_LIST_HEADER = "admin.arena-list-header";
    public static final String ADMIN_ARENA_LIST_ENTRY = "admin.arena-list-entry";
    public static final String ADMIN_ARENA_LIST_UNASSIGNED = "admin.arena-list-unassigned";
    public static final String ADMIN_ARENA_INFO_TITLE = "admin.arena-info-title";
    public static final String ADMIN_ARENA_INFO_MODE = "admin.arena-info-mode";
    public static final String ADMIN_ARENA_INFO_SPAWNS = "admin.arena-info-spawns";
    public static final String ADMIN_ARENA_INFO_REGION = "admin.arena-info-region";
    public static final String ADMIN_ARENA_INFO_UNASSIGNED = "admin.arena-info-unassigned";
    public static final String ADMIN_ARENA_IMPORTED = "admin.arena-imported";
    public static final String ADMIN_START_USAGE = "admin.start-usage";

    // help
    public static final String HELP = "help";
    public static final String ADMIN_HELP = "admin-help";
    public static final String USAGE = "usage";

    // ui
    public static final String UI_HEADER = "ui.header";
    public static final String UI_CLOSE = "ui.close";
    public static final String UI_BACK = "ui.back";
    public static final String UI_MAIN_TITLE = "ui.main-title";
    public static final String UI_MAIN_BODY = "ui.main-body";
    public static final String UI_MAIN_BODY_EMPTY = "ui.main-body-empty";
    public static final String UI_MAIN_BODY_WATCH = "ui.main-body-watch";
    public static final String UI_MAIN_JOIN = "ui.main-join";
    public static final String UI_MAIN_SPECTATE = "ui.main-spectate";
    public static final String UI_MAIN_RULES = "ui.main-rules";
    public static final String UI_MAIN_STATS = "ui.main-stats";
    public static final String UI_MAIN_LOBBY = "ui.main-lobby";
    public static final String UI_MAIN_START = "ui.main-start";
    public static final String UI_MAIN_LEAVE = "ui.main-leave";
    public static final String UI_MAIN_SCORES = "ui.main-scores";
    public static final String UI_MAIN_KICKALL = "ui.main-kickall";
    public static final String UI_INGAME_TITLE = "ui.ingame-title";
    public static final String UI_SPECTATE_TITLE = "ui.spectate-title";
    public static final String UI_SPECTATE_ENTRY = "ui.spectate-entry";
    public static final String UI_SPECTATE_ENTRY_ONE = "ui.spectate-entry-one";
    public static final String UI_RULES_TITLE = "ui.rules-title";
    public static final String UI_STATS_TITLE = "ui.stats-title";
    public static final String UI_SCORES_TITLE = "ui.scores-title";
    public static final String UI_SCORES_WAITING = "ui.scores-waiting";
    public static final String UI_PICK_TITLE = "ui.pick-title";
    public static final String UI_PICK_MODE_BODY = "ui.pick-mode-body";
    public static final String UI_START_MODE = "ui.start-mode";
    public static final String UI_PICK_MODE_BUSY = "ui.pick-mode-busy";
    public static final String UI_PICK_ARENA_BODY = "ui.pick-arena-body";
    public static final String UI_PICK_ENTRY = "ui.pick-entry";
    public static final String UI_PICK_BLOCKED = "ui.pick-blocked";
    public static final String UI_PICK_NONE = "ui.pick-none";
    public static final String UI_FORM_TITLE = "ui.form-title";
    public static final String UI_FORM_FIXED = "ui.form-fixed";
    public static final String UI_FORM_START = "ui.form-start";
    public static final String UI_CONFIRM_STOP_TITLE = "ui.confirm-stop-title";
    public static final String UI_CONFIRM_STOP_BODY = "ui.confirm-stop-body";
    public static final String UI_CONFIRM_KICKALL_TITLE = "ui.confirm-kickall-title";
    public static final String UI_CONFIRM_KICKALL_BODY = "ui.confirm-kickall-body";
    public static final String UI_CONFIRM_YES = "ui.confirm-yes";
    public static final String UI_CONFIRM_NO = "ui.confirm-no";
    public static final String UI_CHAT_JOIN = "ui.chat-join";
    public static final String UI_CHAT_SPECTATE = "ui.chat-spectate";
    public static final String UI_CHAT_RULES = "ui.chat-rules";
    public static final String UI_CHAT_COUNTDOWN = "ui.chat-countdown";
    public static final String UI_CHAT_STOP = "ui.chat-stop";
    public static final String UI_CHAT_GAMES = "ui.chat-games";
    public static final String UI_CHAT_STATS = "ui.chat-stats";
    public static final String UI_CHAT_PREV = "ui.chat-prev";
    public static final String UI_CHAT_NEXT = "ui.chat-next";
    public static final String UI_CHAT_PAGE = "ui.chat-page";
    public static final String UI_CHAT_PAGE_NEXT = "ui.chat-page-next";
    public static final String UI_TOOLTIP_JOIN = "ui.tooltip-join";
    public static final String UI_TOOLTIP_SPECTATE = "ui.tooltip-spectate";
    public static final String UI_TOOLTIP_RULES = "ui.tooltip-rules";
    public static final String UI_TOOLTIP_COUNTDOWN = "ui.tooltip-countdown";
    public static final String UI_TOOLTIP_STOP = "ui.tooltip-stop";
    public static final String UI_TOOLTIP_GAMES = "ui.tooltip-games";
    public static final String UI_TOOLTIP_STATS = "ui.tooltip-stats";
    public static final String UI_ARRIVAL = "ui.arrival";
    public static final String UI_ARRIVAL_MORE = "ui.arrival-more";
    public static final String UI_BUTTON = "ui.button";
    public static final String UI_BUTTON_PRIMARY = "ui.button-primary";
    public static final String UI_BUTTON_DANGER = "ui.button-danger";

    private Msg() {
    }

    /** "mode.<id>.name": the display name of a mode. */
    public static String modeName(String modeId) {
        return "mode." + modeId + ".name";
    }

    /** "mode.<id>.rules": the rules text of a mode. */
    public static String modeRules(String modeId) {
        return "mode." + modeId + ".rules";
    }

    /** "setting.<id>": the label of a setting. */
    public static String setting(String settingId) {
        return "setting." + settingId;
    }

    /** "phase.<name>": the status word of a phase, e.g. phase("WAITING") -> "phase.waiting". */
    public static String phase(String phaseName) {
        return "phase." + phaseName.toLowerCase(java.util.Locale.ROOT);
    }

    /** {@code one} for a count of exactly 1, else {@code many}: picks a message key by its count. */
    public static String plural(String many, String one, int count) {
        return count == 1 ? one : many;
    }
}
