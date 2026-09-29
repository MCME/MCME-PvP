package com.mcmiddleearth.pvp.game;

import java.util.List;
import java.util.Optional;

/** A setting staff can choose when opening a game. The id is used in config.yml and in /pvpadmin start. */
public sealed interface Setting permits IntSetting, BoolSetting {

    /** Number of teams, for modes with a configurable team layout. */
    IntSetting TEAM_COUNT = new IntSetting("teamCount", 2, 6, 1);
    /** Minutes until the game ends on time. */
    IntSetting TIME_LIMIT = new IntSetting("timeLimit", 1, 60, 1);
    /** Points that win the game at once; 0 means no goal. */
    IntSetting SCORE_GOAL = new IntSetting("scoreGoal", 0, 100, 1);
    IntSetting MAX_PLAYERS = new IntSetting("maxPlayers", 2, 100, 1);
    /** Whether players can join as players after the countdown began. */
    BoolSetting MIDGAME_JOIN = new BoolSetting("midgameJoin");

    /** Every setting, in the order menus show them. */
    List<Setting> ALL = List.of(TEAM_COUNT, TIME_LIMIT, SCORE_GOAL, MAX_PLAYERS, MIDGAME_JOIN);

    String id();

    static Optional<Setting> byId(String id) {
        return ALL.stream().filter(s -> s.id().equalsIgnoreCase(id)).findFirst();
    }
}
