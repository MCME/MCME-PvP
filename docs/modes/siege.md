# Siege

| | |
|---|---|
| Mode id | `siege` (1.1.2 name: `Siege`) |
| Status | Not ported to 3.0 yet |
| Production maps | 3: `DD-SG` and `SW-SG` (3 capture points each), `MTE-SG` (13) |
| Players | 1 up to the map's `max` (40 or 50 on the production maps) in 1.1.2; two teams, the Greens (defenders) and the Blacks (attackers) |

The Greens defend a line of beacons and the Blacks attack them, one at a time and in a fixed order. Blacks standing
around the beacon under attack fill its capture bar, and Greens standing there push it back. Every beacon the Blacks
take moves both teams' spawns forward and puts time back on the clock, and the Greens can win the last one back.
The Blacks win by taking the last beacon and keeping it; the Greens win when time runs out.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38), and a port follows
them. Each part below names the ones that apply and describes this mode's own rules.

## Rules

These are 1.1.2's rules for the mode, bugs included: the mode isn't in 3.0 yet, and no change to its own rules is
confirmed. Where 1.1.2 does something odd, the rule names the item under [Not decided yet](#not-decided-yet) that
proposes a fix.

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two teams: the **Greens**, in dark green, defend; the **Blacks**, in black, attack (item 13). 1.1.2's code calls
  the Greens "red" and the Blacks "blue", which is why their spawn points are named `RedSpawn` and `BlueSpawn`.
- At the start the players are sorted by lifetime K/D, lowest first (each comparison is nudged by a random amount
  of up to 0.3 either way), then dealt Greens, Blacks, Greens, … The lowest K/D goes to the Greens, and an odd player
  makes the Greens one bigger (item 16).
- A player put on a team reads "You´ve joined the Greens." in dark green or "You’ve joined the Blacks." in black.
- **Stages.** The stage is the number of the point under attack: 1 at the start, up to K (the number of points), and
  K+1 once the Blacks hold every point. At stage N the Greens spawn at `RedSpawnN` and the Blacks at `BlueSpawnN`;
  when the map has no spawn N, at N-1. So both teams start at `RedSpawn1` and `BlueSpawn1`, and at stage K+1 the
  production maps use spawn K.
- Neither team has team chat: `/t` answers "You aren't on a team!" (item 13).

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11, S14.

- The standard kit, dyed green (`#5E7C16`) for the Greens or black (`#1D1D21`) for the Blacks: an unbreakable leather
  helmet, chestplate, leggings and boots, an unbreakable iron sword (first hotbar slot) and bow (second slot), and 24
  arrows in the last hotbar slot. No food, no shield, no enchantments.
- It is handed out at the start, after every respawn and on a late join.
- **Arrow trickle.** Every 8 seconds from "Game Start!", every player on a team gains an arrow, up to 24:
  - with no arrows at all, they get 1 arrow in the last hotbar slot;
  - with arrows elsewhere but none in the last slot, those arrows are removed and they get 1 in the last slot;
  - with fewer than 24 in the last slot, 1 arrow is added.
- Supply chests restock 24 arrows (S9).
- The Greens' arrows are tinted green (`#00AA00`); the Blacks' arrows aren't tinted (S14, item 13).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

1. Every capture point's block becomes a beacon, on a 3×3 of iron blocks one level down, with green stained glass on
   the block above it, which tints its beam green where the sky is open.
2. Players go to their team's stage-1 spawn.
3. At "Game Start!":
   - the boss bar "Capture Point 1" appears, for the players on a team only (item 7);
   - the clock starts at the time limit, shown in the sidebar's title;
   - every beacon except the point under attack and the last point taken gets a stone block two above it, which
     blocks its beam, so only the active beacons shine.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S15, S24, S35.

**The front line.** The capture points `CapturePoint1` to `CapturePointK` are taken in that order. The Greens hold
them all at the start. At stage N the Blacks attack point N, and the Greens can win back point N-1, the last one the
Blacks took. No other point can change hands.

**Capture values.** Each point has a value: 0 is fully Green, 100 fully Black. Every point starts every game at 0.

**Who is in a zone.** Once a second, every player on a team is checked. A player is in a point's zone when both hold:
- they are less than 10 blocks from it, measured in 3D from their feet to the corner of the point's beacon block;
- there is a honey block 1 to 3 blocks under the block they stand on. Map makers build the honey under each zone's
  floor; without it nobody captures there.

Spectators and players waiting to respawn don't count, and a player who dies leaves the zone at once. When a Green
newly steps into the zone of point N-1, every Black gets the action bar "The Greens started recapturing your point!"
in dark red.

**Each second, in this order:**

1. **The last point is taken** (stage K+1): the overtime rules apply (see [Winning and the end](#winning-and-the-end)).
2. **Capture.** When point N's value has reached 100, the Blacks take it:
   - everyone online reads "The Blacks captured the next point." in black;
   - point N's glass turns black; point N-1 goes back to 100 and black glass, undoing any Green progress on it;
   - the sidebar gives the Blacks one flag more and the Greens one less;
   - the stage becomes N+1, so both teams' spawns move on;
   - the boss bar is titled "Capture Point N+1" (it keeps its title after the last point), and shows 0;
   - every player on a team hears an ender dragon growl;
   - the clock gains the bonus: 40% of the starting time, rounded down (240 seconds in a 10-minute game);
   - the beams move to the new active beacons.
3. **Otherwise, recapture.** When point N-1's value has dropped to 0, the Greens take it back:
   - a Greens' overtime that is running ends (see below);
   - point N goes back to 0 with green glass;
   - everyone online reads "The Greens recaptured the point." in dark green;
   - point N-1 is set to 0 with green glass; the Blacks lose a flag and the Greens gain one;
   - the stage becomes N-1, the boss bar is titled "Capture Point N-1" and shows 0, the growl plays, and the beams
     move. The clock doesn't change.
4. **Otherwise, the values move.** Let d be 2 × the difference between the Blacks and the Greens in a zone.
   - **Point N (under attack).** If nobody of either team is in its zone and its value is neither 0 nor 100, it drops
     by 2: it slowly goes back to the Greens. Then: more Blacks than Greens in the zone adds d; more Greens takes d
     off, but only when the value isn't exactly 0; equal numbers change nothing (contested).
   - **Point N-1 (from stage 2).** If nobody is in its zone and its value isn't 100, it rises by 2: it slowly goes
     back to the Blacks. Then: more Greens takes d off; more Blacks adds d, but only when the value isn't exactly 100;
     equal numbers change nothing.
   - Everyone in a zone, of both teams, reads "Cap at \<value>%" in gray, once a second: the value of the point whose
     zone they are in. Above 100 it reads 100%, below 0 it reads 0%.
   - The boss bar shows point N's value.

So one Black more than the Greens takes a point from 0 in 50 seconds, two in 25 seconds and five in 10 seconds. A
capture lands at the start of the next second. The checks look for exactly 0 and exactly 100, so a value can run past
them and keep going (item 1).

**Score.** The sidebar shows "The Blacks' Flags:", the points the Blacks hold (0 at the start), and "The Greens'
Flags:", the points still green (K at the start). Kills score nothing.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11, S38.

- A player who dies waits 5 seconds (S38), then rejoins the fight at the current stage's spawn, with a fresh kit,
  reading "You´ve joined the Greens." (or the Blacks' line). There is no spawn protection.
- 1.1.2 brought them back 5 to 6 seconds after dying (item 24). With two or more players waiting, its respawn
  countdown could skip a second or throw (item 10).

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

**The clock.** The time limit is the number staff gave, in minutes. The sidebar's title reads "Time: 10m" at
"Game Start!", then "Time: 9m 59s" and so on, and "Time: 59s" under a minute. The game reads "30 seconds remaining!",
then "10 seconds remaining!" down to "2 seconds remaining!" and "1 second remaining!", in green. Every Black capture
adds the bonus; a Green recapture adds nothing.

| How it ends | 1.1.2's result |
|---|---|
| the clock runs out, unless one of the next two rows applies (this includes the end of the Blacks' overtime, and any time after a Greens' overtime) | the Greens win |
| the clock runs out at stage K (the last point under attack) while at least one Black is in its zone, and the game had no overtime yet | **the Blacks' overtime:** the title gets the prefix "Overtime: ", the clock is set to 121 seconds, and everyone online reads "The game is going into overtime! Blacks: Try to capture this point!" in green. The moment no Black is in that zone, the Greens win; when the 121 seconds run out, the Greens win. Capturing the point also hands the Greens the win, because the capture empties the zone (item 2) |
| the Blacks take the last point and the game had no overtime yet | **the Greens' overtime:** the prefix "Overtime: ", the clock at 121 seconds, the boss bar hidden, and "The game is going into overtime! Greens: Try to recapture the point." in green. If the Greens win point K back in time, the game goes on at stage K: the bar and the plain title return, and the clock is set to the regular time that was left before the last capture, minus 1 second, minus the overtime seconds still left (item 3). If they don't, the Blacks win when the 121 seconds run out |
| the Blacks take the last point again after a Greens' overtime | the Blacks win, at the next second |
| a team is left with no players during the game | the other team wins. When the Greens emptied by a logout, chat then also announces "The Greens win!" (item 5). Players waiting to respawn count for their team (S38) |

- A game has at most one overtime. After a Greens' overtime, running out of time goes to the Greens.
- If the Greens' overtime leaves the clock at 0 or below, it never runs out again: the sidebar counts into negative
  seconds, and only a capture or an empty team ends the game (item 3).
- 1.1.2 ended with "Game over!" and "The Greens win!" in dark green, or "Game over!" and "The Blacks win!" in black,
  and showed no summary (item 25). At the end each beacon and the two blocks above it become air; the iron stays
  (item 11).

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S25, S34.

- Players may join during the countdown and the game.
- A player who played for a team earlier in this game goes back to it. Anyone else joins the Blacks when the Greens
  have more players, else the Greens; on equal teams the Greens. The joiner goes to the current stage's spawn with a
  kit. Players who join after "Game Start!" never see the boss bar (item 7).
- In 1.1.2 a player who logged out or used `/pvp spectate` kept counting in the zone they stood in (item 6), and
  `/pvp spectate` ran no win check.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Spectators see the sidebar but not the boss bar (item 7).

## Settings

The mode isn't ported, so it has no 3.0 settings yet. 1.1.2 took one number when staff queued a game,
`/pvp game quickstart <map> <minutes>`; a port would carry it as 3.0's `timeLimit` (S36).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| time in minutes (1.1.2), `timeLimit` (port) | none: staff always typed it; 3.0's `timeLimit` falls back to 10 | 1.1.2: any whole number above 0; 3.0's `timeLimit`: 1 to 60 | The starting time; each capture adds 40% of it. |
| map `max` (1.1.2), `maxPlayers` (port) | the map's `max` | 1.1.2: the map's `max` | The most players. |
| late joining (1.1.2), `midgameJoin` (port) | always on in 1.1.2 | 1.1.2: none | Whether late joiners play. |

## Map requirements

| Point | Meaning | How many |
|---|---|---|
| `CapturePoint1` … `CapturePointK` | the beacons, in attack order | at least 2, numbered 1 to K with no gaps (item 9) |
| `RedSpawnN` | the Greens' spawn at stage N | `RedSpawn1` and `RedSpawn2` required; one per stage on every production map |
| `BlueSpawnN` | the Blacks' spawn at stage N | `BlueSpawn1` and `BlueSpawn2` required; one per stage on every production map |
| the map's spawn (map field) | spectators | one |

- K is the number of points whose name contains `Point`, so no other point may have `Point` in its name (item 9).
- **Honey.** Each zone needs honey blocks 2 to 4 blocks under the players' feet, built into the map. The map files
  don't record blocks, so whether the production worlds have them is unknown.
- **The beacon column.** 1.1.2 stored a point on the block under the map maker's feet. That block becomes the beacon;
  the 3×3 one level lower becomes iron; the block above (feet level) holds the glass, and the one above that (head
  level) the stone. All four end the game as air or iron. 3.0 imports points as standing positions, so a port puts
  the beacon on the block under the point.
- A map also needs its region: players can't leave it.
- 1.1.2 refuses to start a map that lacks one of the six required points: the players read "Game cannot start! Not
  all needed points have been added!", and the start goes on anyway (item 12).
- When a map has neither spawn N nor N-1 for a team, its joiners and respawning players aren't moved.
- **Production maps.** `DD-SG` and `SW-SG` have 3 points with `RedSpawn1` to `3` and `BlueSpawn1` to `3`; `MTE-SG`
  has 13 points with `RedSpawn1` to `13` and `BlueSpawn1` to `13`. Consecutive points are 30 to 169 blocks apart, so
  the zones never overlap, and each stage's spawns are 9 to 90 blocks from that stage's point. On `MTE-SG` both teams
  spawn on the same block at stages 7, 9, 10 and 11, and 3 blocks apart at stage 12 (item 22).
- **In 3.0 now.** The 3 maps import as unassigned arenas with `no 3.0 mode for "Siege" yet`. Their `RedSpawn` and
  `BlueSpawn` points become the `red` and `blue` lists of `teamSpawns`, in number order, and the capture points stay
  in `points`.

## What players see

The mode has no keys in `messages.yml` yet. These are 1.1.2's texts. A port words them as `messages.yml` keys, at
least `mode.siege.name` and `mode.siege.rules`, which every mode needs. The shared texts and who reads them follow S2,
S15 and S33 of the [README](README.md#rules-every-mode-shares).

| When | Text | Color | Who sees it in 1.1.2 |
|---|---|---|---|
| put on a team | "You´ve joined the Greens." / "You’ve joined the Blacks." | dark green / black | the player |
| countdown | "Game begins in 5" … "Game begins in 1", then "Game Start!" | green | everyone online |
| start | "Use /unstuck if you're stuck in a block!" | gray, `/unstuck` green | the players |
| in a zone, every second | "Cap at \<value>%" | gray | everyone in a zone |
| a Green steps onto the last point taken | "The Greens started recapturing your point!" (action bar) | dark red | every Black |
| a capture | "The Blacks captured the next point." | black | everyone online |
| a recapture | "The Greens recaptured the point." | dark green | everyone online |
| a death | "You are Spectating!", "As a spectator, game participants won't see your chat." | gray | the dead player |
| waiting | "Respawn in 5" … "Respawn in 1" | green | the waiting player |
| a kill | "\<victim> was killed by \<killer>" | names in team color, the rest gray | everyone on the server |
| the clock | "30 seconds remaining!", "10 seconds remaining!" … "2 seconds remaining!", "1 second remaining!" | green | everyone online |
| overtime | "The game is going into overtime! Blacks: Try to capture this point!" / "… Greens: Try to recapture the point." | green | everyone online |
| a late join | an empty line | | everyone online |
| a logout | "\<player> left the fight!" | name in team color, the rest gray | everyone online |
| the end | "Game over!", then "The Greens win!" / "The Blacks win!" | the winner's color | everyone online |
| missing points | "Game cannot start! Not all needed points have been added!" | red | the players |
| leaving the region | "You aren't allowed to leave the map!" | red, at most once every 3 seconds | the player |

- **Sidebar**, from "Game Start!": the clock as its title, "Time: 9m 59s" with "Overtime: " in front during an
  overtime; the lines "The Blacks' Flags:" (black) and "The Greens' Flags:" (dark green).
- **Boss bar** "Capture Point N": created green, white within the first second and white from then on; its fill is
  the attack progress of point N. Only the players on a team at "Game Start!" see it, and it is hidden during the
  Greens' overtime (item 7).
- **Sound:** on every capture and recapture, every player on a team hears an ender dragon growl at their own
  position (volume 10, pitch 1).
- **In the world:** green or black glass above each beacon; only the point under attack and the last point taken
  shine, every other beacon has stone on top.
- **Rules text** (`/pvp rules siege`): "Siege" in green, then two gray lines: "Two Teams. There are 3 beacons, but on
  a linear format. You are forced to take one beacon at a time." "Each time the attacking team captures a beacon,
  they spawn near that beacon, until otherwise reclaimed by the defending team." 1.1.2 also prints these lines after
  the Death Run, Capture the Flag and Egg Hunt rules, and "3 beacons" is wrong for `MTE-SG` (item 14).
- No titles.

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32. A port records results as S32 describes. 1.1.2
recorded:

- **Games played:** +1 for every player at the start, and for every late join, also a returning player's.
- **Won and lost:** none. Siege recorded no wins and no losses for anyone (item 4).
- **Spectated:** every spectator online at the end, including the dead who are waiting to respawn.

## Changes from 1.1.2

The confirmed changes every mode shares apply to a port: see the [README](README.md#rules-every-mode-shares). The
mode's own rules have no confirmed change: the mode isn't ported.

## Not decided yet

Shared items not decided yet that apply to a port: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13,
S18, S22, S33.

Every item below is open. Items 1 to 15 are 1.1.2 bugs and oddities, each with a proposed fix. Items 16 to 23 are
design questions. Items 24 to 26 are where 3.0's engine works differently from this mode's 1.1.2 rules. The rule
above is 1.1.2's in every case. 1.1.2 line numbers are in `Gamemode/Siege.java` unless another 1.1.2 file is named;
3.0 paths are under `src/main/java/com/mcmiddleearth/pvp/` (tests under `src/test/java/com/mcmiddleearth/pvp/`).

### 1.1.2 bugs and oddities

1. **Values run past 0 and 100.** 1.1.2: the push can jump past a bound, and the checks look only for exactly 0 or
   100 (`:237-246`, `:256-275`). Two Greens more than the Blacks on a point at 2 take it to -2; from then on the
   empty-zone decay keeps pulling it down by 2 a second, without end, and the Blacks must climb back from there. The
   boss bar shows the size of the number, so a negative value looks like Black progress, while chat says 0%. Likewise
   a point being won back runs past 100 (98 + 4) and then gains 2 a second for good. Proposed fix: keep every value
   between 0 and 100.
2. **The Blacks can't win their overtime.** 1.1.2: capturing the last point empties the zone list (`:176-177`), and
   in the next second the clock's check, which runs first, sees the empty list and gives the Greens the win
   (`:119`). Proposed fix: the Blacks win their overtime by capturing the point, and lose it when no Black is in the
   zone or the time runs out.
3. **The Greens' overtime clock.** 1.1.2: after a retake the clock becomes the regular time left before the capture,
   minus 1 second, minus the overtime seconds still left (`:144`, `:188`), so a quicker retake leaves less time, and
   a result of 0 or below never runs out again. Proposed fix: after a retake, the clock goes on from the regular time
   that was left before the last capture.
4. **Siege records no wins or losses.** 1.1.2: both wins record the Red and Blue teams (`:571-573`, `:582-584`),
   which are empty in Siege, and 1.1.2's stats know no Greens or Blacks (`PVP/PlayerStat.java:117-191`). Proposed
   fix: nothing to port: 3.0 records every participant's result by their team (S32).
5. **A logout that empties the Greens announces both winners.** 1.1.2: the two team checks have no `else`
   (`:710-715`); the Blacks' win ends the game and empties both teams, so "The Greens win!" follows and the game ends
   twice. Proposed fix: one result.
6. **Players who leave keep counting in a zone.** 1.1.2: a logout removes the player from their team and the respawn
   queue, not from the zone lists (`:703-717`), and `/pvp spectate` only takes them off their team (`:606`); the
   once-a-second check visits only players on a team, so they count until the next capture or recapture. Proposed
   fix: only present, living players on a team count.
7. **The boss bar.** 1.1.2: only the players on a team at "Game Start!" see it (`:496-503`), so late joiners,
   reconnecting players and spectators never do; it turns white for good within a second (`:219-227`); and the
   progress of the point being won back shows nowhere but in chat. Proposed fix: show it to everyone in the game,
   with both active points' progress, colored by the team that is gaining.
8. **Ending during a map's first countdown throws.** 1.1.2: the end removes the boss bar, which doesn't exist before
   the first "Game Start!" (`:536`), so `/pvp game end` and an emptied team during that countdown fail, and the
   countdown starts the game anyway. Proposed fix: nothing to port: 3.0's countdown and stop don't depend on the mode.
9. **Strict point names.** 1.1.2: K is the number of points with `Point` in their name (`:670-682`), and the beacons
   are looked up as `CapturePoint1` to `CapturePointK` (`:588-603`): a gap or another name with `Point` in it throws
   at "Game Start!". Proposed fix: read the `CapturePoint…` points as a numbered list in number order (gaps allowed),
   as 3.0 reads numbered spawns (`mode/rules/SpawnPoints.java:101-126`).
10. **The respawn queue breaks with two players waiting.** 1.1.2: the queue removes entries while looping over it
    (`:344-358`); with two or more waiting, a respawn that isn't the last one in the loop throws and the rest of that
    second is skipped. Proposed fix: nothing to port: 3.0's delayed respawn (S38) keeps a wait per player
    (`game/Combat.java:99-118`).
11. **The beacon columns stay in the world.** 1.1.2: the end sets the beacon and the two blocks above it to air
    (`:519-525`): the iron stays, and the floor block and whatever was at feet and head level are gone for good.
    Proposed fix: put every changed block back when the game closes (S35, 3.0's block journal,
    `platform/BlockJournal.java:46`).
12. **Missing points don't stop the start.** 1.1.2: after "Game cannot start!", the start has no `return`
    (`:417-422`): on a map's first game the end throws, on later games the start carries on with no players.
    Proposed fix: nothing to port: 3.0 keeps an arena that lacks its points unassigned (`mode/ModeRules.java:277-279`).
13. **The Blacks are hard to see, and nobody has team chat.** 1.1.2: the Blacks' names, sidebar line and chat lines
    are black (`PVP/Team.java:47`), which is hard to read in chat; they have no scoreboard team, so their arrows are
    untinted (`Handlers/BukkitTeamHandler.java:110-149` has no case for black); team chat knows only Red, Blue,
    Infected and Survivors (`PVP/TeamChat.java:44-48`); and the two join lines use different apostrophes (´ and ’).
    Proposed fix: dark gray text for the Blacks, as 3.0 does for Death Run's Death (`team/Palette.java:43-44`),
    team chat for both teams (3.0's `/t` works for every team), and one apostrophe.
14. **The rules text says 3 beacons.** 1.1.2: "There are 3 beacons" (`command/PVPCommand.java:604`), but `MTE-SG` has
    13. Proposed fix: "a line of beacons".
15. **The old Siege package is dead code.** 1.1.2's `Gamemode/Siege_Otdated/` holds `Siege_Outdated` (an earlier
    Red and Blue click-capture mode, a copy of Team Conquest's clicks with no scoring), `SeigeEditor` (an empty
    class) and `TroopClass` (an unused skeleton with the classes Archer, Infantry and Cavalry and no gear). Nothing
    uses them, no map names them, and the map editor can't select them. Proposed fix: leave them out.

### Design questions

16. **The teams.** 1.1.2: the Greens always defend and the Blacks always attack; the teams are dealt in K/D order,
    Greens first. To decide: keep Greens and Blacks with these fixed roles (1.1.2), and whether teams are dealt in
    turn (1.1.2) or balanced by size and K/D like 3.0's other team modes (`mode/rules/TeamBalancer.java:24-47`).
17. **Zones and honey.** 1.1.2: a zone is everything less than 10 blocks from the beacon block's corner, over
    honey built into the map. To decide: keep honey as the map maker's marker (1.1.2), or describe each zone in the
    arena file (a radius and a height) so maps need no honey. Whether the production worlds still have their honey
    is unknown.
18. **Overtime, fresh.** 1.1.2's two overtimes contradict themselves (items 2 and 3). To decide: both 121-second
    overtimes as 1.1.2 meant them (the Blacks' when time runs out while they stand on the last point, the Greens'
    after the last point falls), with items 2 and 3 fixed, and still at most one per game.
19. **The time bonus.** 1.1.2: each capture adds 40% of the starting time; a recapture adds nothing. To decide: keep
    (1.1.2) or change. With 3.0's 60-minute cap on `timeLimit`, a bonus can still take the clock past 60 minutes.
20. **Progress in chat.** 1.1.2 sends "Cap at \<value>%" to everyone in a zone once a second, which floods chat. To
    decide: keep, or show it only on the boss bar or the action bar.
21. **Kills.** 1.1.2: kills score nothing; only the front line decides. To decide: keep (1.1.2).
22. **`MTE-SG`'s shared spawns.** At stages 7, 9, 10 and 11 both teams spawn on the same block. To decide: keep, or
    move the spawns in the arena file.
23. **Classes.** 1.1.2 has none: `TroopClass` (item 15) was never built. To decide: leave classes out (1.1.2), or
    design them as a later addition.

### Where 3.0's engine differs

24. **The respawn wait.** 1.1.2: "Respawn in 5" down to "Respawn in 1", one a second, and the player was back 5 to 6
    seconds after dying (`:344-358`). 3.0's delayed respawn (S38) counts the wait on the game's once-a-second beat, so
    with `respawnAfter(5)` a player is back 4 to 5 seconds after dying (`game/Combat.java:99-118`). To decide: a
    6-second wait, or accept 3.0's timing.
25. **The summary's lists.** 1.1.2 showed no summary, only "Game over!" and the winner line. 3.0 ends every game with
    a summary (S30); 3.0's other team modes list the highest K/D and the most deaths
    (`platform/SummaryLines.java:25-55`). To decide: which lists a port shows.
26. **One boss bar.** 1.1.2: the boss bar shows capture progress and the sidebar's title shows the clock. 3.0 has one
    boss bar per game, for the countdown, the time limit and overtime (`platform/ui/GameHud.java:22`,
    `platform/ui/HudRenderer.java:239-253`), and its sidebar title shows the time ("Time 9:41"). To decide: which bar
    shows what.

## For developers

**1.1.2's code.** `Gamemode/Siege.java` (753 lines) in 1.1.2's `com.mcmiddleearth.mcme.pvp` package, which 3.0
removed. The fixed numbers are at `:63-69` (step 2 per second, zone radius 10, respawn 5 seconds, arrows every 8
seconds). The once-a-second timers, all started at "Game Start!" in this order: the clock `tickCQ` `:97-135`, the
front line `flagPoints` `:137-327`, the arrow trickle `arrowHandler` `:329-342` (every 8 seconds), the respawn queue
`respawnTimer` `:344-358`, and the zone check `playerMove` `:360-406`. `Start` `:408-517`; the end `:519-549`; the
beams `activateBeacons` `:588-603`; late joins `:608-655`; the listener `:661-741` (the capture points `:670-682`,
deaths `:685-701`, logouts `:703-717`, respawns `:719-740`). The Greens and Blacks are in `PVP/Team.java:47-48`.

**3.0 pieces a port can reuse** (checked in the code):

| Piece | Where | Use here |
|---|---|---|
| delayed respawn | `respawnAfter(seconds)`, `mode/ModeRules.java:359-366`; `Respawn.AFTER_DELAY`, `mode/rules/Respawn.java:13` | the 5-second wait |
| per-team spawn lists | `SpawnPoints.perTeam()`, `mode/rules/SpawnPoints.java:57-95` (the import makes `RedSpawn1…K` the `red` list) | the stage spawns' data; 3.0 hands them out in turn, not by stage |
| per-team named spawns | `SpawnPoints.teamPoints`, `mode/rules/SpawnPoints.java:37`, `:194-240` | one fixed point per team, not one per stage |
| the standard kit, dyed per team | `mode/loadout/Kits.java:11`; team dye `armorRgb`, `team/TeamSpec.java:10` | the kit; `#5E7C16` is already `Palette.GREEN`'s dye (`team/Palette.java:17`) |
| ammunition with a timed refill | `mode/rules/Ammo.java:13`; `platform/AmmoKeeper.java:107-119` | the arrow trickle, as `new Ammo("ARROW", 8, 24, 0, false, 0, 8, 1)` |
| structures | `mode/rules/Structure.java:12`, `StructureBlock.java:4` | the beacon on its iron |
| blocks changed during play, put back at the close | `Game.setBlock`, `game/Game.java:300`; `platform/BlockJournal.java:46` | the glass and the stone |
| notices with a sound | `game/Notice.java:9-29`, `Game.notify` `game/Game.java:447` | "Cap at", the recapture alert, the growl (`entity.ender_dragon.growl`) |
| win conditions | `lastTeamStanding()` `mode/rules/WinConditions.java:33`; `holdOut(side)` `:58` | an empty team; the defenders' win at the time limit. `holdOut` names a side, which `ModeRules.build()` allows only in a layout with sides (`mode/ModeRules.java:548-550`, `mode/SideChecks.java:22-31`) |
| overtime | `withOvertime(…)`, `mode/rules/WinConditions.java:88`; `game/Lifecycle.java:302-313` | only a tie at the time limit, which stops the clock until the next score: not Siege's overtimes |
| a role per team | `mode/rules/TeamRole.java:8`; one random holder per team (`game/TeamPlacement.java:62-65`) | not an attacker/defender split |
| team scores on the sidebar | `SidebarStyle.TEAM_SCORES`, `mode/rules/SidebarStyle.java:8`; `platform/ui/SidebarLines.java:45-51` | the flags each team holds, if the team score is that count |
| hooks | `ModeLogic.onStart` `mode/ModeLogic.java:18`, `onDeath` `:30`, `onLeave` `:47`, `tick` `:84` | building the line, the zone check and the values each second |

Not needed here: flags (`mode/rules/FlagRules.java:10`).

**What would be new:**
- **Capture zones held over time:** per point a value from 0 to 100, the present and living players of each team in
  its zone once a second, contest, decay and regeneration, and capture and recapture events with their texts. 3.0
  watches no zones: `ModeLogic.onRegionEnter` fires only for a step onto the goal's block (`mode/ModeLogic.java:58`,
  `platform/GoalWatch.java:23-30`).
- **The front line:** the stage, the ordered points, and which beacons shine.
- **Stage spawns:** spawn N of each team's list while the stage is N.
- **A clock that gains time:** the time limit is set once, at the start (`game/Lifecycle.java:291`), and nothing adds
  to it.
- **Siege's two overtimes.**
- **Attacker and defender teams:** two fixed teams with their own names, colors and roles. `TeamLayout.fixed(2)`
  gives the palette's Red and Blue (`team/TeamLayout.java:61-68`), and `TeamLayout.sides` gives named sides but puts
  one random player on the role side (`team/TeamLayout.java:43-58`, `game/TeamPlacement.java:67-73`). A class
  system (item 23) would be new too.
- **A capture-progress boss bar**, or another place for it (item 26).
- **Building a mode's own structures at the countdown.** Today only flag bases and eggs are placed then, and only
  they are checked for blocks that hold data when a game opens (`mode/PlacedBlocks.java:20-28`).
- The texts: `mode.siege.name`, `mode.siege.rules` and the mode's lines; `legacyNames()` returning `Siege`; a
  `modes.siege` section in `config.yml` and `PvpConfig.defaults()`.

**Tests.** None yet. `ProductionMapsTest` pins the 3 maps as unassigned (`ProductionMapsTest.java:137-148`); a port
moves them into `PLAYABLE` (`:58-59`), changes the unassigned counts (`:113`, `:142`, `:147`), and must make every map
it plays start a game.
