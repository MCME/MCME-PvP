# Infected

| | |
|---|---|
| Mode id | `infected` (1.1.2 name: `Infected`) |
| Status | Playable in 3.0 |
| Production maps | 20 (`AS-IN`, `BE-IN`, `HD-IN`, `RI-IN`, `TT-IN`, …) |
| Players | 2 up to `maxPlayers`, on two sides: the Survivors and the Infected |

One player starts Infected and everyone else is a Survivor. A Survivor who dies turns Infected and hunts the
others; the Infected are fast, wear only a chestplate and respawn forever. The Infected win when no Survivor is left;
the Survivors win if any of them are still standing when time runs out. In the last two minutes the Infected get a
compass that points at the nearest Survivor.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two sides: the **Survivors** (blue names, blue armor) and the **Infected** (dark red names, red armor). Players on
  the same side can't hurt each other.
- When the countdown starts, one random player becomes the first Infected; everyone else is a Survivor. A party can
  end up split.
- Survivors start at `SurvivorSpawn`. The Infected start and respawn at `InfectedSpawn`. An arena with more than one
  point per side (`SurvivorSpawn1`, `InfectedSpawn1`, …) uses them in turn; every production map has one of each.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- **Survivors:** the standard kit, dyed blue `#3C44AA`: leather helmet, chestplate, leggings and boots, an iron
  sword in the first hotbar slot, a bow in the second and 24 arrows in the last (ninth) slot.
- **Infected:** a red `#B02E26` leather chestplate and no other armor, an iron sword in the first hotbar slot, a bow
  in the second, 24 arrows in the last slot and a shield in the off-hand.
- **Speed.** The Infected have Speed III (amplifier 2) for as long as they are Infected, with the effect's swirl
  particles showing. It comes back with the kit after every respawn (see Not decided yet, item 1).
- Armor, weapons and the shield never break. No enchantments, no food.
- The kit is given again after every respawn and whenever a player changes side.
- Supply chests restock 24 arrows for both sides (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs a player on each side, so 2 players. The sides are made, and every player reads which side
  they are on.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

- **No score.** The sidebar shows how many players each side has left.
- **Turning.** A Survivor who dies, however they die, turns Infected (see Death and respawn). The last Survivor does
  not turn.
- **Compass.** From 2 minutes before the end, every Infected player carries a compass in the fourth hotbar slot.
  Once a second it points at the nearest Survivor still in the fight, and the Infected's action bar says it tracks
  the Survivors. A player who turns Infected after that gets the compass at once. Survivors get none. With a time
  limit of 1 or 2 minutes there is no compass (see Not decided yet, item 3).
- **Time limit:** `timeLimit` minutes (default 10). Chat warns at 30 seconds and every second from 10 to 1.
- There is no spawn protection.
- **Leaving under fire** (S24): a Survivor who leaves this way counts as killed but leaves as a Survivor, without
  turning; when they are the last Survivor, they are out and get the Survivors' loss.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- Everyone respawns at once, except the last Survivor.
- **A Survivor dies:** chat reads "\<victim> was infected by \<killer>", or "\<victim> was infected by stupidity" when
  no player gets the kill. The kill goes to the player Minecraft credits, so a fall right after an Infected's hit
  counts for that Infected. Then the Survivor turns Infected and respawns at `InfectedSpawn` with the Infected kit,
  Speed III and, in the last 2 minutes, the compass.
- **An Infected player dies:** "\<victim> was killed by \<killer>", or Minecraft's own death message, then a respawn at
  `InfectedSpawn` with the full Infected kit.
- **The last Survivor dies:** the "was infected by" line still shows, but they don't turn. They are out, stay a
  Survivor and watch from the arena's spawn point, and the game ends at that moment.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- **The Infected win** as soon as no Survivor is left in the fight: all turned, the last one was killed, or the last
  ones left the game.
- **The Survivors win** when time runs out with at least one Survivor left.
- **The last Infected player leaves:** while 2 or more Survivors are still in the fight, a random one of them becomes
  Infected at once: they move to `InfectedSpawn` with the Infected kit, and chat reads "The Infected have all left.
  \<player> is now Infected!". The game goes on. With only one Survivor left, the Survivors win.
- The summary names the Survivors still standing (after the Survivors' win at the time limit), then lists the
  highest K/D and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S23, S24, S25, S34.

- While the game waits for its countdown, anyone can join as a player until it is full.
- Once the countdown has started, a newcomer can't join as a player until the fight runs (see Not decided yet,
  item 2).
- While the game runs, late joiners join the Infected as long as at least 2 minutes are left: they start at
  `InfectedSpawn` with the Infected kit. In the last 2 minutes they can't join as players. A party joins together.
- A Survivor who left and comes back while 2 or more minutes are left comes back Infected.
- Two kinds of player only come back to watch: the last Survivor, who is out, and an Infected player whose leave
  made a Survivor take their place. That stops players from adding an Infected by leaving and rejoining.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- The last Survivor, once out, watches like a spectator.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`, for example
`/pvpadmin start TT-IN timeLimit=5`. The defaults come from `config.yml` (`modes.infected.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 10 | 1 to 60 | Minutes until time runs out; then the Survivors win if any are left. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play by the rule above. Off: they watch. |

There is no `teamCount` (always two sides) and no `scoreGoal` (there is no score).

## Map requirements

- **`SurvivorSpawn`** and **`InfectedSpawn`**, at least one of each. Numbered extras (`SurvivorSpawn1`,
  `InfectedSpawn2`, …) are used in turn after the unnumbered one; numbers may skip. Names match in any case.
- **The arena spawn** (`spawn`): where waiting players, spectators and the last Survivor stand.
- **A region** keeps players in the arena. Without one, the game reserves its whole world.
- **Unassigned:** an arena without one of the two spawns is set aside at startup with "missing points: InfectedSpawn,
  SurvivorSpawn" (the missing ones); one without an arena spawn with "the map has no spawn point".
- **Production maps:** all 20 have exactly one `SurvivorSpawn` and one `InfectedSpawn`, and all 20 play. `AS-IN`
  also carries `RedBlock` and `BH-IN` carries `SpectatorSpawn`; both are ignored.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).
Texts are quoted from `messages.yml` without their color tags.

- **Sidebar.** The title is the mode's name (`hud.sidebar-title`), followed by "Waiting \<count>/\<max>"
  (`hud.sidebar-waiting`), "Starting in \<seconds>s" (`hud.sidebar-countdown`) or "Time \<time>" (`hud.sidebar-time`,
  such as "Time 9:41"). From the countdown on it lists "Survivors" and "Infected" in their colors, each with the
  players it has left.
- **Boss bar.** "Starting in \<seconds>s" (`hud.bossbar-countdown`) during the countdown, then "\<mode> · \<time> left"
  (`hud.bossbar-time`).
- **Action bar** during the fight: "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`); for the Infected in
  the last 2 minutes "Tracking Survivors!" (`hud.actionbar-tracking`, "Tracking \<team>!"); for the last Survivor
  once out "You're out · spectating" (`hud.actionbar-out`); for spectators "Spectating \<arena> · /pvp leave"
  (`hud.actionbar-spectating`). At a supply chest: "Restocking arrows… \<seconds>" (`restock.arrow.restocking`) for 3,
  2 and 1, then "Arrows restocked!" (`restock.arrow.restocked`) for 1 second.
- **Titles.** "\<seconds>" (`title.countdown`) for the last 5 seconds of the countdown, "Fight!" (`title.fight`),
  "You're out" (`title.eliminated`) for the last Survivor, and "Victory" (`title.victory`), "Defeat"
  (`title.defeat`) or "Draw" (`title.draw`) at the end. A game stopped by staff or by a server stop shows no result
  title.
- **Sounds.** None.
- **Chat,** to the game's players and spectators only:
  - "The game starts in \<seconds> seconds!" (`game.countdown-started`), then to each player "You are a Survivor!
    Stay alive." (`side.survivors.you`) or "You are Infected! Infect the Survivors." (`side.infected.you`);
  - "Game begins in \<seconds>" (`game.countdown-tick`) from 5 to 1, then "Game Start!" (`game.started`);
  - "Countdown cancelled: not enough players." (`game.countdown-cancelled`);
  - "\<victim> was infected by \<killer>" (`mode.infected.death-by.survivors`) or "\<victim> was infected by
    stupidity" (`mode.infected.death.survivors`) for a Survivor's death; a turned Survivor then reads "You are
    Infected! Infect the Survivors.";
  - "\<victim> was killed by \<killer>" (`game.death`) for a credited kill of an Infected player; any other death
    shows Minecraft's own death message, to the game only;
  - "You're out! You're spectating until the game ends." (`game.eliminated`) to the last Survivor;
  - "The Infected have all left. \<player> is now Infected!" (`side.infected.replaced`);
  - "\<time> remaining!" (`game.time-warning`) at 30 seconds and from 10 to 1, as "30 seconds" and "1 second"
    (`time.seconds`, `time.second`);
  - "\<player> left the fight and counts as killed by \<killer>." (`game.fled`) or "\<player> left the fight and counts
    as dead." (`game.fled-alone`);
  - "You joined \<arena> (\<mode>)." (`join.joined`); "\<player> joined (\<count>/\<max>)." (`game.player-joined`) while
    the game waits; "\<player> joined the fight on \<team>." (`game.player-joined-team`) for a late joiner, who also
    reads their side's line; "The game has already started." (`join.midgame-spectating`) or "You're out of this
    game, so you watch it." (`game.rejoin-eliminated`) to a joiner who can only watch;
  - "You left the game." (`leave.left`) to a leaver, and "\<player> left." (`game.player-left`) to the others.
- **End summary:** "Game over — \<mode> on \<arena>" (`summary.header`); "\<team> won!" (`summary.team-won`), as
  "Infected won!" or "Survivors won!", or "It's a draw." (`summary.draw`); after a Survivors' win at the time limit
  "Still standing: \<players>" (`summary.still-standing`); "Highest K/D: \<entries>" (`summary.best-kd`) and "Most
  deaths: \<entries>" (`summary.most-deaths`), the top 3 each, every entry "\<player> (\<value>)" (`summary.entry`).
  Then [Games] and [My stats] buttons. The result stays for `endingSeconds` (10 seconds).
- **Rules:** `/pvp rules infected` shows "\<mode> rules" (`rules.header`) and `mode.infected.rules`: "Everyone starts
  as a Survivor, except one player, who is Infected. The Infected are faster but have less armour, and respawn
  forever. A Survivor who dies becomes Infected. In the last 2 minutes the Infected get a compass that points at the
  nearest Survivor. The Infected win when every Survivor is infected; the Survivors win if any are left when time
  runs out."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Result.** At the end, each player who took part gets the result of the side they are on at that moment. A
  Survivor who turned gets the Infected's result. The last Survivor, who is out, gets the Survivors' loss. A game
  stopped by staff or by a server stop is a draw for everyone.
- **Left.** A player who left before the end and did not come back to play gets "left". That includes an Infected
  player whose leave made a Survivor take their place.
- **Kills and deaths:** every credited kill counts a kill; every death during the fight counts a death, whatever its
  cause, also a Survivor's death that turns them.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Compass | Every player online got one at 2:00 left; only the Infected's pointed anywhere | Only the Infected get one | The Survivors' compass never worked |
| Shield | Placed in the first hotbar slot, before the sword and bow | In the off-hand; sword and bow move up one slot | The Infected can block while holding the sword |
| The last Infected player leaves | The Survivors won at once | With 2 or more Survivors in the fight, a random one becomes Infected and the game goes on | The game goes on when its only Infected player leaves |
| Leaving and rejoining | An Infected player could leave and join again | A player whose leave made a Survivor Infected only comes back to watch | Stops adding an Infected by leaving and rejoining |
| The last Survivor's death | They turned Infected, so every player got the win | They are out, stay a Survivor and get the loss; the "was infected by" line still shows and the game ends at once | The last Survivor lost |
| Wins and losses | Recorded at the moment of the win for the players on each side then | Won, lost or draw for every player from the game's result, by their side at the end; "left" for players who left | Every mode records results the same way |
| Time limit | A number of minutes had to be given when starting the game | The `timeLimit` setting, default 10 minutes | A game can start with a sensible default |
| End summary | "Game over!" and "Infected Wins!", or "Survivors win!" with "Remaining:" and the names | The result, the Survivors still standing after a win at the time limit, the best K/D and the most deaths | The same summary as Team Deathmatch |
| Rules text | Three lines, "Infected gets a Speed effect, but has less armor" | See What players see | Mentions the compass and how each side wins |
| Errors | The last Survivor or the last Infected using `/pvp leave` or `/pvp spectate` never ended the game; a second Infected game on the same map before a restart broke at its start, because the first game had removed its sidebar; a map missing a spawn failed at the start | Fixed; a map missing its spawns is set aside at startup | They were bugs |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S18,
S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **Speed after a death.** 1.1.2: the first Infected got Speed II from joining the side and Speed III on top
   (`PVP/Team.java:124-126`, `Gamemode/Infected.java:173-175`); a late joiner got Speed II only
   (`PVP/Team.java:124-126`, `Gamemode/Infected.java:425`); a dying Survivor got both while still dead
   (`Gamemode/Infected.java:338-344`). Minecraft clears every effect when a player dies, and nothing in 1.1.2 gave
   Speed back after a respawn (`Gamemode/Infected.java:360-366`, `Handlers/AllGameHandlers.java:82-105`). So an
   Infected player was fast only until their first death, and a turned Survivor never was. The effect showed its
   swirl particles. 3.0 now: Speed III without particles, given with the Infected kit at every spawn, respawn, side
   switch and late join (`mode/loadout/Kits.java:42-48`, `platform/KitItems.java:57-63`, `game/Combat.java:308-317`).
   Rule above: Speed III after every respawn, as 1.1.2's rules text promised ("Infected gets a Speed effect"); the
   particles follow 1.1.2. To decide: confirm 3.0's Speed for every Infected player, with or without particles
   (change this document), or go back to 1.1.2's Speed until the first death (change the code).
2. **Joining during the countdown.** 1.1.2: a player who joined after the countdown began was refused with "Can't
   join Infected midgame!", because the late-join check compared the time limit, still in minutes until "Game
   Start!", with 120 seconds (`Gamemode/Infected.java:150`, `:203`, `:424`, `maps/Map.java:89-97`). The refused
   player was still put on the game's list, without a side or a kit. Had the check passed, they would have joined the
   Infected, as every 1.1.2 late joiner did. 3.0 now: they join the Survivors and are placed and frozen with the
   others (`mode/rules/MidgameJoin.java:20-29`, `game/Joins.java:218-219`, `game/TeamPlacement.java:103-105`). Rule
   above: 1.1.2's (no joining as a player during the countdown). To decide: keep 1.1.2's rule (change the code), make
   them Infected (change both), or accept 3.0's (change this document).
3. **Compass slot and short time limits.** 1.1.2 put the compass in the fourth hotbar slot
   (`Gamemode/Infected.java:91`, `:342`), and handed compasses out only when the clock passed exactly 2:00
   (`Gamemode/Infected.java:88`), so a game with a 1- or 2-minute time limit had none. 3.0 now: the third hotbar
   slot, next to the bow (`mode/infected/Infected.java:32`), and from the first second of a game of 2 minutes or less
   (`platform/CompassTracker.java:37-42`). Rule above: 1.1.2's. To decide: keep 1.1.2's rule (change the code) or
   accept 3.0's (change this document).

## For developers

- **Mode class:** `src/main/java/com/mcmiddleearth/pvp/mode/infected/Infected.java`, with
  `InfectedLogic.java` beside it. Id `infected`; legacy name `Infected`. Paths below are relative to
  `src/main/java/com/mcmiddleearth/pvp/`.
- **Declarations** (`mode/infected/Infected.java:34-42`):
  - layout: `TeamLayout.sides(Palette.SURVIVORS, Palette.INFECTED)`; the second side, `infected`, is the role side
    that gets one random player at the countdown (`game/TeamPlacement.java:68-73`);
  - the sides' `TeamSpec`s (`team/Palette.java:40-41`): `survivors` blue, armor `0x3C44AA`; `infected` dark red, armor `0xB02E26`;
  - spawns: `SpawnPoints.bySide(survivors → "SurvivorSpawn", infected → "InfectedSpawn")`, each read as a numbered
    list;
  - kit: `LoadoutPolicy.bySide(survivors → Kits.STANDARD, infected → Kits.INFECTED)`; `Kits.INFECTED` carries
    `KitEffect("speed", 2)` (`mode/loadout/Kits.java:42-48`), given by `platform/KitItems.java:51-64` with infinite
    duration and no particles;
  - respawn: `Respawn.AT_SPAWN` for both sides, no spawn protection;
  - late join: `MidgameJoin.toSide(infected, 120, false)`: the other side during the countdown, the Infected while
    120 or more seconds are left, not for newcomers only;
  - tracking: `new Tracking(infected, survivors, 120, 2)`, carried out by `platform/CompassTracker.java`;
  - win: `WinConditions.lastTeamStanding().or(WinConditions.holdOut(survivors))`;
  - left at the defaults: chests restock `Restock.ARROWS` for both sides, arrow pickups up to 24, block use allowed,
    sidebar `SidebarStyle.PLAYERS_LEFT`, kill score 0.
- **Settings** derived from the declarations: `timeLimit` (from `holdOut`), `maxPlayers`, `midgameJoin`. Defaults:
  `config.yml`, `modes.infected`.
- **ModeLogic hooks:** `onDeath` only (`mode/infected/InfectedLogic.java:16-26`): a dying Survivor switches to the
  Infected through `Game.switchSide`, unless no other Survivor is alive, then `Game.eliminate`. The switch waits
  until `playerDied` has fired (`game/Roles.java:103-137`), so chat still names the Survivor's side; after a leave
  under fire there is no switch (`game/Roles.java:128-131`).
- **Engine and platform pieces it relies on:** `game/Roles.java` (side switches, and the refill of an emptied role
  side, `replaceEmptiedRole`, 2 or more alive players needed, `REFILL_FROM`), `PlayerSession.roleTakenOver` (the
  leaver only watches), `game/Joins.java:207-241` (late joining), `platform/CompassTracker.java` and
  `platform/ui/ActionBarLines.java` (compass and tracking line), `platform/SideTexts.java` (the side texts and the
  "was infected by" lines), `platform/SummaryLines.java:146-159` (still standing).
- **Texts:** `mode.infected.name`, `mode.infected.rules`, `mode.infected.death-by.survivors`,
  `mode.infected.death.survivors`, `side.survivors.you`, `side.infected.you`, `side.infected.replaced`,
  `hud.actionbar-tracking`, `summary.still-standing`.
- **Tests:** `src/test/java/com/mcmiddleearth/pvp/mode/infected/InfectedTest.java` (the role pick, turning, the last
  Survivor, winning, results), `InfectedPaperTest.java` (kits, Speed III, the shield, compass, texts),
  `InfectedJoinsTest.java` (late joining, the refill and its leaver); `game/RoleReplacementTest.java`,
  `game/SwitchSideTest.java`, `platform/CompassTrackerTest.java`, `ProductionMapsTest.java` (all 20 production maps
  play).
