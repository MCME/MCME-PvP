# Team Slayer

| | |
|---|---|
| Mode id | `teamslayer` (1.1.2 name: `TeamSlayer`) |
| Status | Playable in 3.0 |
| Production maps | 38 (`A-TS`, `HD-TS`, `MT-TS`, `RI-TS`, …) |
| Players | One per team up to `maxPlayers`; 2 to 4 teams |

Two teams fight with endless respawns: nobody is ever out. Every kill scores a point for your team, and the first
team to the score goal wins. There is no time limit, so the game lasts until a team gets there.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S16, S18, S19.

- The game has `teamCount` teams (default 2, at most 4), in this order: Red, Blue, Green, Yellow. The production
  maps have spawns for Red and Blue only, so they play with 2 teams.
- Teams are made when the countdown starts, by lifetime K/D (kills per death), as S16 describes.
- Each team spawns on its own three points: `RedSpawn1` to `RedSpawn3` and `BlueSpawn1` to `BlueSpawn3`
  (`GreenSpawn…` and `YellowSpawn…` for a third and fourth team; 3.0 differs: Not decided yet, item 2). At the start
  a team's players are placed on them in turn: the first Red player on `RedSpawn1`, the second on `RedSpawn2`, the
  third on `RedSpawn3`, the fourth on `RedSpawn1` again.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

Everyone gets the standard kit in their team's color:

- leather helmet, chestplate, leggings and boots, dyed Red `#B02E26` or Blue `#3C44AA` (Green `#5E7C16`, Yellow
  `#FED83D`);
- an iron sword in the first hotbar slot, a bow in the second and 24 arrows in the last (ninth) slot.

Armor, sword and bow never break. There are no enchantments, no shield and no food. Supply chests restock 24 arrows
(S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs one player per team.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

- **Score.** Every kill scores 1 point for the killer's team. Only a kill scores. The kill goes to the player
  Minecraft credits, so a fall right after an enemy's hit counts for that enemy. A death with no credited killer
  (a fall, lava or the void on your own) scores nothing.
- **Leaving under fire** (S24) counts as a kill for the attacker, so it scores a point for their team and can win the
  game.
- There is no time limit.
- **Team chat.** `/t <message>` reaches your own team.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- Everyone respawns at once, forever.
- You come back at your team's next spawn point in turn, continuing the rotation the start began (`RedSpawn1`,
  `RedSpawn2`, `RedSpawn3`, `RedSpawn1`, …), with the full standard kit.
- There is no spawn protection.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- The first team to reach the score goal (`scoreGoal`, default 50) wins at once.
- When every player of a team has left the game during the fight, that team is out. With 2 teams the other team wins
  at once; with 3 or 4 teams the game goes on while two or more teams have players, and the last team with players
  wins.
- The summary lists the highest K/D and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S34.

- A player who played for a team earlier in the game goes back to that team.
- Anyone else joins a team that is 10 or more points behind the leader (the lowest-scoring one; ties: the fewest
  players, then color order); when no team is that far behind, the team with the fewest players (on equal teams:
  S22).
- A late joiner starts at their team's first spawn point (`RedSpawn1`, `BlueSpawn1`) with the standard kit (3.0
  differs: Not decided yet, item 1).
- Leaving costs your team nothing: its points stay, and you can come back.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`, for example
`/pvpadmin start HD-TS scoreGoal=75`. The defaults come from `config.yml` (`modes.teamslayer.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `teamCount` | 2 | 2 to 4, and at most the arena's team spawn sets (2 on every production map) | Number of teams. |
| `scoreGoal` | 50 | 1 to 100 | Team kills that win; it can't be 0, since there is no time limit. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none), and at least one per team | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play by the rule above. Off: they watch. |

## Map requirements

- **Team spawns:** `RedSpawn1`, `RedSpawn2`, … for Red and `BlueSpawn1`, `BlueSpawn2`, … for Blue, matched in any
  case. Numbers may skip. An unnumbered `RedSpawn` would be Red's only spawn. A third team needs `GreenSpawn…`, a
  fourth `YellowSpawn…`; an arena supports as many teams as it has spawn sets, counted from Red up to the first
  color without one.
- **The arena spawn** (`spawn`): where waiting players and spectators stand.
- **A region** keeps players in the arena. Without one, the game reserves its whole world.
- **Unassigned:** an arena without a spawn for Red or for Blue is set aside at startup with "missing points:
  BlueSpawn, RedSpawn" (the missing ones); one without an arena spawn with "the map has no spawn point".
- **Production maps:** all 38 have `RedSpawn1`–`RedSpawn3` and `BlueSpawn1`–`BlueSpawn3`. `MT-TS` also has
  `RedSpawn4` (see Not decided yet, item 2). `AS-TS` carries `RedBlock` and `OIE-TS` carries `SpectatorSpawn`; both
  are ignored.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).
Texts are quoted from `messages.yml` without their color tags.

- **Sidebar.** The title is the mode's name (`hud.sidebar-title`), followed by "Waiting \<count>/\<max>"
  (`hud.sidebar-waiting`) or "Starting in \<seconds>s" (`hud.sidebar-countdown`). From the countdown on, it lists
  each team's name in its color with the team's score, and "Goal" (`hud.sidebar-goal`) with the score goal. The
  sidebar sorts by number, so "Goal" sits at the top.
- **Boss bar.** "Starting in \<seconds>s" (`hud.bossbar-countdown`) during the countdown; none during the fight.
- **Action bar** during the fight: "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`). At a supply chest:
  "Restocking arrows… \<seconds>" (`restock.arrow.restocking`) for 3, 2 and 1, then "Arrows restocked!"
  (`restock.arrow.restocked`) for 1 second. Spectators: "Spectating \<arena> · /pvp leave"
  (`hud.actionbar-spectating`).
- **Titles.** "\<seconds>" (`title.countdown`) for the last 5 seconds of the countdown, "Fight!" (`title.fight`) at
  the start, and "Victory" (`title.victory`), "Defeat" (`title.defeat`) or "Draw" (`title.draw`) at the end. A
  game stopped by staff or by a server stop shows no result title.
- **Sounds.** None.
- **Chat,** to the game's players and spectators only:
  - "The game starts in \<seconds> seconds!" (`game.countdown-started`), then to each player "You're on team
    \<team>." (`join.team`);
  - "Game begins in \<seconds>" (`game.countdown-tick`) from 5 to 1, then "Game Start!" (`game.started`);
  - "Countdown cancelled: not enough players." (`game.countdown-cancelled`);
  - "\<victim> was killed by \<killer>" (`game.death`) for a credited kill; any other death shows Minecraft's own
    death message, to the game only;
  - "\<player> left the fight and counts as killed by \<killer>." (`game.fled`), or "\<player> left the fight and
    counts as dead." (`game.fled-alone`);
  - "You joined \<arena> (\<mode>)." (`join.joined`) to a joiner; "\<player> joined (\<count>/\<max>)."
    (`game.player-joined`) while the game waits; "\<player> joined the fight on \<team>."
    (`game.player-joined-team`) for a late joiner, who also reads "You're on team \<team>.";
  - "You left the game." (`leave.left`) to a leaver, and "\<player> left." (`game.player-left`) to the others.
- **End summary:** "Game over — \<mode> on \<arena>" (`summary.header`); "\<team> won!" (`summary.team-won`) or "It's
  a draw." (`summary.draw`); "Highest K/D: \<entries>" (`summary.best-kd`) and "Most deaths: \<entries>"
  (`summary.most-deaths`), the top 3 each, every entry "\<player> (\<value>)" (`summary.entry`). Then [Games] and [My
  stats] buttons. The result stays for `endingSeconds` (10 seconds), then everyone goes to the lobby.
- **Rules:** `/pvp rules teamslayer` shows "\<mode> rules" (`rules.header`) and `mode.teamslayer.rules`: "Two to
  four teams, and endless respawns. Every kill scores a point for your team. The first team to the score goal
  wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Result.** At the end, each player who took part gets their team's result: won or lost. A game stopped by staff
  or by a server stop is a draw for everyone. A player who left before the end and did not come back to play gets
  "left".
- **Kills and deaths:** every credited kill counts a kill, also one from leaving under fire; every death during the
  fight counts a death, whatever its cause.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Teams | Always 2, Red and Blue | 2 to 4 (`teamCount`, default 2); the production maps have spawns for 2 | Room for bigger games once maps get more team spawns |
| What scores | Any death of a team's player gave the other team a point, even a fall or lava | Only a credited kill, for the killer's team | One point per kill, as the rules always said |
| Respawn point | A random one of the team's three spawns | The team's spawns in turn | Spreads respawns evenly |
| Helms Deep bomb | Coded, but never worked: its item was never handed out | Removed | It never worked |
| Score goal | A number had to be given when starting the game | The `scoreGoal` setting, default 50, 1 to 100 | A game can start with a sensible default |
| Rules text | "Two teams, and infinite respawns. 1 point per kill. First team to a certain point total wins." | See What players see | Describes 2 to 4 teams |
| Wins and losses | Recorded at the moment of the win, for the players on each team then | Won, lost or draw for every player from the game's result; "left" for players who left before the end | Every mode records results the same way |
| Errors | The last player of a team using `/pvp spectate` never ended the game; a new player joining during the countdown hit an error; a player who left stayed on the game's roster; a map missing a spawn failed at the start | Fixed; a map missing its spawns is set aside at startup | They were bugs |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S16,
S18, S22, S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **A late joiner's spawn point.** 1.1.2: every late joiner started at their team's first spawn, `RedSpawn1` or
   `BlueSpawn1` (`Gamemode/TeamSlayer.java:243-280`). 3.0 now: a late joiner starts at their team's next spawn in
   the rotation (`game/Equip.java:24-33`, `game/SpawnCursor.java:28-36`). Rule above: 1.1.2's. To decide: keep
   1.1.2's rule (change the code) or accept 3.0's (change this document).
2. **Which spawn points a team uses.** 1.1.2 needed all six of `RedSpawn1`–`RedSpawn3` and `BlueSpawn1`–`BlueSpawn3`
   and used only those (`Gamemode/TeamSlayer.java:56-63`). 3.0 now: every numbered spawn of a team is used, and one
   per team is enough (`arena/LegacyMapImporter.java:228-247`, `mode/rules/SpawnPoints.java:57-95`). On `MT-TS`, Red
   rotates over 4 spawns (`RedSpawn4` too) and Blue over 3. Rule above: 1.1.2's. To decide: keep 1.1.2's rule (change
   the code) or accept 3.0's (change this document).

## For developers

- **Mode class:** `src/main/java/com/mcmiddleearth/pvp/mode/teamslayer/TeamSlayer.java`. Id `teamslayer`; legacy
  name `TeamSlayer`, matched ignoring spaces and case, so a map stored as `Team Slayer` gets this mode too.
- **Declarations** (`TeamSlayer.java:26-35`):
  - layout: `TeamLayout.configurable(2, 4)` with `.balanced()`;
  - spawns: `SpawnPoints.perTeam()`, the arena's `teamSpawns`;
  - kit: `LoadoutPolicy.fixed(Kits.STANDARD)`;
  - respawn: `Respawn.AT_SPAWN`, no spawn protection;
  - score: `killScore(1)`;
  - late join: `MidgameJoin.trailingTeam(TRAILING_GAP)` with `TRAILING_GAP = 10` (`TeamSlayer.java:24`);
  - win: `WinConditions.lastTeamStanding().orFirstTo(Setting.SCORE_GOAL)`;
  - sidebar: `SidebarStyle.TEAM_SCORES`;
  - left at the defaults: chests restock `Restock.ARROWS`, arrow pickups up to 24, block use allowed.
- **Settings** derived from the declarations: `teamCount`, `scoreGoal` (minimum 1 without a time limit,
  `ModeSettings.java:56-60`), `maxPlayers`, `midgameJoin`. Defaults: `config.yml`, `modes.teamslayer`.
- **ModeLogic hooks:** none; the mode is declarations only.
- **Engine and platform pieces it relies on:** `TeamPlacement.placeUnits` and `TeamBalancer.assign` (countdown
  teams), `TeamPlacement.trailing` (late joiners, `TeamPlacement.java:124-133`), `SpawnCursor` (one rotation per team
  for the countdown, respawns and late joins), `Combat.count` (credited kills and scores), `SupplyChests`,
  `ui/SidebarLines` (`TEAM_SCORES`), `SummaryLines`.
- **Texts:** `mode.teamslayer.name`, `mode.teamslayer.rules`, `hud.sidebar-goal`.
- **Tests:** `src/test/java/com/mcmiddleearth/pvp/mode/teamslayer/TeamSlayerTest.java` (scoring, the spawn rotation,
  winning, late joiners, the bomb gone), `TeamSlayerPaperTest.java` (kit colors, sidebar, chests),
  `ProductionMapsTest.java` (all 38 production maps play).
