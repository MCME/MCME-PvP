# Team Conquest

| | |
|---|---|
| Mode id | `teamconquest` (1.1.2 name: `TeamConquest`; two production maps spell it `Team Conquest`) |
| Status | Not ported to 3.0 yet |
| Production maps | 9: `COL-TC`, `DALR-TC`, `HD-TC`, `HDold-TC`, `MO-TC`, `OIE-TC`, `SR-TC` (`TeamConquest`), `MT-TC`, `RI-TC` (`Team Conquest`) |
| Players | 1 up to the map's `max` (30 to 100 on the production maps) in 1.1.2; two teams, Red and Blue |

Two teams fight over three beacons. You take a beacon for your team by right-clicking it again and again, 50 clicks
for a neutral one. Every time a player of the other team dies, your team scores one point for each beacon it holds
more than they do. The first team to the point goal wins.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38), and a port follows
them. Each part below names the ones that apply and describes this mode's own rules.

## Rules

These are 1.1.2's rules for the mode, bugs included: the mode isn't in 3.0 yet, and no change to its own rules is
confirmed. Where 1.1.2 does something odd, the rule names the item under [Not decided yet](#not-decided-yet) that
proposes a fix.

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two teams, Red and Blue. At the start the players are dealt out in the order the server lists online players:
  Red, Blue, Red, Blue, … Red gets the odd player. Lifetime K/D plays no part (item 14).
- Each team has one spawn point, `RedSpawn` and `BlueSpawn`.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- The standard kit, dyed red (`#B02E26`) or blue (`#3C44AA`): an unbreakable leather helmet, chestplate, leggings
  and boots, an unbreakable iron sword (first hotbar slot) and bow (second slot), and 24 arrows in the last hotbar
  slot. No food, no shield, no enchantments.
- It is handed out at the start and on a late join, and never again on a respawn (item 19). The only top-up is
  arrows: a player who holds fewer than 24 respawns with 24 in the last hotbar slot.
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- Every capture point's block becomes a beacon on a 3×3 of iron blocks one level down: a working beacon that shows a
  white beam where the sky is open.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24, S35.

**Capture points.** Every map point whose name contains `Point` is a capture point (item 4). Every playable
production map has three: `CapturePoint1`, `CapturePoint2` and `CapturePoint3`. Each point has a capture value from
-50 (Blue) to +50 (Red); 0 is neutral. On a map's first game since the server started, every point is at 0.

**Capturing.** Right-click a capture beacon while the game runs. The beacon's window doesn't open.

- Each click moves the value one step toward the clicker's team: +1 for Red, -1 for Blue.
- After the step the clicker reads "Cap at \<n>%" in their team color. The number is twice the value, seen from the
  clicker's side, so it is negative while the point still leans to the other team: "Cap at -96%".
- The click that reaches +50 (Red) or -50 (Blue): the clicker reads "Point Captured!". If their team didn't own the
  point, it does now: the glass block above the beacon becomes red or blue stained glass, which tints the beam, and
  every player of the game reads "Red Team captured a point!" or "Blue Team captured a point!". The other team loses
  the point.
- That click doesn't store the value: it stays at ±49 (item 1). Every later click by the owners prints "Cap at 100%"
  and "Point Captured!" again, and the other team needs 49 clicks to bring the value back to 0.
- A click on a point whose value is exactly 0 first prints "Point is neutral!" in gray, removes the glass, and takes
  the point from the other team. It never takes it from the clicker's own team (item 3). Then the click's own step
  follows, so a neutral point's first click also prints "Cap at 2%".
- So a neutral point takes 50 clicks, and an enemy point 99: 49 down to 0, the click at 0, 48 up, and the capturing
  click.
- Ownership changes only on the capturing click and on a click at 0. While the other team pushes your point from 49
  down to 1, you still own it and still score with it.
- Nothing else moves the value: no standing near it, no contest, no decay, no timer. When both teams click the same
  beacon, the faster clickers win. Holding the use button repeats the click about 5 times a second, so one player
  takes a neutral point in about 10 seconds and two players in about 5 (Minecraft's own repeat rate; not checked in
  game).
- The values carry over to the next game on the same map, until the server restarts (item 2).

**Scoring.** Every death of a player on a team, while the game runs, scores for the other team: the number of points
that team owns minus the number the dead player's team owns, when that is above 0.

- Every death counts, whatever killed the player: another player, a fall, lava, the void (item 11). A leave under
  fire (S24) is a death too, so it scores the same way.
- Examples: Red owns 3 and Blue 0: each Blue death gives Red 3. Red owns 1 and Blue 2: a Blue death gives Red
  nothing, and a Red death gives Blue 1. With equal ownership nobody scores.
- Owning points gives nothing else: no spawns, no time, no effects.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- Respawn is immediate, at the team's spawn. There is no wait and no spawn protection.
- The kit isn't handed out again; arrows are topped up to 24 (see [Kit](#kit), item 19).

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- **The goal.** After every death during the game: if Blue's score is at or above the goal, Blue wins; otherwise, if
  Red's is, Red wins. Only the team of the dead player's enemy can have scored, so there are no ties.
- **A team with no players left.** When a player logs out during the game and Red has no players left, Blue wins;
  otherwise, if Blue has none left, Red wins. Players who leave with `/pvp spectate` don't trigger this check
  (item 9); `/pvp leave` does, through the logout that follows it.
- There is no time limit and no draw (item 16). Otherwise only staff end a game.
- 1.1.2 showed no end summary (item 20). At the end each beacon and the block above it become air; the iron stays
  (item 7).

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S25, S34.

- Players may join during the countdown and the game.
- A player who played for a team earlier in this game goes back to it. Anyone else joins Blue when Red has at least
  as many players, else Red; on equal teams Blue (S22). The joiner goes to their team's spawn with a kit.
- `/pvp spectate` makes a player a spectator, and `/pvp join` puts them back on their team.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

## Settings

The mode isn't ported, so it has no 3.0 settings yet. 1.1.2 took one number when staff queued a game,
`/pvp game quickstart <map> <point goal>`; a port would carry it as 3.0's `scoreGoal` (S36).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| point goal (1.1.2), `scoreGoal` (port) | none: staff always typed it (item 15) | 1.1.2: any whole number above 0 | The score that wins. |
| map `max` (1.1.2), `maxPlayers` (port) | the map's `max` | 1.1.2: the map's `max` | The most players. |
| late joining (1.1.2), `midgameJoin` (port) | always on in 1.1.2 | 1.1.2: none | Whether late joiners play. |

## Map requirements

| Point | Meaning | How many |
|---|---|---|
| `RedSpawn`, `BlueSpawn` | the team's spawn, for the start, every respawn and late joins | one each |
| `CapturePoint1`, `CapturePoint2`, `CapturePoint3` | capture points | all three required |
| any other point whose name contains `Point` | also a capture point (item 4) | any number |
| the map's spawn (map field) | spectators | one |

- A map also needs its region: players can't leave it.
- 1.1.2 stored a point on the block under the map maker's feet. The beacon replaces that block, the iron goes one
  level lower, and the glass sits where the map maker's feet were. 3.0 imports points as standing positions, so a
  port puts the beacon on the block under the point.
- 1.1.2 refuses to start a map that lacks one of the five required points: the players read "Game Cannot Start! Not
  all needed points have been added!" (item 5).
- The capture points are read once per map, at its first game since the server started: points added later count
  after a restart.
- **Production maps.** Eight maps have exactly `RedSpawn`, `BlueSpawn` and `CapturePoint1` to `CapturePoint3`.
  - `OIE-TC` has `Point1`, `Point2`, `Point3` and a `SpectatorSpawn` instead, so it never started in 1.1.2 (item 17).
  - `DALR-TC` has `RedSpawn` and `BlueSpawn` on the same block (-1301, 56, -1540) (item 18).
  - On `MO-TC` each team spawns 2 blocks from a beacon: `RedSpawn` next to `CapturePoint1`, `BlueSpawn` next to
    `CapturePoint3`.
- **In 3.0 now.** All 9 maps import as unassigned arenas: 7 with `no 3.0 mode for "TeamConquest" yet` and 2 with
  `no 3.0 mode for "Team Conquest" yet`. Their `RedSpawn` and `BlueSpawn` become the `red` and `blue` lists of
  `teamSpawns`, and the capture points stay in `points`.

## What players see

The mode has no keys in `messages.yml` yet. These are 1.1.2's texts. A port words them as `messages.yml` keys, at
least `mode.teamconquest.name` and `mode.teamconquest.rules`, which every mode needs. The shared texts and who reads
them follow S2, S15 and S33 of the [README](README.md#rules-every-mode-shares).

| When | Text | Color | Who sees it in 1.1.2 |
|---|---|---|---|
| put on a team | "You are on the Red Team!" / "You are on the Blue Team!" | team color | the player |
| countdown | "Game begins in 5" … "Game begins in 1", then "Game Start!" | green | everyone online |
| start | "Use /unstuck if you're stuck in a block!" | gray, `/unstuck` green | the players |
| a click | "Cap at \<n>%" | team color | the clicker |
| a click at 0 | "Point is neutral!" | gray | the clicker |
| a click at ±50 | "Point Captured!" | team color | the clicker |
| a point changes owner | "Red Team captured a point!" / "Blue Team captured a point!" | team color | the players, not spectators |
| a kill | "\<victim> was killed by \<killer>" | names in team color, the rest gray | everyone on the server |
| a late join | "\<player> has joined the fight on Red Team!" / "… Blue Team!" | name and team in team color, the rest gray | everyone online |
| a logout | "\<player> left the fight!" | name in team color, the rest gray | everyone online |
| the end | "Game over!", then "Red Team Wins!" / "Blue Team Wins!" | the winner's color | everyone online |
| missing points | "Game Cannot Start! Not all needed points have been added!" | red | the players |
| leaving the region | "You aren't allowed to leave the map!" | red, at most once every 3 seconds | the player |

- **Sidebar**, from "Game Start!" to the end: the title "Score", and the lines "Blue:" (blue) and "Red:" (red) with
  each team's score, and "Goal:" (white) with the goal. A port uses 3.0's team-score sidebar (S15).
- **In the world:** each beacon's beam is white while the point is neutral, and red or blue through the glass of
  the team that owns it.
- **Rules text** (`/pvp rules teamconquest`): "Team Conquest Rules" in green, then four gray lines: "Two teams. There
  are 3 beacons, which each team can capture by repeatedly right clicking the beacon." "Points are awarded on kills,
  based on the difference between each team's number of beacons." "i.e. if Red has 3 beacons and Blue has 0, Red
  gets 3 point per kill. If Red has 1 and Blue has 2, Red doesn't get points for a kill." "First team to a certain
  point total wins."
- No action bar (except a supply chest's restock countdown), boss bar, title or sound.

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32. A port records results as S32 describes. 1.1.2
recorded:

- **Games played:** +1 for every player at the start, and for every late join, also a returning player's.
- **Won and lost:** every player on the winning team at the end gets a win, every player on the losing team a loss.
  Nobody leaves a team when they die in this mode, so that is everyone still playing; after a win by an emptied
  team, nobody gets a loss. Players who logged out or spectated get neither.
- **Spectated:** every spectator online at the end, including players who used `/pvp spectate`.
- A game that staff end records no win, loss or spectated game.

## Changes from 1.1.2

The confirmed changes every mode shares apply to a port: see the [README](README.md#rules-every-mode-shares). The
mode's own rules have no confirmed change: the mode isn't ported.

## Not decided yet

Shared items not decided yet that apply to a port: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13,
S18, S22, S33.

Every item below is open. Items 1 to 9 are 1.1.2 bugs and oddities, each with a proposed fix. Items 10 to 18 are
design questions. Items 19 and 20 are where 3.0's engine works differently from this mode's 1.1.2 rules. The rule
above is 1.1.2's in every case. 1.1.2 line numbers are in `Gamemode/TeamConquest.java` unless another 1.1.2 file is
named; 3.0 paths are under `src/main/java/com/mcmiddleearth/pvp/` (tests under `src/test/java/com/mcmiddleearth/pvp/`).

### 1.1.2 bugs and oddities

1. **A capture leaves the value at ±49.** 1.1.2: the click that reaches ±50 stores nothing (`:258-273`, `:290-305`),
   so the owners re-read "Cap at 100%" and "Point Captured!" on every click, and neutralizing takes 49 clicks instead
   of 50. Proposed fix: store ±50; a click on a point your team fully owns changes nothing and says nothing.
2. **Capture values carry over to the next game.** 1.1.2: the values live in the listener, made once per map until
   a restart (`:221-232`); the end of a game clears only ownership and glass (`:202-205`, `PVP/Team.java:219-220`).
   A point left at +49 falls to Red in one click next game, while Blue needs 99. Proposed fix: every point starts
   every game at 0.
3. **Ownership and glass can disagree.** 1.1.2: a click at 0 removes only the *other* team's ownership
   (`:243-252`, `:276-285`). If the enemy pushed your point to 0 and your team clicks it, your team keeps it and
   keeps scoring with it while the glass is gone; climbing back to 50 then shows no glass and no broadcast. Proposed
   fix: one source of truth: the point turns neutral on the click that brings it to 0, belongs to a team at ±50, and
   the glass always shows the owner.
4. **Any point named with `Point` is a capture point, and a stray beacon breaks the click.** 1.1.2: every key that
   contains `Point` becomes a capture point (`:226-230`); a click on a beacon that isn't one throws (`:240`), so it
   does nothing. Proposed fix: the capture points are `CapturePoint`, `CapturePoint1`, `CapturePoint2`, …, read like
   3.0's numbered point lists (`mode/rules/SpawnPoints.java:101-126`), and only their beacons count.
5. **Missing points leave a half-started game.** 1.1.2: after "Game Cannot Start!", the end of the game throws on a
   map's first game (`:106-111`, `:202`), so nothing is cleaned up and the game stays registered as running. This is
   what `OIE-TC` does. Proposed fix: nothing to port: 3.0 keeps an arena that lacks its points unassigned and refuses
   to open it (`mode/ModeRules.java:277-279`).
6. **A Red win queues the next game twice.** 1.1.2: `queueNextGame()` at `:368` and again in the end (`:210`); with
   two or more games queued, one is skipped. Proposed fix: nothing to port: 3.0 has no game queue.
7. **The beacon bases stay in the world.** 1.1.2: the end sets the beacon and the block above it to air
   (`:202-205`): the iron stays, and the map maker's floor block under each point is gone for good. Proposed fix:
   build the base like Capture the Flag's bases and put every block back when the game closes (S35, 3.0's block
   journal, `platform/BlockJournal.java:46`).
8. **The Helm's Deep TNT is dead code.** 1.1.2: on a map titled exactly `Helms_Deep`, once Red has 60 points, one
   random Red player would get TNT (`:336-346`), but that item is commented out in `Handlers/GearHandler.java`, and
   the production maps are titled `HelmsDeep` and `HelmsDeepOld`. Proposed fix: leave it out.
9. **`/pvp spectate` never ends the game.** 1.1.2: leaving that way runs an empty win check
   (`Gamemode/BasePluginGamemode.java:73-76`, `:201-202`), so a team emptied by it plays on until someone logs out.
   Proposed fix: nothing to port: 3.0 runs the win check after every leave (`game/Lifecycle.java:86-95`).

### Design questions

10. **Two spellings of the mode.** 1.1.2 loaded `TeamConquest` and `Team Conquest` alike, dropping spaces
    (`maps/Map.java:134`); production has 7 and 2 of them (`ProductionMapsTest.java:147`). 3.0 compares 1.1.2 names
    without spaces and case (`mode/ModeRegistry.java:39-47`, `:67-68`), so the one legacy name `TeamConquest` covers
    both. To decide: nothing beyond declaring that name; the census test should show both spellings assigned.
11. **Points per death or per kill.** 1.1.2 scores every death of a team member, killer or not (`:312-334`); its
    rules text says "on kills" (`command/PVPCommand.java:581`). To decide: keep every death (1.1.2's play) or score
    credited kills only. 3.0's `ModeLogic.onDeath` hears every counted death with its credited killer
    (`mode/ModeLogic.java:30`), so either is simple.
12. **Clicks or a zone.** 1.1.2 captures by clicking, and the faster clickers win a contested beacon. Siege captures
    by standing in a zone. To decide: keep click capture (1.1.2 and its rules text) or capture by presence.
13. **Progress shows only in the clicker's chat.** 1.1.2 prints one chat line per click to the clicker; teammates and
    the enemy see only the glass. To decide: keep, or show the value on the action bar or a boss bar near the point.
14. **Team balancing.** 1.1.2 sorts by K/D (`:99`) and then deals from the online list (`:135-150`), so K/D has no
    effect. 3.0's team modes balance by size and lifetime K/D (`balanced()`, `mode/rules/TeamBalancer.java:24-47`);
    without it they deal round-robin in join order (`:50-53`). To decide: round-robin (1.1.2's effect) or balanced.
15. **The default goal.** 1.1.2 had none: staff typed any number above 0 every time. 3.0's `scoreGoal` runs 1 to 100
    without a time limit and falls back to 1 when `config.yml` sets none (`game/Setting.java:14`,
    `mode/ModeSettings.java:56-60`). To decide: a default for `modes.teamconquest.scoreGoal`, and whether 100 is
    enough. Which goals staff used is unknown.
16. **No time limit.** 1.1.2: with equal ownership nobody scores, and a game runs until staff end it. To decide:
    keep it (1.1.2) or add an optional time limit.
17. **`OIE-TC`'s point names.** Its capture points are `Point1` to `Point3`. To decide: rename them to
    `CapturePoint1` to `CapturePoint3` in its arena file, or accept `Point` too.
18. **`DALR-TC`'s shared spawn.** Both teams spawn on the same block. To decide: keep it, or move one spawn in the
    arena file.

### Where 3.0's engine differs

19. **The kit after a respawn.** 1.1.2: never handed out again; only arrows were topped up to 24
    (`Handlers/AllGameHandlers.java:88-93`), and what else a respawning player carried depended on the world's
    `keepInventory` game rule, which 1.1.2 never sets. 3.0's immediate respawn hands out a fresh kit after every
    respawn (`game/Combat.java:308-317`). To decide: keep 1.1.2's rule (port it) or accept 3.0's (change this
    document).
20. **The summary's lists.** 1.1.2 showed no summary, only "Game over!" and the winner line. 3.0 ends every game with
    a summary (S30); 3.0's other team modes list the highest K/D and the most deaths
    (`platform/SummaryLines.java:25-55`). To decide: which lists a port shows.

## For developers

**1.1.2's code.** `Gamemode/TeamConquest.java` (487 lines) in 1.1.2's `com.mcmiddleearth.mcme.pvp` package, which 3.0
removed. `Start` `:97-194`; the end `:198-213`; the listener `GamemodeHandlers` `:219-421`: the capture points
`:225-232`, clicks `:234-310`, deaths, scoring and the goal `:312-375`, respawns `:377-386`, logouts `:388-420`; late
joins `:423-463`. Ownership is kept in `PVP/Team.java` (`getCapturedPoints`). The shared parts are in
`Gamemode/BasePluginGamemode.java` and `Handlers/AllGameHandlers.java`.

**3.0 pieces a port can reuse** (checked in the code):

| Piece | Where | Use here |
|---|---|---|
| two fixed teams, Red and Blue | `team/TeamLayout.java:14`, `:61-68`; `balanced()` `mode/ModeRules.java:328` | the teams |
| per-team spawns | `SpawnPoints.perTeam()`, `mode/rules/SpawnPoints.java:25`, `:57-95` | `RedSpawn`, `BlueSpawn` (the import makes them `teamSpawns`) |
| the standard kit | `mode/loadout/Kits.java:11` | the kit |
| immediate respawn | `Respawn.AT_SPAWN`, `mode/rules/Respawn.java:8` | respawns |
| late joining | `MidgameJoin.JOIN`, `mode/rules/MidgameJoin.java:11` | late joins |
| win conditions | `lastTeamStanding()` `mode/rules/WinConditions.java:33`, `firstTo(SCORE_GOAL)` `:50` | the goal and an empty team |
| team scores on the sidebar | `SidebarStyle.TEAM_SCORES`, `mode/rules/SidebarStyle.java:8` | Red, Blue and the goal |
| team score changes | `ScoreTracker.addTeamScore`, `game/ScoreTracker.java:32` | the ownership difference per death |
| hooks | `ModeLogic.onStart` `mode/ModeLogic.java:18`, `onDeath` `:30`, `onInteract` `:51` | beacons, scoring, clicks |
| beacon clicks | `platform/GameEventRouter.java:384-394`: a beacon stays closed, and the main-hand click reaches the game | one click is one step |
| structures | `mode/rules/Structure.java:12`, `StructureBlock.java:4` | the beacon on its iron |
| blocks changed during play, put back at the close | `Game.setBlock`, `game/Game.java:300`; `platform/BlockJournal.java:46` | the glass |
| notices to one player | `game/Notice.java:9`, `Game.notify` `game/Game.java:447` | "Cap at \<n>%" |

Not needed here: flags (`mode/rules/FlagRules.java:10`), delayed respawn and overtime.

**What would be new:**
- **Capture points:** per game, each point's value and owner; read from the `CapturePoint…` points; capture and
  neutral events with their texts. Nothing in 3.0 holds a point's ownership yet.
- **Building a mode's own structures at the countdown.** Today only flag bases and eggs are placed then, and only
  they are checked for blocks that hold data when a game opens (`mode/PlacedBlocks.java:20-28`). A port either
  extends this to capture beacons or places them itself in `onStart` with `Game.setBlocks` (`game/Game.java:314`).
- The texts: `mode.teamconquest.name`, `mode.teamconquest.rules` and the capture lines; `legacyNames()` returning
  `TeamConquest`; a `modes.teamconquest` section in `config.yml` and `PvpConfig.defaults()`.

**A sketch of the declarations, keeping 1.1.2's rules:** `teams(TeamLayout.fixed(2))` (round-robin, unless item 14
picks `balanced()`), `spawnPoints(SpawnPoints.perTeam())`, `loadout(LoadoutPolicy.fixed(Kits.STANDARD))`,
`respawn(Respawn.AT_SPAWN)`, `midgameJoin(MidgameJoin.JOIN)`,
`win(WinConditions.lastTeamStanding().orFirstTo(Setting.SCORE_GOAL))`, `sidebar(SidebarStyle.TEAM_SCORES)`, and a
`ModeLogic` for the beacons, clicks and scoring.

**Tests.** None yet. `ProductionMapsTest` pins the 9 maps as unassigned (`ProductionMapsTest.java:137-148`); a port
moves them into `PLAYABLE` (`:58-59`), changes the unassigned counts (`:113`, `:142`, `:147`), and must make every map
it plays start a game.
