# Team Deathmatch

| | |
|---|---|
| Mode id | `tdm` (1.1.2 name: `TeamDeathmatch`) |
| Status | Playable in 3.0 |
| Production maps | 35 (`AS-TDM`, `BE-TDM`, `HO-TDM`, `OMP-TDM`, `WE-TDM`, …) |
| Players | One per team up to `maxPlayers` (20 to 100 on the production maps); 2 to 4 teams |

Two or more teams fight with a standard kit, and nobody respawns. When you die you are out and watch the rest of the
game as a spectator. The last team with players left wins. There is no time limit and no score.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S16, S18, S19.

- A game has 2 to 4 teams, set by staff with `teamCount` (default 2). The teams are Red, Blue, Green and Yellow, in
  that order: a 3-team game is Red, Blue and Green. An arena holds as many teams as it has team spawns for; every
  imported 1.1.2 map has spawns for Red and Blue only, so they play with 2 teams.
- Teams are made when the countdown starts, by lifetime K/D (kills per death, or the kills when there are no deaths),
  as S16 describes.
- Every player of a team starts on that team's spawn point (`RedSpawn`, `BlueSpawn`, …), all on the same spot.
- Each player is told their team when the countdown starts.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

The standard kit, the same for every team except the color:

| Slot | Item |
|---|---|
| Head, chest, legs, feet | Leather helmet, chestplate, leggings and boots, dyed in the team color, unbreakable |
| Hotbar 1 | Iron sword, unbreakable |
| Hotbar 2 | Bow, unbreakable |
| Hotbar 9 | 24 arrows |

- No enchantments, no food, no shield, nothing in the off-hand.
- Armor dye: Red `#B02E26`, Blue `#3C44AA`, Green `#5E7C16`, Yellow `#FED83D`.
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs one player per team.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

- **Team chat.** `/t <message>` reaches the players of your own team who are still in the fight.
- **Leaving under fire** (S24) counts as a death, so the player who left is out.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- There is no respawn. A player who dies while the fight runs is out: they respawn at once at the arena's spawn point
  in Spectator mode, read "You're out! You're spectating until the game ends." and see the title "You're out".
- A kill counts for the killer only when the killer is another player of this game, and not a teammate.
- A death during the countdown or after the end doesn't count.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- The last team with players left wins, at once: after a death, or when a player leaves or logs out.
- There is no time limit and no score goal.
- The summary lists the highest K/D and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S25, S34.

- **Late joining** (once the countdown has started): a player joins the team with the fewest players left, as long as
  no team has fewer players left than it started with. After that, late joiners only watch ("The game has already
  started."). During the countdown a late joiner is always placed on a team.
- Each team's starting size is the number of players it has when the countdown begins (3.0 differs: Not decided yet,
  item 1).
- A late joiner during the fight starts on their team's spawn with the kit, at once.
- **Coming back.** A player who left and was not out plays again by the late-joining rule. One who was out (dead, or
  left under fire) only watches (S25).
- Their team may lose when a player leaves.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Players who are out watch like spectators, from the arena's spawn point.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`. The defaults come from
`config.yml` (`modes.tdm.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `teamCount` | 2 | 2 to 4, and at most the arena's team spawn sets (2 on imported maps) | Number of teams. With one possible value the form shows it as text. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none), and at least one per team | Most players in the game; spectators don't count, players who are out do. |
| `midgameJoin` | on | on or off | Off: everyone who joins after the countdown started watches. |

## Map requirements

- **Team spawns:** `RedSpawn` and `BlueSpawn` (important points, any case), plus `GreenSpawn` for 3 teams and
  `YellowSpawn` for 4. In the arena file they are the `teamSpawns` lists `red`, `blue`, `green` and `yellow`. An arena
  holds teams from Red up to the first color without a spawn.
- Numbered team spawns (`RedSpawn1`, `RedSpawn2`, …) are used in turn, but only on a map without the plain
  `RedSpawn`; when it has the plain one, that is the only spawn of the team, as in 1.1.2.
- **`spawn`** (the arena's spawn point): where waiting players, spectators and players who are out stand.
- **Region:** optional. Without one there is no boundary, and a game reserves the arena's whole world (`PS-TDM` has
  none). With one, a game blocks every arena whose region overlaps it.
- **Max players:** the map's `max`.
- An arena is **unassigned** (not playable) when its 1.1.2 map had no game mode ("the map has no game mode"), has no
  `spawn` ("the map has no spawn point"), or lacks a team spawn of a 2-team game ("missing points: BlueSpawn,
  RedSpawn", naming only the missing ones). Asking for more teams than the arena has spawns for is refused: "\<arena>
  supports up to \<max> teams."

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).

**Sidebar** (on the game's own scoreboard):
- Title: `hud.sidebar-title` "\<mode>" in bold, followed by `hud.sidebar-waiting` "Waiting \<count>/\<max>" while the
  game waits, or `hud.sidebar-countdown` "Starting in \<seconds>s" during the countdown; nothing after it while it runs.
- Lines from the countdown on: each team's name in its color, with the number of players it has left.

**Boss bar:** `hud.bossbar-countdown` "Starting in \<seconds>s" (yellow) during the countdown; none while the fight
runs.

**Action bar** while the fight runs: `hud.actionbar-score` "Kills \<kills> · Deaths \<deaths>"; for a player who is out
`hud.actionbar-out` "You're out · spectating"; for spectators `hud.actionbar-spectating` "Spectating \<arena> · /pvp
leave". A restock shows `restock.arrow.restocking` "Restocking arrows… \<seconds>", then `restock.arrow.restocked`
"Arrows restocked!".

**Titles:** `title.countdown` "\<seconds>" for the last 5 seconds of the countdown, `title.fight` "Fight!",
`title.eliminated` "You're out", and at the end `title.victory` "Victory", `title.defeat` "Defeat" or `title.draw`
"Draw".

**Sounds:** none.

**Chat:**
- Joining: `join.joined` "You joined \<arena> (\<mode>)." to the joiner; `game.player-joined` "\<player> joined
  (\<count>/\<max>)." to the others while the game waits; `game.player-joined-team` "\<player> joined the fight on
  \<team>." after the countdown started. A join that ends up watching: `join.spectating` "You're spectating \<arena>."
  with `join.midgame-spectating` "The game has already started.", `join.full-spectating` "The game is full." or
  `game.rejoin-eliminated` "You're out of this game, so you watch it."
- Countdown: `game.countdown-started` "The game starts in \<seconds> seconds!", then `join.team` "You're on team
  \<team>." to each player, `game.countdown-tick` "Game begins in \<seconds>" for 5 to 1, and `game.started` "Game
  Start!". A cancelled countdown: `game.countdown-cancelled` "Countdown cancelled: not enough players."
- During the fight: `game.death` "\<victim> was killed by \<killer>", `game.eliminated` "You're out! You're spectating
  until the game ends.", `game.fled` "\<player> left the fight and counts as killed by \<killer>." or `game.fled-alone`
  "\<player> left the fight and counts as dead.", `game.player-left` "\<player> left.", `game.boundary` "You aren't
  allowed to leave the map!"; to the leaver `leave.left` "You left the game."
- Team chat: `team-chat.format` "[TEAM] \<player>: \<message>" in the team color; without a team `error.no-team`
  "You're not on a team."
- The end, in this order: `summary.header` "Game over — \<mode> on \<arena>"; the result, `summary.team-won` "\<team>
  won!", `summary.draw` "It's a draw.", `game.stopped` "The game on \<arena> was stopped by staff.", `game.shutdown`
  "The game on \<arena> ended because the server is stopping." or `game.abandoned` "Everyone left, so the game on
  \<arena> ended."; then `summary.best-kd` "Highest K/D: \<entries>" and `summary.most-deaths` "Most deaths: \<entries>",
  each with the top 3 as `summary.entry` "\<player> (\<value>)" separated by `summary.separator` ", "; then [Games] and
  [My stats] buttons.
- `/pvp rules tdm`: `mode.tdm.rules` "Two to four teams, and no respawns. The last team with players left wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Won / lost:** every member of the winning team gets a win and every member of another team a loss, including the
  players who were out, also when they left or logged out after being knocked out.
- **Neither:** a draw (also a staff stop or a server shutdown), and a player who left the game while still alive.
- **Kills and deaths:** leaving under fire counts as a death.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Number of teams | Always Red and Blue | 2 to 4 (`teamCount`): Red, Blue, Green, Yellow | Bigger games can use more teams |
| Win and loss stats | Only the winners still alive got a win; nobody got a loss; the dead got "spectated" | Every member of the winning team wins and every member of the losing team loses, out or not, also after leaving | 1.1.2 never counted losses |
| Leaving ends the game | `/pvp spectate` (and the in-game part of `/pvp leave`) never checked for a winner, so the game could hang | Every way out is checked, and the last team standing wins at once | 1.1.2 could hang until staff ended it |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S16,
S18, S22, S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **Starting team sizes.** 1.1.2: each team's starting size was taken when the countdown began
   (`Gamemode/TeamDeathmatch.java:110-111`), so a player who left during the countdown closed late joining for the
   whole game. 3.0 now: the starting sizes are taken again when the fight starts (`game/Lifecycle.java:284-288`), and
   during the countdown a late joiner is always placed (`game/Joins.java:215-216`). Rule above: 1.1.2's. To decide:
   keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
2. **The rules text.** 1.1.2: "Two teams, and no respawns. First team to run out of players loses." 3.0 now:
   `mode.tdm.rules` "Two to four teams, and no respawns. The last team with players left wins." Rule above: 3.0's
   text, listed here so the wording can be checked. To decide: keep 1.1.2's wording (change `messages.yml`) or accept
   3.0's.

## For developers

- **Class:** `mode/tdm/TeamDeathmatch.java`, registered in `PvpPlugin.java:100`. Legacy name `TeamDeathmatch`
  (spaces and case ignored, so "Team Deathmatch" maps to it).
- **Declarations** (`mode/tdm/TeamDeathmatch.java:18-25`):
  - layout: `TeamLayout.configurable(2, 4)` with `balanced()` (`TEAM_COUNT` setting);
  - spawns: `SpawnPoints.perTeam()` (the arena's `teamSpawns`);
  - loadout: `LoadoutPolicy.fixed(Kits.STANDARD)` (`mode/loadout/Kits.java:11-18`);
  - respawn: `Respawn.NEVER` (the dead are eliminated and spectate);
  - late joining: `MidgameJoin.SMALLEST_TEAM_WHILE_FULL` (`game/Joins.java:207-222`);
  - win: `WinConditions.lastTeamStanding()` (`mode/rules/WinConditions.java:111-121`);
  - settings derived from these: `teamCount`, `maxPlayers`, `midgameJoin` (`mode/ModeSettings.java`);
  - defaults kept: supply chests give `Restock.ARROWS`, arrow pickup on, block use on, no spawn protection, sidebar
    `PLAYERS_LEFT`, no extras.
- **ModeLogic hooks:** none; the game uses the empty default logic (`game/Game.java:53-54`).
- **Engine pieces it relies on:** `game/TeamPlacement.java` and `mode/rules/TeamBalancer.java` (teams),
  `game/SpawnCursor.java` (spawns in turn), `game/Joins.java` (joining and leaving), `game/Combat.java` (deaths,
  credit, leaving under fire, respawns), `game/Lifecycle.java` (phases and win checks),
  `game/MatchRecords.java` and `stats/StatsRecorder.java` (results), `platform/GameEventRouter.java` and
  `platform/DamageRules.java` (events, damage, freeze), `platform/SupplyChests.java`, `platform/RegionGuard.java`,
  `platform/ui/HudRenderer.java` with `platform/ui/SidebarLines.java`, `platform/TextAnnouncer.java` and
  `platform/SummaryLines.java` (texts), `platform/LobbyProtection.java` (the lobby).
- **Main tests:** `mode/tdm/TeamDeathmatchTest`, `TdmRoundTest` (a full round on MockBukkit), `game/BalancingTest`,
  `mode/rules/TeamBalancerTest`, `game/MidgameJoinTest`, `game/KillCreditTest`, `game/MatchRecordsTest`,
  `mode/rules/WinConditionsTest`, `platform/DamageRulesTest`, `platform/GameEventRouterTest`,
  `platform/SupplyChestsTest`, `ProductionMapsTest` (35 `tdm` arenas).
