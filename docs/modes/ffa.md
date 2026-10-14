# Free For All

| | |
|---|---|
| Mode id | `ffa` (1.1.2 name: `FreeForAll`) |
| Status | Playable in 3.0 |
| Production maps | 16 (`AS-FFA`, `BE-FFA`, `LT-FFA`, `OMP-FFA`, `WE-FFA`, …) |
| Players | 2 up to `maxPlayers` (20 to 100 on the production maps); free for all, no teams |

Everyone fights everyone with a standard kit and respawns at once. Every kill is a point. When time runs out (10
minutes by default), the player with the most kills wins.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S17, S18, S19, S20.

- There are no teams. Each player has a personal color (S17), shown on the name tag, in the tab list, on the leather
  armor and in the chat prefix ("\<color>Player").
- Spawns are the arena's `PlayerSpawn` points: `PlayerSpawn`, then `PlayerSpawn1`, `PlayerSpawn2`, … At the countdown
  players are placed on them in turn, starting at `PlayerSpawn`; with more players than points the turn starts over,
  so players can share a spawn.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

The standard kit:

| Slot | Item |
|---|---|
| Head, chest, legs, feet | Leather helmet, chestplate, leggings and boots, dyed in the player's color, unbreakable |
| Hotbar 1 | Iron sword, unbreakable |
| Hotbar 2 | Bow, unbreakable |
| Hotbar 9 | 24 arrows |

- No enchantments, no food, no shield, nothing in the off-hand.
- Armor dye per color: Aqua `#3AB3DA`, Blue `#3C44AA`, Dark Aqua `#169C9C`, Dark Green `#5E7C16`, Dark Purple
  `#8932B8`, Dark Red `#B02E26` (the same red as Red), Gold `#9D9D97` (light gray), Green `#80C71F`, Light Purple
  `#C74EBD`, Red `#B02E26`, Yellow `#FED83D` (3.0 differs for Dark Red and Gold: S17).
- After a respawn no new kit is handed out: a player with fewer than 24 arrows gets 24 arrows in hotbar slot 9, and
  keeps the rest of the kit (3.0 differs: Not decided yet, item 1).
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs 2 players. The time limit starts at "Game Start!".

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S14, S15, S24.

- **Scoring.** A kill scores 1 point for the killer, when the killer is another player of this game. Dying costs
  nothing, and a death without a killer (a fall, lava) scores for nobody. The score is the player's kills.
- **Everyone can hurt everyone.** Personal colors are not teams.
- **No spawn protection.** (1.1.2 had code for 7.5 seconds of healing after a respawn, but it was never switched on.)
- **No team chat:** `/t` answers "You're not on a team."
- **Leaving under fire** (S24) gives the attacker a kill and a point.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11, S20.

- A player who dies while the fight runs respawns at once on the next `PlayerSpawn` point in turn.
- A death during the countdown or after the end doesn't count.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S26, S30, S31.

- The game lasts `timeLimit` minutes (default 10). Chat warns "30 seconds remaining!", then every second from
  "10 seconds remaining!" to "2 seconds remaining!", and "1 second remaining!".
- When time runs out, the player with the most points wins. A tie for the most points, or nobody with a point, is a
  draw.
- With a `scoreGoal` above 0 (off by default), the first player to reach it wins at once.
- The winner is picked from everyone who played, also a player who left (3.0 differs: Not decided yet, item 2).
- Players leaving never end the game (3.0 differs: S26).
- The summary lists the highest K/D, the most kills and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S17, S21, S23, S24, S25, S34.

- **Late joining** (once the countdown has started): a late joiner always plays, at once. During the fight they start
  on the next `PlayerSpawn` point in turn, with the kit.
- **Coming back.** A player who left plays again at once, with the color, kills, deaths and points they had.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`. The defaults come from
`config.yml` (`modes.ffa.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 10 | 1 to 60 | Minutes from "Game Start!" until time runs out. |
| `scoreGoal` | 0 | 0 to 100 | Points (kills) that win at once; 0 means no goal. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | Off: everyone who joins after the countdown started watches. |

## Map requirements

- **Player spawns:** important points named `PlayerSpawn`, `PlayerSpawn1`, `PlayerSpawn2`, … (any case). At least
  one is needed; numbers may skip. The plain `PlayerSpawn` comes first, then the others by number. Other points on
  the map (such as `RedBlock` on `AS-FFA`) are ignored. The production maps have 3 (`FIL-FFA`) to 27 (`LDp-FFA`,
  `OMP-FFA`) player spawns.
- On 9 of the 16 production maps (`AS-FFA`, `BE-FFA`, `BH-FFA`, `DAM-FFA`, `DOS-FFA`, `HO-FFA`, `OIE-FFA`, `RI-FFA`,
  `RV1-FFA`) a missing number or another point left a hole in 1.1.2's spawn list, and on all of them but `AS-FFA`
  the highest numbers were never used; a player sent to a hole hit an error, which on `BH-FFA` stopped a game with 2
  players from starting. 3.0 uses every `PlayerSpawn` point (S19).
- **`spawn`** (the arena's spawn point): where waiting players and spectators stand.
- **Region:** optional. Without one there is no boundary, and a game reserves the arena's whole world. With one, a
  game blocks every arena whose region overlaps it (the AS maps of every mode share one place).
- **Max players:** the map's `max`.
- An arena is **unassigned** (not playable) when its 1.1.2 map had no game mode ("the map has no game mode"), has no
  `spawn` ("the map has no spawn point"), or has no player spawn at all ("missing points: PlayerSpawn").

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).

**Sidebar** (on the game's own scoreboard):
- Title: `hud.sidebar-title` "\<mode>" in bold, followed by `hud.sidebar-waiting` "Waiting \<count>/\<max>" while the
  game waits, `hud.sidebar-countdown` "Starting in \<seconds>s" during the countdown, and `hud.sidebar-time`
  "Time \<time>" ("Time 9:59") while the fight runs.
- Lines from the countdown on: the five best players still in the game, by points (then kills, fewest deaths, name),
  each name in its color with the points as the number.

**Boss bar:** `hud.bossbar-countdown` "Starting in \<seconds>s" (yellow) during the countdown; `hud.bossbar-time`
"\<mode> · \<time> left" (green, emptying as time runs out) while the fight runs.

**Action bar** while the fight runs: `hud.actionbar-score` "Kills \<kills> · Deaths \<deaths>"; for spectators
`hud.actionbar-spectating` "Spectating \<arena> · /pvp leave". A restock shows `restock.arrow.restocking` "Restocking
arrows… \<seconds>", then `restock.arrow.restocked` "Arrows restocked!".

**Titles:** `title.countdown` "\<seconds>" for the last 5 seconds of the countdown, `title.fight` "Fight!", and at the
end `title.victory` "Victory", `title.defeat` "Defeat" or `title.draw` "Draw".

**Sounds:** none.

**Chat:**
- Joining: `join.joined` "You joined \<arena> (\<mode>)." to the joiner and `game.player-joined` "\<player> joined
  (\<count>/\<max>)." to the others, also after the start. A join that ends up watching: `join.spectating` "You're
  spectating \<arena>." with `join.midgame-spectating` "The game has already started." or `join.full-spectating` "The
  game is full."
- Countdown: `game.countdown-started` "The game starts in \<seconds> seconds!", `game.countdown-tick` "Game begins in
  \<seconds>" for 5 to 1, and `game.started` "Game Start!". A cancelled countdown: `game.countdown-cancelled`
  "Countdown cancelled: not enough players."
- Time: `game.time-warning` "\<time> remaining!" with `time.seconds` "\<seconds> seconds" or `time.second` "\<seconds>
  second", at 30 seconds and every second from 10 to 1.
- During the fight: `game.death` "\<victim> was killed by \<killer>", `game.fled` "\<player> left the fight and counts as
  killed by \<killer>." or `game.fled-alone` "\<player> left the fight and counts as dead.", `game.player-left` "\<player>
  left.", `game.boundary` "You aren't allowed to leave the map!"; to the leaver `leave.left` "You left the game."
- The end, in this order: `summary.header` "Game over — \<mode> on \<arena>"; the result, `summary.player-won` "\<player>
  won with \<score> kills!" (`summary.player-won-one` "\<player> won with \<score> kill!" for 1), `summary.draw` "It's a
  draw.", `game.stopped` "The game on \<arena> was stopped by staff.", `game.shutdown` "The game on \<arena> ended
  because the server is stopping." or `game.abandoned` "Everyone left, so the game on \<arena> ended."; then
  `summary.best-kd` "Highest K/D: \<entries>", `summary.most-kills` "Most kills: \<entries>" and `summary.most-deaths`
  "Most deaths: \<entries>", each with the top 3 as `summary.entry` "\<player> (\<value>)" separated by
  `summary.separator` ", "; then [Games] and [My stats] buttons.
- `/pvp rules ffa` (or `freeforall`): `mode.ffa.rules` "Everyone for themselves, madly killing everyone! Highest number
  of kills wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Won:** the winner. **Lost:** every other player who took part and was still playing in the game at the end.
- **Neither:** a draw (a tie, nobody with a point, a staff stop or a server shutdown), and a player who left before
  the end and didn't come back to play.
- **Kills and deaths:** leaving under fire counts as a death.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Time limit | Staff typed the minutes with quickstart every time; there was no default | The `timeLimit` setting, 1 to 60 minutes, default 10, in the Start game form or `timeLimit=` | A game can be opened from the menu |
| Score goal | None | Optional `scoreGoal`: the first to reach it wins at once; 0 (the default) is off | Shorter games when staff want them |
| A tie, or no kills | One of the tied players, picked arbitrarily, was named the winner, even with 0 kills | A draw | A fair result |
| Win and loss stats | Free For All recorded no wins or losses | The winner gets a win, the others a loss; a draw counts as neither | 1.1.2 never counted Free For All results |
| Rules text | `/pvp rules` could never show the Free For All rules | `/pvp rules ffa` and `/pvp rules freeforall` show them | 1.1.2 left the mode out of its list |
| Sidebar | Every player's kills, with "Time: 9m 59s" as its title | The five best players; the time on the sidebar title and the boss bar; your own kills and deaths on the action bar | Readable with many players |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S17, S18,
S20, S26, S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **The kit after a respawn.** 1.1.2: no new kit; a respawning player with fewer than 24 arrows got 24 arrows in
   hotbar slot 9, and nothing else was given (`Handlers/AllGameHandlers.java:82-93`,
   `Gamemode/FreeForAll.java:379-398`). The rest of the kit stayed only if the world keeps inventories on death
   (`keepInventory`, unknown for production); otherwise the dead player's kit dropped where they died and they came
   back with only the arrows. 3.0 now: every respawn clears the inventory and effects and hands out a fresh kit
   (`game/Combat.java:307-317`, `mode/rules/Respawn.java:7-8`). Rule above: 1.1.2's. To decide: keep 1.1.2's rule
   (change the code) or accept 3.0's (change this document).
2. **A winner who left.** 1.1.2: the winner was the top of the per-game kill counts, which kept players who had
   logged out, so a player who was gone could win (`Gamemode/FreeForAll.java:274-289`,
   `Gamemode/BasePluginGamemode.java:252-254`). 3.0 now: only players still in the game at the end can win; a player
   who left and came back to play counts again (`mode/rules/WinConditions.java:103-109`). Rule above: 1.1.2's. To
   decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
3. **The rules text.** 1.1.2: "Every man for himself, madly killing everyone! Highest number of kills wins." 3.0 now:
   `mode.ffa.rules` "Everyone for themselves, madly killing everyone! Highest number of kills wins." Rule above: 3.0's
   text, listed here so the wording can be checked. To decide: keep 1.1.2's wording (change `messages.yml`) or accept
   3.0's.

## For developers

- **Class:** `mode/ffa/FreeForAll.java`, registered in `PvpPlugin.java:101`. Legacy name `FreeForAll` (spaces and
  case ignored, so "Free For All" maps to it).
- **Declarations** (`mode/ffa/FreeForAll.java:18-26`):
  - layout: `noTeams()`; players get personal colors (`team/Palette.java:26-37`, `game/TeamPlacement.java:134-139`);
  - spawns: `SpawnPoints.numbered("PlayerSpawn")` (`mode/rules/SpawnPoints.java:97-145`), handed out in turn by
    `game/SpawnCursor.java`;
  - loadout: `LoadoutPolicy.fixed(Kits.STANDARD)` (`mode/loadout/Kits.java:11-18`);
  - respawn: `Respawn.AT_SPAWN` (at once, at the next spawn, with a fresh kit);
  - late joining: `MidgameJoin.JOIN`;
  - win: `WinConditions.highestAt(Setting.TIME_LIMIT).orFirstTo(Setting.SCORE_GOAL)`
    (`mode/rules/WinConditions.java:123-172`);
  - scoring: `killScore(1)`;
  - settings derived from these: `timeLimit`, `scoreGoal`, `maxPlayers`, `midgameJoin` (`mode/ModeSettings.java`);
  - defaults kept: supply chests give `Restock.ARROWS`, arrow pickup on, block use on, no spawn protection, no death
    penalty, no extras.
- **ModeLogic hooks:** none; the game uses the empty default logic (`game/Game.java:53-54`).
- **Engine pieces it relies on:** `game/TeamPlacement.java` (personal colors), `game/SpawnCursor.java` (spawns in
  turn), `game/Joins.java` (joining and leaving), `game/Combat.java` (deaths, credit, leaving under fire, respawns),
  `game/Lifecycle.java` (phases, the time limit, win checks), `game/ScoreTracker.java` (scores and rankings),
  `game/MatchRecords.java` and `stats/StatsRecorder.java` (results), `platform/GameEventRouter.java` and
  `platform/DamageRules.java` (events, damage, freeze), `platform/SupplyChests.java`, `platform/RegionGuard.java`,
  `platform/ui/HudRenderer.java` with `platform/ui/SidebarLines.java`, `platform/TextAnnouncer.java` and
  `platform/SummaryLines.java` (texts), `platform/LobbyProtection.java` (the lobby).
- **Main tests:** `mode/ffa/FreeForAllTest`, `game/MidgameJoinTest`, `game/KillCreditTest`, `game/MatchRecordsTest`,
  `mode/rules/WinConditionsTest`, `mode/rules/SpawnPointsTest`, `platform/DamageRulesTest`,
  `platform/GameEventRouterTest`, `platform/SupplyChestsTest`, `platform/ui/HudRendererTest`, `ProductionMapsTest`
  (16 `ffa` arenas).
