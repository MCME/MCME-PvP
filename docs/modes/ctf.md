# Capture the Flag

| | |
|---|---|
| Mode id | `ctf` (1.1.2 name: `CaptureTheFlag`) |
| Status | Playable in 3.0 (not in a release yet) |
| Production maps | 5 (`COL-CTF`, `DAM-CTF`, `MO-CTF`, `RB-CTF`, `SR-CTF`) |
| Players | One per team up to `maxPlayers` (20 to 100 on the production maps); two teams, Red and Blue |

Two teams, Red and Blue, each with a flag on its base: a banner on a beacon. Right-click the enemy flag to take it,
carry it home on your head, and right-click your own flag to score a capture; your own flag must be at home for that.
The first team to 3 captures wins. When time runs out the team with more captures wins, and a tie goes to overtime,
where the next capture wins. The dead come back after 8 seconds.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S16, S18, S19.

- Two teams, Red and Blue, made when the countdown starts, by lifetime K/D (S16).
- Red players spawn on `RedSpawn1` and Blue players on `BlueSpawn1`: at the start, after every respawn and when they
  join late. `RedSpawn2` and `BlueSpawn2` are the flag bases, never spawns.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- **Standard kit:** leather helmet, chestplate, leggings and boots dyed in the team color (Red `#B02E26`, Blue
  `#3C44AA`), an iron sword in the first hotbar slot, a bow in the second and 24 arrows in the last. Armor, sword and
  bow never break. No enchantments, no food, no shield.
- The kit is given in full after every respawn. A capture gives the capturer a full new kit too, with 24 arrows (3.0
  differs: Not decided yet, item 1).
- A carrier wears the enemy banner instead of their leather helmet and keeps sword, bow and arrows.
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7, S35.

- The countdown needs one player per team.
- At the game's first countdown both bases are built (see Playing). A countdown that is cancelled leaves them
  standing for the next one.
- A game doesn't open, and its first countdown doesn't start, when a base block would replace a block that holds data
  (a sign, a container, a banner, a head, a spawner). The refusal names the block and where it is. Nothing changes in
  the world.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24, S35.

**The bases.** Each team's base stands on the floor block under its base point (`RedSpawn2` for Red, `BlueSpawn2`
for Blue):

- the floor block becomes a beacon, on a 3×3 of iron blocks right under it, so it works;
- the block above it, at the point itself, becomes the team's standing banner: the flag;
- the block three above the floor block becomes the team's stained glass, which tints the beam red or blue; the block
  between banner and glass is left alone.
- Every one of these blocks goes back to what it was when the game closes (after a crash, at the next start).
- The beacons can't be opened.

**Taking a flag.** Right-click the enemy team's banner on its base while it is home and you carry no flag. The banner
disappears from the base, you wear it on your head and you glow in your team's color. Only the banner on the base
counts: any other banner on the map is just a banner. You can take the enemy flag while your own is out.

**Carrying.** There is no slowness or other effect besides the glow. A carrier can't drop the flag.

**Capturing.** Right-click your own team's banner on its base while you carry the enemy flag. Your banner is there only
while your own flag is home, so your flag must be home to capture. Your team scores 1 capture, the enemy flag goes back
on its base, and your helmet comes back and your glow ends. Reaching the score goal wins at once.

**Returning.** A carried flag goes home at once, and the game is told, when its carrier dies, leaves the game (logs
out, `/pvp leave` or a kick) or stops playing in any other way. There is no dropped flag and no return timer.

**While a flag is out**, every player in the fight sees on their action bar, once a second, what to do: the carrier
to take it home, a player whose own flag is out to get it back, everyone else to protect their carrier.

**Kills** score nothing; only captures count.

**Leaving under fire** (S24) counts as a death: a carrier's flag goes home, and a player who comes back first waits
out the respawn wait.

**Time.** The time limit counts down (15 minutes by default). Chat warns at 30 seconds left and every second from 10
seconds down to 1.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11, S38.

- A player who dies waits as S38 describes, while their action bar counts down. 8 to 9 seconds after their death they
  respawn at their team's spawn with the full kit (3.0 differs: Not decided yet, item 2). There is no spawn
  protection.
- While they wait they can't hurt or be hurt, carry a flag, use supply chests or team chat.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- The first team to `scoreGoal` captures (3 by default) wins at once. With a goal of 0 only the clock decides.
- When time runs out (`timeLimit`, 15 minutes by default), the team with more captures wins. A tie, 0 to 0 included,
  goes to **overtime**: the clock stops, the game reads "Overtime! The next capture wins!", and the next capture wins.
  Overtime has no time limit.
- A team whose players have all left the game loses at once, also in overtime. Players waiting to respawn count for
  their team.
- The summary lists the highest K/D and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S34, S38.

- A newcomer joins the team with fewer players in the fight, players waiting to respawn not counted (3.0 differs: Not
  decided yet, item 3; on equal teams: S22), gets the kit at their team's spawn and plays at once.
- A player who played for a team earlier in the game goes back to that team. One who left while waiting to respawn
  first waits out the rest of their wait, as a spectator at the arena's spawn point, then respawns.
- A carrier who leaves sends their flag home.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Players waiting to respawn watch from where they died (S38).

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`. The defaults come from
`config.yml` (`modes.ctf.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 15 | 1 to 60 | Minutes until time runs out; then the most captures win, and a tie goes to overtime. |
| `scoreGoal` | 3 | 0 to 100 | Captures that win at once; 0: only the clock decides. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play by the rules above. Off: they watch. |

## Map requirements

- **`spawn`**: the arena's spawn point, where waiting players and spectators stand.
- **Four points** in `points` (names in any case): `RedSpawn1` and `BlueSpawn1`, the team spawns, and `RedSpawn2`
  and `BlueSpawn2`, the flag bases. A base point is a standing position: the beacon replaces the block under it, the
  banner the point's own block. Every production map has exactly these four; base to base is 30 to 153 blocks, a
  spawn to its own base 14 to 73 blocks.
- No base block may hold data when the game opens or first counts down. 1.1.2 left its iron bases in the world, and an
  empty space where each beacon was; 3.0 builds over them and puts back what it found.
- **Region**: optional. Players and spectators can't leave it.
- An arena is unassigned with "missing points: BlueSpawn1, BlueSpawn2, RedSpawn1, RedSpawn2" (the names it lacks), or
  "the map has no spawn point". Its file keeps the mode. `/pvpadmin arena info <id>` shows
  "Spawns: RedSpawn1 1, BlueSpawn1 1" and "Flags: red RedSpawn2, blue BlueSpawn2".

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).

**Flags**

- Taken: `game.flag-taken` "\<player> has taken the \<team> flag!" to the game (\<team> is the flag's team). The
  flag's team hears a bell (`block.bell.use`), the taker's team a chime (`block.note_block.chime`).
- To the carrier, in chat on taking it and on the action bar every second: `game.flag-you-carry` "You have the enemy
  flag! Right-click your own flag to capture it!".
- Action bar of the others while any flag is out: `game.flag-yours-taken` "The enemy stole your flag, retrieve it!"
  (their own flag is out) or `game.flag-teammate-carries` "A teammate has taken their flag, protect them!". Players
  outside the game, spectators and players waiting to respawn don't get these lines.
- Captured: `game.flag-captured` "\<player> has captured the \<team> flag!", the title `hud.title-flag-captured`
  "\<player> captured the \<team> flag!" and the sound `ui.toast.challenge_complete`, all to everyone in the game.
- Back home: `game.flag-returned` "The \<team> flag is back at its base." to the game, and `block.beacon.activate` for
  the flag's team. A flag that goes home after the game has ended goes quietly.
- The carrier glows in their team's color and wears the flag's banner.

**Respawning and overtime**

- While waiting: `hud.actionbar-respawning` "Respawning in \<seconds>", from 7 down to 1.
- `/t` while waiting: `error.team-chat-waiting` "You can talk to your team again once you're back in the fight.".
- Overtime: `game.overtime-ctf` "Overtime! The next capture wins!" in chat, and the boss bar `hud.bossbar-overtime`
  "\<mode> · Overtime" (purple).

**HUD**

- Sidebar: the title `hud.sidebar-title` "\<mode>" with `hud.sidebar-time` "Time \<time>" ("Capture the Flag Time
  14:59"; no time in overtime), then `Red` and `Blue` with their captures and `hud.sidebar-goal` "Goal" with the score
  goal (when it is above 0).
- Boss bar: `hud.bossbar-countdown` "Starting in \<seconds>s" during the countdown, then `hud.bossbar-time` "\<mode> ·
  \<time> left".
- Time warnings: `game.time-warning` "\<time> remaining!" with `time.seconds` or `time.second`: "30 seconds
  remaining!", "10 seconds remaining!" … "1 second remaining!".
- Action bar while no flag is out: `hud.actionbar-score` "Kills \<kills> · Deaths \<deaths>".

**Other texts**

- The countdown: `game.countdown-started` "The game starts in \<seconds> seconds!", `join.team` "You're on team
  \<team>.", `game.countdown-tick` "Game begins in \<seconds>" (5 to 1), `game.started` "Game Start!", titles
  `title.countdown` and `title.fight` "Fight!".
- `game.death` "\<victim> was killed by \<killer>" (other deaths keep their usual message), to the game only.
- `game.fled` "\<player> left the fight and counts as killed by \<killer>." and `game.fled-alone`.
- `game.player-joined-team` "\<player> joined the fight on \<team>.", `game.player-left` "\<player> left.".
- Supply chests: `restock.arrow.restocking` "Restocking arrows… \<seconds>", then `restock.arrow.restocked` "Arrows
  restocked!". Leaving the region: `game.boundary` "You aren't allowed to leave the map!".
- A refused opening or countdown: `error.arena-data-block` "\<arena> can't be used: the game would replace \<block> at
  \<x> \<y> \<z>.".
- The end: `summary.header` "Game over — \<mode> on \<arena>", `summary.team-won` "\<team> won!" (or `summary.draw`),
  `summary.best-kd` "Highest K/D: \<entries>", `summary.most-deaths` "Most deaths: \<entries>", and the titles
  `title.victory`, `title.defeat` or `title.draw`; the result stays `endingSeconds` (10).

**Rules text** (`mode.ctf.rules`): "Two teams, each with a flag on its base. Right-click the enemy flag to take it,
carry it home and right-click your own flag to score; your own flag must be home. A carrier who dies or leaves sends
the flag home. The first team to the score goal (3 by default) wins; at the time limit the most captures wins, and a
tie goes to overtime, where the next capture wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- Every member of the winning team gets a win and every member of the losing team a loss, players waiting to respawn
  included. A draw counts as neither. A player who left the game gets neither.
- The match record lists the winner and each player's team, kills, deaths and result. Captures are team scores, and
  the record keeps only player scores, so it has no capture counts.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S27, S30, S31, S32, S34, S35, S36, S37, S38. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Time running out | Red's score was compared with an unused score line: Blue could never win on time, Red won with any capture even from behind, and Red at 0 meant overtime | The team with more captures wins; a tie goes to overtime | 1.1.2 read the wrong score |
| The winning capture | The win was checked at the next click after it | The game ends on the capture | The game must end when the goal is reached |
| Which banner is the flag | Any standing banner of the team's color, anywhere on the map, could be taken or used to capture | Only the banner on the team's base | Decoration banners counted as flags |
| A carrier who leaves | The flag was lost for the rest of the game | The flag goes home | Neither team could capture any more |
| The carrier | No marker besides the banner | Glows in their team's color | Easier to find and protect |
| A flag going home | Silent | Announced, with a sound | Players know the flag is back |
| Score goal and time limit | 3 captures, fixed; the time limit in minutes as staff typed it | `scoreGoal` 3 by default (0 to 100), `timeLimit` 15 by default (1 to 60) | Staff can choose |
| The map after a game | The iron bases stayed, with an empty space where each beacon was | Every block goes back | Games damaged the maps |
| Waiting to respawn | As a spectator at the arena's spawn point, off the team, with "You are Spectating!" and "Respawn in 8" … "Respawn in 1" in chat; a player who left while waiting was refused ("Can't join CaptureTheFlag midgame!") until the wait, which kept running while they were away, was over | As S38 describes: on the team, where they died, with the countdown on the action bar; a player who comes back first waits out the seconds they had left | Players see the fight they died in; waiting is not losing |
| Overtime | Sidebar title "Overtime"; "Overtime! The next captured flag wins!" | The boss bar shows overtime; "Overtime! The next capture wins!" | Clearer |
| Captures | Chat only | Chat, a title for everyone in the game, and sounds for taking, returning and capturing | Easier to follow |
| The action bar | Blanked for everyone online every second while no flag was out | Only players in the game get flag lines | Other games' and the lobby's action bars stay |
| The HUD | Sidebar title "Time: 14m 59s"; lines `Goal:`, `Blue:`, `Red:` | Sidebar `Red`, `Blue`, `Goal`; the time on the sidebar and the boss bar | One layout for every mode |
| Wins and losses | Only living players got a win or a loss; the dead got a spectated game | Everyone on each team, players waiting to respawn included | Every player shares their team's result |
| Crashes | Two or more players waiting to respawn could crash the respawn queue; a team emptying on a quit announced both teams as winners | Gone | Bugs |
| Rules text | One line, followed by the Egg Hunt and Siege texts | The text above (`mode.ctf.rules`) | Clear and complete |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S16,
S18, S22, S33.

Code references are relative to `src/main/java/com/mcmiddleearth/pvp/`.

1. **A capture refills the capturer's kit.** 1.1.2: a capture gave the capturer a full new kit, back to 24 arrows.
   3.0 now: only their helmet comes back; arrows used stay used (`game/FlagPlay.java:87-103`). Rule above: 1.1.2's. To
   decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
2. **The respawn wait is a second shorter.** 1.1.2: "Respawn in 8" down to "Respawn in 1", one a second, and the
   player was back 8 to 9 seconds after dying. 3.0 now: the wait is counted on the game's once-a-second beat, so the
   player is back 7 to 8 seconds after dying, and the action bar shows 7 down to 1 (`mode/ModeRules.java:127-134`,
   `game/Combat.java:105-117`, `platform/ui/ActionBarLines.java:46-49`). Rule above: 1.1.2's. To decide: keep 1.1.2's
   rule (change the code) or accept 3.0's (change this document).
3. **Which team a late joiner gets.** 1.1.2: the team with fewer players in the fight, players waiting to respawn not
   counted. 3.0 now: players waiting to respawn count for their team (`game/TeamPlacement.java:116-118`,
   `game/GameTeam.java:44-46`). Rule above: 1.1.2's. To decide: keep 1.1.2's rule (change the code) or accept 3.0's
   (change this document).

## For developers

- **Class:** `mode/ctf/CaptureTheFlag.java`: the declarations and `base(team, point)`, the structure of a base. The
  mode has no `ModeLogic`: the engine runs the flags, the waits and overtime.
- **ModeRules:**
  - layout: `teams(TeamLayout.fixed(2)).balanced()` (Red, Blue; `TeamBalancer.assign`);
  - spawns: `SpawnPoints.teamPoints(Map.of("red", "RedSpawn1", "blue", "BlueSpawn1"))`;
  - kit: `LoadoutPolicy.fixed(Kits.STANDARD)`;
  - respawn: `respawnAfter(8)` (`Respawn.AFTER_DELAY`);
  - flags: `new FlagRules(List.of(base("red", "RedSpawn2"), base("blue", "BlueSpawn2")))`; a base is the 3×3
    `minecraft:iron_block` at dy −1, `minecraft:beacon` at (0, 0, 0), the flag `minecraft:<team>_banner` at (0, 1, 0)
    and `minecraft:<team>_stained_glass` at (0, 3, 0), relative to the floor block under the point; the carried item
    is `RED_BANNER` or `BLUE_BANNER`;
  - late join: `MidgameJoin.JOIN`;
  - win: `withOvertime(lastTeamStanding().or(highestAt(TIME_LIMIT)).orFirstTo(SCORE_GOAL))`, from
    `WinConditions` and `Setting`;
  - settings: `timeLimit`, `scoreGoal`, `maxPlayers`, `midgameJoin`; no spawn protection;
  - extras: `sidebar(SidebarStyle.TEAM_SCORES)`; supply chests give the default `Restock.ARROWS`.
- **Engine and platform pieces:** flags `game/FlagPlay.java` (take, capture, return), `game/GameFlag.java`,
  `game/FlagReturn.java`; waits `game/Combat.java` (`tickRespawnWaits`, `backIntoPlay`) and
  `PlayerSession.waitingToRespawn`; overtime `WinConditions.withOvertime` and `game/Lifecycle.java` (`runSecond`);
  placed blocks `mode/PlacedBlocks.java`, `game/DataBlocks.java` and the block journal `platform/BlockJournal.java`;
  the death place `platform/GameEventRouter.java` (`viewpoint`); action-bar lines `platform/FlagStatus.java`; sounds
  `platform/GameSounds.java`; the capture title `platform/ui/HudRenderer.java`.
- **Tests:** `mode/ctf/CaptureTheFlagTest.java` (rules, bases, take, capture, return), `CaptureTheFlagRoundsTest.java`
  (respawn wait, goal, time limit, overtime, late joins, results), `CaptureTheFlagPaperTest.java`; shared
  `game/FlagsTest.java`, `game/OvertimeTest.java`, `game/RespawnDelayTest.java`, `game/StructuresTest.java`,
  `platform/FlagsPaperTest.java`, `platform/FlagStatusTest.java`, `platform/FlagBasesAcrossCountdownsTest.java`; the
  production maps in `ProductionMapsTest`.
