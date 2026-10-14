# King of the Hill

| | |
|---|---|
| Mode id | `kingofthehill` (1.1.2 name: `KingOfTheHill`) |
| Status | Unused (no maps); not ported to 3.0, and 1.1.2 could never start it |
| Production maps | 0 |
| Players | in 1.1.2's code, two teams, Red and Blue, of at most 16 each (31 in all, given the dealing order) |

Red and Blue fight over a single hill. As 1.1.2 wrote it, the hill is a beacon that the teams were meant to hold, with
a target of 50 points. But 1.1.2 could never start the mode, and its hill code does nothing, so there are no rules
that players ever knew. This document records the code as written, and the questions a port must answer first.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38), and a port follows
them. Each part below names the ones that apply and describes this mode's own rules.

## Rules

1.1.2 never ran this mode (item 1), and no change to its own rules is confirmed. These rules describe 1.1.2's code as
written, as if a game had started. Where the code is odd, the rule names the item under
[Not decided yet](#not-decided-yet).

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two teams, Red and Blue. The players are dealt in the order they joined, with no K/D sort: Red when Blue has at
  least as many players, else Blue. So Red, Blue, Red, … and Red gets the odd player (item 20).
- **Team cap.** Once either team has 16 players, every further player becomes a spectator at the `SpectatorSpawn`
  point, 2 blocks above it (item 7). With this dealing order at most 31 play: 16 Red and 15 Blue.
- Each team has one spawn point, `RedSpawn` and `BlueSpawn`. Players appear 2 blocks above the stored point, one
  block higher than in most other modes, so they drop a block (item 18).

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

The mode has its own kit, the same for both teams (item 6):

| Slot | Item |
|---|---|
| armor | leather helmet, chestplate, leggings and boots, **not dyed**, each with Unbreaking 100 |
| first hotbar slot | iron sword, Unbreaking 100 |
| second hotbar slot | bow, Unbreaking 100 |
| third and fourth hotbar slots | 64 arrows each, 128 in all |

- Unbreaking 100 is an enchantment far above the normal maximum of III, given instead of making the items
  unbreakable.
- Without dye, the two teams' armor looks the same.
- The kit is handed out at the start only: not on a respawn. A player who respawns holding fewer than 24 arrows
  gets 24 in the last hotbar slot. Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

1. The `Hill` point's block becomes a beacon, with no iron under it: a beacon without a base shows no beam (item 12).
2. Players go to their team spawn.
3. The countdown counts from 10, one step every 11 ticks (0.55 seconds), not every second (item 5): "Game begins in
   10" at 2 seconds, down to "Game begins in 1" at about 7 seconds. The freeze ends at 7 seconds, and the game starts
   at 7.5 seconds.
4. At "Game Start!": the sidebar "Points" appears, each team player is put back on their spawn and gets the kit, and
   the game runs. There is no "/unstuck" hint.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

Nothing in the game can be won or scored (item 3):

- **The hill zone.** The code checks, on every move, whether a player is less than 4 blocks from the `Hill` point (in
  3D, from their feet to the corner of the beacon block). For a Red player in the zone it adds 1 to a hill counter,
  but only when that counter is already above 0; it starts at 0, and nothing raises it. For a Blue player in the zone
  it does nothing. A player leaving the zone lowers the counter only if they are on a list that nothing fills. So
  standing on the hill changes nothing.
- **A click capture** of 200 steps, copied from Team Conquest and shown as "Cap at \<n>%" with n half the value, is in
  the code but never called.
- The sidebar shows "Blue:" 0 and "Red:" 0 for the whole game.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- Respawn is immediate, at the team's spawn, 2 blocks above the point. Nobody is ever out, and there is no wait and
  no spawn protection.
- The kit isn't handed out again (see [Kit](#kit)).
- Every respawn on the whole server prints "koth" to the server console, and "This is in koth" when no King of the
  Hill game runs; each start adds another copy of these prints (item 4).

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- There is no win condition and no handling of players who log out (item 11). A game runs until staff end it or the
  plugin stops, even when a team has no players left.
- At the end the beacon and the block above it become air (item 12), every player leaves their team, and everyone goes
  to the lobby. There is no winner line and no summary.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S23, S24, S34.

- Late joining is never allowed (item 17). A player who joins the server during a game reads "Sorry, you can't join
  this game midgame" and "You can join the next game, though!"; `/pvp join` answers "Can't join KingOfTheHill
  midgame!".
- A player who logs out leaves their team; the game goes on.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- In 1.1.2 the spectators are the players over the team cap, players who join the server during the game, and
  players who used `/pvp spectate`. Online players who didn't join the game are left alone (item 8).

## Settings

None. 1.1.2 asked for no number when staff queued the mode: its parameter question returns nothing, which is what
makes queueing it fail (item 1). The code holds an unused target of 50 points (`:54`). A port's settings are open
(item 15).

## Map requirements

| Point | Meaning | How many |
|---|---|---|
| `RedSpawn`, `BlueSpawn` | the team's spawn, for the start, "Game Start!" and every respawn | one each, required |
| `Hill` | the hill: its block becomes the beacon, and the zone is measured from it | one, required |
| `SpectatorSpawn` | where players over the team cap watch | used, but not required (item 7) |
| the map's spawn (map field) | spectators who join the server during a game | one |

- A map also needs its region: players can't leave it.
- A map that lacks a required point: the players read "Game Cannot Start!" with a rude line blaming the map maker in green, and the end
  of the game then throws (item 10).
- **No map has ever used the mode.** 1.1.2's map editor can't set it (item 2), so only a map file edited by hand to
  `"gmType": "KingOfTheHill"` would load it. No production map does.
- **In 3.0 now.** 3.0 has no `kingofthehill` mode, so such a map would import as an unassigned arena with
  `no 3.0 mode for "KingOfTheHill" yet`.

## What players see

The mode has no keys in `messages.yml` yet. These are 1.1.2's texts, as written. A port words them as `messages.yml`
keys, at least `mode.kingofthehill.name` and `mode.kingofthehill.rules`, which every mode needs. The shared texts and
who reads them follow S2, S15 and S33 of the [README](README.md#rules-every-mode-shares).

| When | Text | Color | Who sees it in 1.1.2 |
|---|---|---|---|
| put on a team | "You are on the Red Team!" / "You are on the Blue Team!" | team color | the player |
| over the team cap | "You are Spectating!", "As a spectator, game participants won't see your chat." | gray | the player |
| countdown | "Game begins in 10" … "Game begins in 1", then "Game Start!" | green | everyone online |
| a kill | "\<victim> was killed by \<killer>" | names in team color, the rest gray | everyone on the server |
| a late join | "Can't join KingOfTheHill midgame!" | yellow | the player |
| missing points | "Game Cannot Start!" with a rude line blaming the map maker | green | the players |
| leaving the region | "You aren't allowed to leave the map!" | red, at most once every 3 seconds | the player |

- **Sidebar**, from "Game Start!": the title "Points" and the lines "Blue:" (blue) and "Red:" (red), both 0.
- **In the world:** a beacon at `Hill`, without a beam.
- **Rules text:** none. `/pvp rules` doesn't accept `kingofthehill` (item 2).
- No end lines, action bar (except a supply chest's restock countdown), boss bar, title or sound. The unused capture
  code holds "Cap at \<n>%" and "Point Captured!".

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32. A port records results as S32 describes. 1.1.2's code
records:

- **Games played:** +1 for every player at the start.
- **Kills and deaths** count as in every mode: a death for a player of the game, a kill for their killer.
- **Won, lost and spectated:** never recorded: a game has no result.

## Changes from 1.1.2

The confirmed changes every mode shares apply to a port: see the [README](README.md#rules-every-mode-shares). The
mode's own rules have no confirmed change: the mode isn't ported.

## Not decided yet

Shared items not decided yet that apply to a port: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13,
S18, S33.

Every item below is open. Items 1 to 12 are 1.1.2 bugs and oddities, each with a proposed fix. Items 13 to 19 are
design questions: since the mode never ran, its rules are still to be designed. Item 20 is where 3.0's engine works
differently from this mode's 1.1.2 code. 1.1.2 line numbers are in `Gamemode/KingOfTheHill.java` unless another 1.1.2
file is named; 3.0 paths are under `src/main/java/com/mcmiddleearth/pvp/`.

### 1.1.2 bugs and oddities

1. **It can't be started.** 1.1.2: the mode answers the parameter question with nothing (`:210-212`), and every way
   of queueing a game asks it first (`command/PVPCommand.java:470`, `:491`, `:639`, `:657`), which throws; a game
   that isn't queued can't be started. Proposed fix: nothing to port: a 3.0 mode declares its settings, and every
   mode opens the same way.
2. **Missing from the map editor and the rules.** 1.1.2: `/mapeditor <map> gm` lists 12 modes without
   `KingOfTheHill` (`command/PVPCommand.java:177`), and `/pvp rules` doesn't accept it either (`:123`). Proposed
   fix: a port registers the mode, so 3.0's menus, `/pvp rules` and a future arena editor list it, and adds its
   rules text.
3. **The hill does nothing.** 1.1.2: the zone check only raises a counter that is already above 0 and lowers it for
   players on a list nothing fills (`:229-248`); the click capture is never called (`:264-296`). Proposed fix: design
   the hill (items 14 and 15).
4. **A new listener on every start, and console prints.** 1.1.2: each start registers another listener and never
   removes it (`:85-90`); its respawn handler prints "koth" or "This is in koth" on every respawn on the server
   (`:252`, `:260`). Proposed fix: nothing to port: 3.0 modes are hooks that live for one game.
5. **The countdown.** 1.1.2: from 10, every 11 ticks (`:75`, `:186`), so it ends half a second after the freeze.
   Proposed fix: 3.0's countdown, as for every mode (S5, `countdownSeconds`, 7 by default,
   `config/PvpConfig.java:39`).
6. **The kit.** 1.1.2: undyed armor, so the teams look alike, and Unbreaking 100 instead of unbreakable items
   (`:126-175`); in vanilla Minecraft, Unbreaking spares armor only part of its wear, so this armor would still wear
   out. 128 arrows, where other modes give 24; no new kit after a respawn, where 3.0's immediate respawn hands out a
   fresh one (`game/Combat.java:308-317`). Proposed fix: the standard kit dyed per team
   (`mode/loadout/Kits.java:11`), or a kit of its own with dyed, unbreakable items; the arrows and the kit after a
   respawn are design choices.
7. **`SpectatorSpawn` is used but not required.** 1.1.2: the 32nd player is sent to it (`:104`), which throws on a
   map without it. Proposed fix: players who can't play watch from the arena's spawn, as in every 3.0 mode.
8. **Non-players aren't made spectators.** 1.1.2: the start touches only the players who joined (`:91-106`), unlike
   every other mode. Proposed fix: nothing to port: in 3.0 only players who join or spectate a game are in it (S2).
9. **No state before the first start.** 1.1.2: the mode has no constructor, so its state is empty until a start, and
   a join to a queued game would be treated as a late join (`maps/Map.java:90-97`). Proposed fix: nothing to port.
10. **Missing points: the end throws.** 1.1.2: after "Game Cannot Start!" with a rude line blaming the map maker, the end uses the
    listener, which doesn't exist yet (`:78-84`, `:193`). Proposed fix: nothing to port: 3.0 keeps an arena that
    lacks its points unassigned (`mode/ModeRules.java:277-279`).
11. **No end and no logout handling.** 1.1.2: there is no win condition and no quit handler; a game runs until staff
    end it, even with an empty team. Proposed fix: a team with no players left loses
    (`WinConditions.lastTeamStanding()`, `mode/rules/WinConditions.java:33`), plus the scoring of item 15.
12. **The beacon.** 1.1.2: a lone beacon, without a base, shows no beam (`:86-88`), and the end leaves the `Hill`
    block and the one above it as air for good (`:193-196`). Proposed fix: a beacon base like Team Conquest's and
    Siege's, and every changed block put back when the game closes (S35, `platform/BlockJournal.java:46`).

### Design questions

13. **Port it at all?** No production map uses the mode, and 1.1.2 never ran it. To decide: port it, or leave it out
    until a map needs it.
14. **How the hill is held.** The dead code points to a zone within 4 blocks of `Hill` (`:234`) and a capture of 200
    steps (`:264-296`), counting up for Red and down for Blue. To decide: capture by standing in the zone (like Siege)
    or by clicking (like Team Conquest), how fast, what a contested hill does, and whether the hill must be captured
    before it scores.
15. **How it scores and is won.** The unused target is 50 (`:54`). To decide: points for every second the hill is
    held, the goal (`scoreGoal`, default 50?), a time limit, what a tie at the time limit does, and which lists the
    end summary shows (S30).
16. **The team cap of 16.** 1.1.2 caps each team at 16 (`:92`). 3.0 has a `maxPlayers` setting per game but no cap
    per team. To decide: drop the cap (1.1.2's 31 becomes `maxPlayers`) or keep 16 per team.
17. **Late joining.** 1.1.2 refuses it (`:66`, `:206-208`). To decide: keep refusing (`MidgameJoin.SPECTATE`,
    `mode/rules/MidgameJoin.java:18`) or let late joiners play (`MidgameJoin.JOIN`, `:11`).
18. **The spawn height.** 1.1.2 puts players 2 blocks above the stored point (`:95`, `:99`, `:128`, `:153`, `:255`,
    `:257`), one block higher than most other modes. To decide: stand on the point, as every 3.0 mode does.
19. **The point names.** To decide: keep `Hill`, `RedSpawn` and `BlueSpawn`, and drop `SpectatorSpawn` (item 7).

### Where 3.0's engine differs

20. **Team placement.** 1.1.2's code: dealt in join order, Red first. 3.0's team modes balance by size and lifetime
    K/D (`mode/rules/TeamBalancer.java:24-47`), or deal round-robin in join order without `balanced()` (`:50-53`). To
    decide: which.

## For developers

**1.1.2's code.** `Gamemode/KingOfTheHill.java` (321 lines) in 1.1.2's `com.mcmiddleearth.mcme.pvp` package, which
3.0 removed. `Start` `:72-187` (the points check `:78-84`, the listener `:85-90`, the teams and the cap `:91-106`, the
countdown and the kit `:108-186`); the end `:189-204`; no late joins `:206-208`; the parameter question `:210-212`;
the listener `Gamepvp` `:214-297` (the hill `:224-227`, the zone check `:229-248`, respawns `:250-262`, the unused
click capture `:264-296`).

**3.0 pieces a port can reuse** (checked in the code):

| Piece | Where | Use here |
|---|---|---|
| two fixed teams, Red and Blue | `team/TeamLayout.java:14`, `:61-68` | the teams |
| per-team spawns | `SpawnPoints.perTeam()`, `mode/rules/SpawnPoints.java:25`, `:57-95` | `RedSpawn`, `BlueSpawn` |
| kits | `mode/loadout/Kits.java:11`; `KitItem` (`mode/loadout/KitItem.java:12`) has `unbreakable` and `dyed`, but no enchantments | the kit (item 6) |
| immediate respawn | `Respawn.AT_SPAWN`, `mode/rules/Respawn.java:8` | respawns |
| late joining | `MidgameJoin.SPECTATE` `mode/rules/MidgameJoin.java:18`, `JOIN` `:11` | item 17 |
| win conditions | `lastTeamStanding()` `mode/rules/WinConditions.java:33`, `firstTo(SCORE_GOAL)` `:50`, `highestAt(TIME_LIMIT)` `:41`, `withOvertime(…)` `:88` | an empty team, a goal, a time limit, a tie |
| team scores | `ScoreTracker.addTeamScore` `game/ScoreTracker.java:32`; `SidebarStyle.TEAM_SCORES` `mode/rules/SidebarStyle.java:8` | points for holding the hill, on the sidebar |
| hooks | `ModeLogic.onStart` `mode/ModeLogic.java:18`, `tick` `:84`, `onInteract` `:51` | the beacon, the once-a-second hold check, or clicks |
| structures and blocks changed during play | `mode/rules/Structure.java:12`; `Game.setBlock` `game/Game.java:300`; `platform/BlockJournal.java:46` | the beacon and its base, put back at the close |
| notices | `game/Notice.java:9`, `Game.notify` `game/Game.java:447` | capture feedback |

**What would be new:**
- **A zone held over time:** who of each team is within the hill's radius once a second, and the points or capture
  progress that follow. 3.0 watches no zones: `ModeLogic.onRegionEnter` fires only for a step onto the goal's block
  (`mode/ModeLogic.java:58`, `platform/GoalWatch.java:23-30`). Siege's capture zones would need the same piece.
- **A cap per team**, if item 16 keeps it.
- The texts: `mode.kingofthehill.name`, `mode.kingofthehill.rules` and the mode's lines; `legacyNames()` returning
  `KingOfTheHill`; a `modes.kingofthehill` section in `config.yml` and `PvpConfig.defaults()`.

**Tests.** None, and no production map to test on: `ProductionMapsTest` has no King of the Hill map. A port needs a
test arena of its own, with `RedSpawn`, `BlueSpawn` and `Hill`.
