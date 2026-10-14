# Egg Hunt

| | |
|---|---|
| Mode id | `egghunt` (1.1.2 name: `EggHunt`) |
| Status | Playable in 3.0 (not in a release yet); each arena plays once staff have listed its eggs |
| Production maps | 7 (`BE-EH`, `CG-EH`, `DAH-EH`, `DOS-EH`, `LT-EH`, `OIE-EH`, `RI-EH`), unassigned until their eggs are listed |
| Players | 2 up to `maxPlayers` (30 to 100 on the production maps); every player for themselves |

Every player for themselves, hunting eggs: wool blocks hidden around the map, worth 1 to 5 points by color.
Right-click an egg to collect it. A kill is worth a point and every death costs one. The most points when time runs
out, or when the last egg is found, wins.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S17, S18, S19.

- No teams: everyone can hurt everyone.
- **Personal colors** (S17). Each player's name and leather armor take their color. The armor dyes are light blue
  `#3AB3DA`, blue `#3C44AA`, cyan `#169C9C`, green `#5E7C16`, purple `#8932B8`, red `#B02E26` (Dark Red), light gray
  `#9D9D97` (Gold), lime `#80C71F`, magenta `#C74EBD`, red `#B02E26` (Red) and yellow `#FED83D` (3.0 differs for Dark
  Red and Gold: S17).
- **Spawns.** The arena's numbered `PlayerSpawn` points: `PlayerSpawn`, then `PlayerSpawn1`, `PlayerSpawn2` and so on
  by number (numbers may skip). At the start players take them in turn from the first; every respawn and every late
  joiner takes the next one in turn, starting over after the last.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- **Standard kit** in the player's color: leather helmet, chestplate, leggings and boots, an iron sword in the first
  hotbar slot, a bow in the second and 24 arrows in the last. Armor, sword and bow never break. No enchantments, no
  food, no shield.
- The kit is given in full after every respawn.
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7, S35.

- The countdown needs 2 players.
- At the game's first countdown every egg on the arena's list is set: each listed block becomes its wool, so every
  game starts with all its eggs. A countdown that is cancelled leaves them for the next one.
- A game doesn't open, and its first countdown doesn't start, when a listed egg would replace a block that holds data
  (a sign, a container, a banner, a head, a spawner). The refusal names the block and where it is. Nothing changes in
  the world.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S14, S15, S24, S35.

**Eggs.**

- An egg is a block on the arena's egg list (see Map requirements). By its wool it is worth:

  | Wool | Points |
  |---|---|
  | `red_wool` | 1 |
  | `orange_wool` | 2 |
  | `green_wool` | 3 |
  | `blue_wool` | 4 |
  | `pink_wool` | 5 |

- A player in the fight collects an egg by right-clicking it: the block becomes air and the player scores its points.
  Their action bar shows what it was worth ("+3 (green egg)") with a sound, and the sidebar's "Eggs left" goes down.
- Each egg can be collected once per game. Collected eggs don't come back during the game. When the game closes every
  egg block goes back to what it was before the countdown (after a crash, at the next start).
- Only listed eggs count. Wool that isn't on the list, even in those five colors, does nothing.

**Scoring.**

- Egg points.
- +1 point for a kill, to the credited killer: the player of the game who killed them.
- −1 point for every death, whatever the cause (a kill, a fall, the void, `/kill`), but never below 0.
- **Leaving under fire** (S24) counts as a death: −1 point for the leaver (never below 0), and a kill and +1 point
  for the other player.

**Time.** The time limit counts down (10 minutes by default). Chat warns at 30 seconds left and every second from 10
seconds down to 1.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- Respawn is at once, at the next `PlayerSpawn` point in turn, with the full kit.
- **Spawn protection.** After a respawn nobody can hurt the player for 8 seconds (counted on the game's once-a-second
  beat, so 7 to 8 seconds), until they shoot, throw or hit someone. Their action bar counts it down. The void and
  `/kill` still get through.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S26, S30, S31.

- When time runs out (`timeLimit`, 10 minutes by default), the player with the most points wins.
- When the last egg is collected, the game ends at once, by the same rule.
- Either way, a tie for the most points, or nobody above 0, is a draw. Only players still in the game can win.
- There is no score goal, and players leaving never end the game (3.0 differs: S26).
- The summary lists the most points, the most deaths and the highest K/D (kills per death; the kills when there are
  no deaths).

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S17, S21, S23, S24, S34.

- Late joiners always play. A newcomer starts at 0 points, gets the kit at the next spawn and plays at once. One who
  joins during the countdown is placed and frozen with the others.
- A player who comes back keeps their color, points, kills and deaths.
- Leaving has no effect on the others.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Spectators can't collect eggs. There is no team chat in this mode.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`. The defaults come from
`config.yml` (`modes.egghunt.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 10 | 1 to 60 | Minutes until time runs out; then the most points win. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play at once. Off: they watch. |

There is no score goal.

## Map requirements

- **`spawn`**: the arena's spawn point, where waiting players and spectators stand.
- **`PlayerSpawn` points** in `points`: `PlayerSpawn`, `PlayerSpawn1`, `PlayerSpawn2`, … (names in any case, numbers
  may skip). The production maps have 11 to 26 each (`CG-EH` 11, `LT-EH` 26, the others 19).
- **Region**: needed to list the eggs, and players and spectators can't leave it.
- **The egg list** (below). An arena whose list has no egg of the five colors is unassigned: "no eggs listed: run
  /pvpadmin arena eggs \<id>". Its file keeps the mode, and it plays as soon as eggs are listed.
- Other unassigned reasons: "missing points: PlayerSpawn", "the map has no spawn point".

### The egg list

An arena's eggs are the `eggs` list of its file, `arenas/<id>.json`. No 1.1.2 map had one, so every imported Egg Hunt
arena starts with none. Each entry is a block of the arena's world and its block data:

```json
"eggs" : [
  { "x" : 1203, "y" : 41, "z" : -380, "block" : "minecraft:red_wool" },
  { "x" : 1188, "y" : 47, "z" : -366, "block" : "minecraft:pink_wool" }
]
```

- Only entries whose block is one of the five wools count; others are ignored. Two entries at one block: the first
  counts.
- The list is the truth for every game: at the countdown each listed block is set to its wool, also where builders
  have since removed it, and only listed blocks can be collected.
- `/pvpadmin arena info <id>` shows the number of entries ("Eggs: 15").
- `/pvpadmin arena import <id>` replaces the file from the 1.1.2 map and so empties the list: run the scan again
  afterwards.

### Listing the eggs: `/pvpadmin arena eggs <id>`

Permission `pvp.staff.arena`. The command scans the arena's region for the five wools and replaces the arena's egg
list with what it finds.

- **What it scans:** every block inside the region's outline and between its heights. The chunks under the region are
  loaded in the background, at most 8 asked for per tick and at most 8 loading at once, so the server doesn't stall
  (the largest production arena, `LT-EH`, covers about 675 chunks). A part of the region that was never generated has
  nothing to find. Imported 1.1.2 regions span heights 0 to 1000, so wool below height 0 is not found.
- **The result:** the eggs found, ordered from the lowest block up, are saved in the arena file, and the arena plays at
  once. The reply counts them by color, cheapest first.
- **Decoration** in those colors is listed too, as 1.1.2 counted it. To leave a block out, stop the server and delete
  its entry from the arena file; a later scan lists it again, so change the block itself if it should never count.
- **Run it again** whenever builders change an Egg Hunt arena.
- **Refused:** for an arena that doesn't exist, has no region, or whose mode isn't Egg Hunt; while a game is open on
  the arena or on one that overlaps it; while its world isn't loaded; and while another scan runs (one scan at a
  time).
- **Stopped:** when a chunk can't be read, or no chunk finishes loading for 30 seconds. Nothing is saved and the arena
  keeps the eggs it had. It keeps them too when a game opens on the arena, or on one that overlaps it, before the scan
  ends.

| Key | Text |
|---|---|
| `admin.eggs-scanning` | "Scanning \<arena> for eggs…" |
| `admin.eggs-found`, `admin.eggs-found-one` | "Found \<count> eggs in \<arena>: \<colors>. Saved." (\<colors> like "red 8, orange 3") |
| `admin.eggs-none` | "No eggs found in \<arena>; its list is now empty." |
| `admin.eggs-scan-failed` | "The scan of \<arena> stopped: \<reason>. Nothing was saved." |
| `admin.eggs-save-failed` | "The eggs of \<arena> could not be saved: \<reason>" |
| `admin.eggs-busy` | "The eggs of \<arena> are being scanned. Try again once that scan has finished." |
| `error.unknown-arena` | "There is no arena called \<arena>." |
| `error.arena-no-region` | "\<arena> has no region to scan." |
| `error.arena-no-collectibles` | "The mode of \<arena> has no eggs to list." |
| `error.arena-blocked` | "\<arena> is in use: the game on \<holder> covers the same area." |
| `error.arena-unassigned` | "\<arena> can't be played: \<reason>", here "world \<world> is not loaded" |

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).

**Eggs and points**

- Collecting: `game.egg-collected` "+\<points> (\<color> egg)" on the action bar for a second, with the color words
  `egg.red`, `egg.orange`, `egg.green`, `egg.blue` and `egg.pink` ("red" … "pink"), and the sound
  `entity.experience_orb.pickup`, to the collector.
- Sidebar: the title `hud.sidebar-title` "\<mode>" with `hud.sidebar-time` "Time \<time>" ("Egg Hunt Time 9:41"), the
  five best players still in the game with their points, and `hud.sidebar-eggs-left` "Eggs left" with the number
  left.
- Boss bar: `hud.bossbar-countdown` "Starting in \<seconds>s" during the countdown, then `hud.bossbar-time` "\<mode>
  · \<time> left".
- Time warnings: `game.time-warning` "\<time> remaining!" ("30 seconds remaining!", "10 seconds remaining!" … "1
  second remaining!").
- Action bar: `hud.actionbar-protected` "Spawn protection \<seconds>s · attacking ends it" after a respawn, otherwise
  `hud.actionbar-score` "Kills \<kills> · Deaths \<deaths>".

**Other texts**

- The countdown: `game.countdown-started` "The game starts in \<seconds> seconds!", `game.countdown-tick` "Game begins
  in \<seconds>" (5 to 1), `game.started` "Game Start!", titles `title.countdown` and `title.fight` "Fight!".
- `game.death` "\<victim> was killed by \<killer>" (other deaths keep their usual message), to the game only.
- `game.fled` "\<player> left the fight and counts as killed by \<killer>." and `game.fled-alone`.
- `join.joined` "You joined \<arena> (\<mode>)." to a joiner; `game.player-joined` "\<player> joined
  (\<count>/\<max>)." and `game.player-left` "\<player> left." to the others.
- Supply chests: `restock.arrow.restocking` "Restocking arrows… \<seconds>", then `restock.arrow.restocked` "Arrows
  restocked!". Leaving the region: `game.boundary` "You aren't allowed to leave the map!".
- A refused opening or countdown: `error.arena-data-block` "\<arena> can't be used: the game would replace \<block> at
  \<x> \<y> \<z>.".
- The end: `summary.header` "Game over — \<mode> on \<arena>", then `summary.player-won-points` "\<player> won with
  \<score> points!" (`summary.player-won-points-one` for 1 point) or `summary.draw` "It's a draw.", then
  `summary.most-points` "Most points: \<entries>", `summary.most-deaths` "Most deaths: \<entries>" and
  `summary.best-kd` "Highest K/D: \<entries>", each entry `summary.entry` "\<player> (\<value>)"; the titles
  `title.victory`, `title.defeat` or `title.draw`. A game everyone left: `game.abandoned` "Everyone left, so the game
  on \<arena> ended.". The result stays `endingSeconds` (10).

**Rules text** (`mode.egghunt.rules`): "Every player for themselves. Eggs (colored wool) are hidden around the map,
worth 1 to 5 points by color: right-click one to collect it. A kill is worth 1 point and every death costs 1. The most
points when time runs out, or when the last egg is found, wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- The winner gets a win; every other player who played to the end gets a loss. A draw counts as neither. A player who
  left the game gets neither.
- The match record lists the winner and each player's kills, deaths, points and result.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S27, S30, S31, S32, S34, S35, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Eggs from game to game | Collected eggs were gone for good: each game used up the map's eggs | Every game starts with all its eggs; the blocks go back when it closes | The maps ran out of eggs |
| What is an egg | Any wool of the five colors, anywhere, decoration included | Only the blocks on the arena's egg list, which the staff scan fills | A saved list is the same in every game |
| Dying | Cost nothing, although the rules text said it did | −1 point, never below 0 | The rules promised a penalty |
| The last egg | Nothing happened | The game ends at once | Nothing is left to hunt |
| After a respawn | Health set back to full once a second for 7.5 s | 8 s of spawn protection, ended by attacking | Healing didn't protect against a burst of damage |
| A tie | Everyone tied for the most points won, everyone at 0 included; nobody got a loss | A tie, or nobody above 0, is a draw; the only leader wins and the others get a loss | A win means beating the others |
| Collecting an egg | No feedback besides the sidebar score | The points on the action bar, a sound, and "Eggs left" on the sidebar | Players see what they found |
| The summary | No "Game over" or winner line; "Most Points", "Most Deaths" (deaths by a killer only) and "Highest KD", each naming every player tied for the top | The summary of S30: the winner or a draw, then the most points, the most deaths and the highest K/D, the top 3 each | One summary for every mode |
| Highest K/D | Points per death; "infinity" with no deaths | Kills per death; the kills when there are no deaths | It was not a K/D |
| Spawns | A random point of the map for every respawn and late join | The numbered `PlayerSpawn` points in turn, as in Free For All | The same spawns as the other modes without teams |
| The sidebar | Every player's score; the time in the title, changing only on whole minutes above one minute | The five best scores and "Eggs left"; the time on the sidebar and the boss bar | One layout for every mode |
| Joining during the countdown | Broke: the joiner was left where they stood, with a kit | Works: the joiner is placed and frozen with the others | Bug |
| The server log | Every respawn on the server was logged once an Egg Hunt had run | Not logged | Noise |
| Rules text | One line, followed by the Siege text | The text above (`mode.egghunt.rules`) | Clear and correct |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S17, S18,
S26, S33.

Nothing open.

## For developers

- **Class:** `mode/egghunt/EggHunt.java`. The mode has no `ModeLogic`: the engine runs the eggs and the scores.
- **ModeRules:**
  - layout: `noTeams()` (personal colors from `Palette.PERSONAL`);
  - spawns: `SpawnPoints.numbered("PlayerSpawn")`;
  - kit: `LoadoutPolicy.fixed(Kits.STANDARD)`;
  - respawn: `Respawn.AT_SPAWN`, `spawnProtection(8)`;
  - scores: `killScore(1)`, `deathScore(-1)` (floored at 0 in `game/Combat.java`);
  - eggs: `collectibles(new Collectibles(Map.of("minecraft:red_wool", 1, "minecraft:orange_wool", 2,
    "minecraft:green_wool", 3, "minecraft:blue_wool", 4, "minecraft:pink_wool", 5)))`;
  - late join: `MidgameJoin.JOIN`;
  - win: `highestAt(TIME_LIMIT).or(highestWhenAllCollected())`, from `WinConditions` and `Setting`;
  - settings: `timeLimit`, `maxPlayers`, `midgameJoin`;
  - extras: supply chests give the default `Restock.ARROWS`; the sidebar adds "Eggs left" for any mode with
    collectibles.
- **Engine and platform pieces:** the eggs of a game `game/CollectiblePlay.java`; the list `arena/Egg.java`,
  `Arena.eggs()`, `ArenaRegistry.saveEggs`; which entries count `mode/rules/Collectibles.java`; the "no eggs listed"
  reason `mode/MapPoints.java`; placing them `mode/PlacedBlocks.java`, `game/DataBlocks.java` and the block journal
  `platform/BlockJournal.java`; the scan `command/ArenaEggsCommand.java` and `platform/EggScanner.java`
  (`CHUNKS_PER_TICK = 8`, `STALL_TICKS = 600`) with `platform/SnapshotBlockReader.java`; the HUD
  `platform/ui/SidebarLines.java` and `platform/ui/HudRenderer.java` (`collected`); the summary order
  `platform/SummaryLines.java`; sounds `platform/GameSounds.java`.
- **Tests:** `mode/egghunt/EggHuntTest.java` (rules, eggs, scores, protection, ties, the last egg, late joins),
  `EggHuntPaperTest.java`; shared `game/CollectiblesTest.java`, `game/DeathScoreTest.java`,
  `platform/EggScannerTest.java`, `platform/EggsPaperTest.java`, `command/ArenaEggsCommandTest.java`; the production
  maps in `ProductionMapsTest`.
