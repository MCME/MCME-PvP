# Death Run

| | |
|---|---|
| Mode id | `deathrun` (1.1.2 name: `DeathRun`) |
| Status | Playable in 3.0 |
| Production maps | 5, of which 4 play (`DR3-DR`, `DS-DR`, `PK1-DR`, `PK2-DR`); `DALR-FFA` lacks its points |
| Players | 2 up to `maxPlayers`, on two sides: the Runners and one Death |

One player is Death; everyone else is a Runner. The Runners race with empty hands through a course to a beacon at
the end, while Death, armed with a sword and a bow, hunts them. A Runner who dies is out, but Death respawns forever.
If at least one Runner reaches the beacon before time runs out, all the Runners win; otherwise Death wins.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two sides: the **Runners** (blue names) and **Death** (a dark gray name and black armor). Runners can't hurt each
  other.
- When the countdown starts, one random player becomes Death; everyone else is a Runner. There is one Death per game.
- Runners start at `RunnerSpawn`. Death starts and respawns at `DeathSpawn`. An arena with more than one point per
  side (`RunnerSpawn1`, `DeathSpawn1`, …) uses them in turn; every production map has one of each.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- **Runners:** nothing at all: no armor, no weapon, an empty inventory.
- **Death:** the standard kit, dyed black `#1D1D21`: leather helmet, chestplate, leggings and boots, an iron sword
  in the first hotbar slot, a bow in the second and 24 arrows in the last (ninth) slot. Death has no Speed and no
  other effect.
- Armor, sword and bow never break. No enchantments, no shield, no food.
- **Arrows.** Shot arrows can never be picked up, by anyone. Death keeps the arrows the kit gives; a respawn brings a
  fresh 24. Arrows stuck in a block disappear after 9 seconds.
- **Supply chests.** During the fight, Death gets 24 arrows in the last slot from a chest while carrying fewer than
  24, with the slowness and countdown of S9. Runners get nothing from a chest.

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7, S35.

- The countdown needs a player on each side, so 2 players. The sides are made and every player reads which side they
  are on. At the same moment the block under the arena's `VictoryPoint` becomes a beacon: the goal.
- The Runners are frozen on `RunnerSpawn`. Death is placed on `DeathSpawn` but is not frozen: Death may move about
  during the countdown (3.0 differs: Not decided yet, item 1).
- Doors, buttons and levers still work during the countdown (3.0 differs: Not decided yet, item 2).

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

- **The goal.** While the game runs, a Runner finishes by stepping onto the beacon, or by right-clicking it. A
  finish is for good: the Runner becomes a spectator right where they stand, to watch the others arrive, and chat
  reads "\<player> reached the goal in \<time>!". Death on the beacon finishes nothing, and the beacon's screen never
  opens.
- **No block use.** While the game runs, right-clicking a block does nothing for the players: no doors, trapdoors,
  fence gates, buttons or levers. Pressure plates and tripwires still work, and so do items.
- **No score.** The sidebar shows the Runners still running, Death, and how many have reached the goal.
- The course's own traps, fall damage, lava, fire, drowning and the void hurt as usual. There is no spawn
  protection.
- A Runner can still hit Death with bare hands, and Death can die like anyone else.
- **Time limit:** `timeLimit` minutes (default 10). Chat warns at 30 seconds and every second from 10 to 1.
- **Leaving under fire** (S24): a Runner who leaves this way is out, and shares the Runners' result instead of
  getting "left".

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- **A Runner dies:** they are out for good and never respawn. Chat reads "\<Runner> has died", without naming a
  killer (3.0 differs: Not decided yet, item 4). They watch from the arena's spawn point.
- **Death dies:** chat reads "\<Death> has died", and Death respawns at once at `DeathSpawn` with a fresh standard
  kit.
- The kill still goes to the player Minecraft credits, for the stats.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- **The run ends** as soon as no Runner is still running, because each one finished, died or left, or when time runs
  out.
- **The Runners win** if at least one of them reached the goal; every Runner shares the win, also those who died.
  **Otherwise Death wins.** Reaching the goal does not end the run by itself: the others keep running.
- **Death leaves:** while 2 or more Runners are still running, a random one of them becomes Death at once: they move
  to `DeathSpawn` with Death's kit, and chat reads "Death has left. \<player> is the new Death!". With fewer, the run
  goes on without Death.
- The summary shows who reached the goal, in order, with their times, or that nobody did. There are no K/D lists:
  Death has every kill.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S23, S24, S25, S34.

- While the game waits for its countdown, anyone can join as a player until it is full.
- Once the countdown has started, a newcomer can't join as a player until the fight runs (3.0 differs: Not decided
  yet, item 3).
- While the game runs, a newcomer becomes a Runner as long as at least 2 minutes are left: they start at
  `RunnerSpawn` with nothing. In the last 2 minutes they can't join as players. A party joins together.
- Anyone who already played in this run only comes back to watch: a Runner who died, finished or left, and a Death
  who left. "Played" counts from the moment the fight starts: a player who only took part in the countdown, or only
  watched, may still run.
- When the last Runner still running leaves, the run ends at once.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Runners who died watch from the arena's spawn point; Runners who finished, from where they finished.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`, for example
`/pvpadmin start PK1-DR timeLimit=15`. The defaults come from `config.yml` (`modes.deathrun.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 10 | 1 to 60 | Minutes until time runs out; then the Runners win if one of them reached the goal, otherwise Death. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: newcomers run by the rule above. Off: they watch. |

There is no `teamCount` (always two sides) and no `scoreGoal` (there is no score).

## Map requirements

- **`RunnerSpawn`** and **`DeathSpawn`**, at least one of each. Numbered extras (`RunnerSpawn1`, `DeathSpawn2`, …)
  are used in turn after the unnumbered one; numbers may skip. Names match in any case.
- **`VictoryPoint`**, one point: where a Runner stands to finish. The goal is the block under it, the block the
  point stands on; it becomes a beacon at every countdown, and the block that was there goes back when the game
  closes, or at the next start after a crash. Only this point is a goal: other points with "Point" in their name are
  ignored. Keep it inside the region, and reachable on foot.
- The beacon is placed without a check for blocks that hold data: a sign, chest, banner or head under `VictoryPoint`
  would lose its text or contents when it goes back. The production maps have a beacon there.
- **The arena spawn** (`spawn`): where waiting players, spectators and Runners who died stand.
- **A region** keeps players in the arena. Without one, the game reserves its whole world.
- **Unassigned:** an arena without one of the three is set aside at startup with "missing points: DeathSpawn,
  RunnerSpawn, VictoryPoint" (the missing ones); one without an arena spawn with "the map has no spawn point".
- **Production maps:** `DR3-DR`, `DS-DR` (Death_Swim), `PK1-DR` and `PK2-DR` each have one `RunnerSpawn`, one
  `DeathSpawn` and one `VictoryPoint`, and play. 1.1.2 set a beacon under their `VictoryPoint` at every game start
  and never put the old block back, so the block that returns after a 3.0 game is that beacon. `DALR-FFA` (title
  "Secret") is stored as Death Run but only has `PlayerSpawn` to `PlayerSpawn3`: it could not start in 1.1.2 either,
  and stays unassigned, keeping its mode, until its Death Run points are added.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).
Texts are quoted from `messages.yml` without their color tags.

- **Sidebar.** The title is the mode's name (`hud.sidebar-title`), followed by "Waiting \<count>/\<max>"
  (`hud.sidebar-waiting`), "Starting in \<seconds>s" (`hud.sidebar-countdown`) or "Time \<time>" (`hud.sidebar-time`).
  From the countdown on it lists "Runners" with the Runners still running, "Death" with 1 (0 while nobody is Death),
  and "Finished" (`hud.sidebar-finished`) with the number who reached the goal.
- **Boss bar.** "Starting in \<seconds>s" (`hud.bossbar-countdown`) during the countdown, then "\<mode> · \<time> left"
  (`hud.bossbar-time`).
- **Action bar** during the fight: "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`); "Finished ·
  spectating" (`hud.actionbar-finished`) for a Runner who reached the goal; "You're out · spectating"
  (`hud.actionbar-out`) for a Runner who died; "Spectating \<arena> · /pvp leave" (`hud.actionbar-spectating`) for
  spectators. For Death at a supply chest: "Restocking arrows… \<seconds>" (`restock.arrow.restocking`) for 3, 2 and
  1, then "Arrows restocked!" (`restock.arrow.restocked`) for 1 second.
- **Titles.** "\<seconds>" (`title.countdown`) for the last 5 seconds of the countdown, "Fight!" (`title.fight`),
  "Goal!" (`title.finished`) for a Runner who reaches the goal, "You're out" (`title.eliminated`) for a Runner who
  dies, and "Victory" (`title.victory`), "Defeat" (`title.defeat`) or "Draw" (`title.draw`) at the end. A game
  stopped by staff or by a server stop shows no result title.
- **Sounds.** None.
- **Chat,** to the game's players and spectators only:
  - "The game starts in \<seconds> seconds!" (`game.countdown-started`), then to each player "You are a Runner! Reach
    the beacon." (`side.runners.you`) or "You are Death! Stop the Runners." (`side.death.you`);
  - "Game begins in \<seconds>" (`game.countdown-tick`) from 5 to 1, then "Game Start!" (`game.started`);
  - "Countdown cancelled: not enough players." (`game.countdown-cancelled`);
  - "\<player> reached the goal in \<time>!" (`game.finished`), the time in minutes and seconds since the start, such
    as "2:31";
  - "\<victim> was killed by \<killer>" (`game.death`) for a credited kill; any other death shows Minecraft's own death
    message, to the game only;
  - "You're out! You're spectating until the game ends." (`game.eliminated`) to a Runner who died;
  - "Death has left. \<player> is the new Death!" (`side.death.replaced`), and the new Death reads "You are Death!
    Stop the Runners.";
  - "\<time> remaining!" (`game.time-warning`) at 30 seconds and from 10 to 1;
  - "\<player> left the fight and counts as killed by \<killer>." (`game.fled`) or "\<player> left the fight and counts
    as dead." (`game.fled-alone`);
  - "You joined \<arena> (\<mode>)." (`join.joined`); "\<player> joined (\<count>/\<max>)." (`game.player-joined`) while
    the game waits; "\<player> joined the fight on \<team>." (`game.player-joined-team`) for a late joiner, who also
    reads "You are a Runner! Reach the beacon."; "The game has already started." (`join.midgame-spectating`) or
    "You're out of this game, so you watch it." (`game.rejoin-eliminated`) to a joiner who can only watch;
  - "You left the game." (`leave.left`) to a leaver, and "\<player> left." (`game.player-left`) to the others.
- **End summary:** "Game over — \<mode> on \<arena>" (`summary.header`); "\<team> won!" (`summary.team-won`), as
  "Runners won!" or "Death won!", or "It's a draw." (`summary.draw`); then "Reached the goal: \<entries>"
  (`summary.finishers`), each entry "\<rank>. \<player> (\<time>)" (`summary.finisher`) in finish order, or "Nobody
  reached the goal." (`summary.no-finishers`). No K/D or deaths lists. Then [Games] and [My stats] buttons. The
  result stays for `endingSeconds` (10 seconds).
- **Rules:** `/pvp rules deathrun` shows "\<mode> rules" (`rules.header`) and `mode.deathrun.rules`: "One player is
  Death; everyone else is a Runner. Runners race to the beacon at the end of the course while Death hunts them. A
  Runner who dies is out. The Runners win if at least one of them reaches the goal before time runs out; otherwise
  Death wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Result.** The Runners share one result: won when at least one of them reached the goal, lost otherwise. It goes
  to every Runner who took part, also those who died or finished, even if they left afterwards. Death gets the
  opposite. A Runner who became Death gets Death's result. A game stopped by staff or by a server stop is a draw.
- **Left.** A player who left while still in the race, and did not come back to play, gets "left". That includes a
  Death who left.
- **Kills and deaths:** every credited kill counts a kill, nearly always Death's; every death during the fight
  counts a death, Death's own too. Reaching the goal is not a death.
- Finish times show in the end summary only; they are not stored.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S35, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Death's arrows | After every shot Death was put back to a single arrow | Death keeps the kit's 24 arrows; chests fill them up to 24 | The one-arrow reset belonged to One In The Quiver |
| Death leaves | The run went on without Death | With 2 or more Runners still running, a random one becomes Death | Keeps the hunt going |
| Who may come back | Only former Runners were turned away: a Death who left could come back as a Runner, and a Runner who left during the countdown never could | Anyone who played in the run, from the moment the fight started, only watches; the countdown alone doesn't count | A Death who left can't come back as a Runner who knows where Death waits |
| Finishing | Right-clicking a beacon | Right-clicking the goal's beacon, or stepping onto it | Running onto the goal finishes the run |
| The goal | Every point with "Point" in its name became a beacon, and right-clicking any beacon block finished | Only the block under `VictoryPoint`; no production map has another such point | Only the map's own goal counts |
| The beacon after a game | It stayed for good | The original block goes back when the game closes, and after a crash at the next start | Games put back what they change |
| Wins and losses | Runners who reached the goal were never counted as winners; Runners who died or finished counted as spectators; a run won by the last Runner reaching the goal recorded nothing | All Runners share the result, also those who died; Death gets the opposite; "left" for players who left | Every mode records results the same way |
| Supply chests | Runners got 24 arrows they could not shoot, and 3 seconds of slowness | Runners get nothing; Death gets arrows | Runners have no bow |
| Death's name | Black in chat and in the tab list | Dark gray; the armor stays black | Black text can't be read in chat |
| Side and finish texts | "You are a runner, don't stand RUN!" (garbled) and "You are Death, kill the runners!"; "\<player> has reached the goal!" | "You are a Runner! Reach the beacon." and "You are Death! Stop the Runners."; "\<player> reached the goal in 2:31!", a "Goal!" title and "Finished · spectating" on the action bar | The 1.1.2 line was garbled; the time shows how the race went |
| Sidebar | "Runners:" only, under the time | Adds "Death" and "Finished" with the number who reached the goal | Shows how the race goes |
| End summary | "Game over!" with "\<names> win!", or "Death Wins!" | The result, then who reached the goal in order with their times, or "Nobody reached the goal." | Shows the race at a glance |
| Time limit | A number of minutes had to be given when starting the game | The `timeLimit` setting, default 10 minutes | A game can start with a sensible default |
| Rules text | Ran on into the Capture the Flag, Egg Hunt and Siege rules | Its own text only, reworded | It was a bug |
| Errors | The handler for players logging out never ran and `/pvp leave` checked nothing, so the run went on after its last Runner left and the sidebar kept counting them; a late joiner showed as a blank chat line; a map missing a point failed at the start | Fixed; a map missing its points is set aside at startup | They were bugs |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S18,
S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **Death during the countdown.** 1.1.2 froze only the Runners (`Gamemode/DeathRun.java:139-151`): Death could walk
   about during the 7-second countdown, unable to hurt anyone and without a kit until the start. 3.0 now: every
   player is frozen during the countdown, Death included (`game/Game.java:247-251`,
   `platform/GameEventRouter.java:212-231`, `platform/GameEventRouter.java:261-293`). Rule above: 1.1.2's. To decide:
   keep 1.1.2's rule (change the code), and then whether an unfrozen Death may use the kit before the start, or
   accept 3.0's (change this document).
2. **Doors and buttons during the countdown.** 1.1.2 blocked block use only while the game ran
   (`Gamemode/DeathRun.java:242-245`), so during the countdown Death, and Runners within reach of their spawn, could
   open doors and press buttons and levers, and a door left open stayed open into the run. 3.0 now: blocked from the
   countdown on (`platform/GameEventRouter.java:386-390`, `mode/ModeRules.java:213-216`). Rule above: 1.1.2's. To
   decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
3. **Joining during the countdown.** 1.1.2: a player who joined after the countdown began was refused with "Can't
   join DeathRun midgame!", because the late-join check compared the time limit, still in minutes until "Game
   Start!", with 120 seconds (`Gamemode/DeathRun.java:117`, `:173`, `:341`, `maps/Map.java:89-97`); the refused
   player was still put on the game's list, without a side. 3.0 now: they become Runners and are placed and frozen
   with the others (`game/Joins.java:218-219`, `game/TeamPlacement.java:103-105`). Rule above: 1.1.2's. To decide:
   keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
4. **Death messages.** 1.1.2: a Runner's death read "\<Runner> has died" in blue, and Death's "\<Death> has died" in
   black; neither named the killer (`Gamemode/DeathRun.java:273`, `:297`). 3.0 now: "\<victim> was killed by
   \<killer>" (`game.death`) for a credited kill, otherwise Minecraft's own death message
   (`platform/TextAnnouncer.java:150-159`, `platform/GameEventRouter.java:121-135`). Rule above: 1.1.2's. To decide:
   keep 1.1.2's rule (change the code) or accept 3.0's (change this document).

## For developers

- **Mode class:** `src/main/java/com/mcmiddleearth/pvp/mode/deathrun/DeathRun.java`, with `DeathRunLogic.java`
  beside it. Id `deathrun`; legacy name `DeathRun`. Paths below are relative to
  `src/main/java/com/mcmiddleearth/pvp/`.
- **Declarations** (`mode/deathrun/DeathRun.java:32-46`):
  - layout: `TeamLayout.sides(Palette.RUNNERS, Palette.DEATH)`; the second side, `death`, is the role side that
    gets one random player at the countdown (`game/TeamPlacement.java:68-73`);
  - the sides' `TeamSpec`s (`team/Palette.java:42-44`): `runners` blue, armor `0x3C44AA`; `death` dark gray text, armor
    `0x1D1D21`;
  - spawns: `SpawnPoints.bySide(runners → "RunnerSpawn", death → "DeathSpawn")`, each read as a numbered list;
  - kit: `LoadoutPolicy.bySide(runners → Kits.NONE, death → Kits.STANDARD)`;
  - respawn: `Respawn.AT_SPAWN`, with `respawnFor(runners, Respawn.NEVER)`; no spawn protection;
  - goal: `new Goal("VictoryPoint", runners, "minecraft:beacon")` (`mode/rules/Goal.java`, the record, and
    `mode/deathrun/DeathRun.java:32-46`);
  - `noBlockUse()`, `noRestockFor(runners)` (Death keeps the default `Restock.ARROWS`), `noArrowPickup()`;
  - late join: `MidgameJoin.toSide(runners, 120, true)`: the other side during the countdown, the Runners while 120
    or more seconds are left, newcomers only;
  - win: `WinConditions.race(runners, death)` (`mode/rules/WinConditions.java:220-266`);
  - left at the defaults: sidebar `SidebarStyle.PLAYERS_LEFT` (the goal adds the "Finished" line), kill score 0.
- **Settings** derived from the declarations: `timeLimit` (from `race`), `maxPlayers`, `midgameJoin`. Defaults:
  `config.yml`, `modes.deathrun`.
- **ModeLogic hooks:** `onRegionEnter` (a step onto the goal block) and `onInteract` (a right-click on it), both
  calling `Game.finish` for a player of the goal's side (`mode/deathrun/DeathRunLogic.java:16-33`).
- **Engine and platform pieces it relies on:** `game/Lifecycle.java:154-157` (the goal marker, set at every
  countdown, with no data check), `platform/PaperWorldActions.java:72-79` and `platform/BlockJournal.java` (the first
  original of each changed block, also on disk as `restore/<game id>.json`), `platform/BlockRestore.java` (the
  replay at startup after a crash), `platform/GoalWatch.java:23-30` and `platform/GameEventRouter.java:243-252`
  (steps, at MONITOR), `platform/GameEventRouter.java:372-395` (block use and clicks), `game/Combat.java:153-164`
  (finishing), `game/Roles.java:68-83` (a new Death when Death leaves), `PlayerSession.playedWhileRunning`
  (`game/Lifecycle.java:289`), `platform/SummaryLines.java:123-140` (the finishers),
  `platform/ui/SidebarLines.java:52-53` (the "Finished" line).
- **Texts:** `mode.deathrun.name`, `mode.deathrun.rules`, `side.runners.you`, `side.death.you`,
  `side.death.replaced`, `game.finished`, `title.finished`, `hud.actionbar-finished`, `hud.sidebar-finished`,
  `summary.finishers`, `summary.finisher`, `summary.no-finishers`.
- **Tests:** `src/test/java/com/mcmiddleearth/pvp/mode/deathrun/DeathRunTest.java` (the role pick, the goal,
  finishing, winning, the finish order), `DeathRunPaperTest.java` (kits, the beacon and its restore, steps and
  clicks, block use, chests, arrows, texts, sidebar, summary), `DeathRunJoinsTest.java` (late joining, a new Death);
  `platform/GoalsAndBlockUseTest.java`, `platform/SupplyChestsPerModeTest.java`, `game/RoleReplacementTest.java`,
  `ProductionMapsTest.java` (the 4 playable maps start with the beacon under `VictoryPoint`; `DALR-FFA` waits for
  its points).
