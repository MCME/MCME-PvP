# Snowball

| | |
|---|---|
| Mode id | `snowball` (1.1.2 name: `Snowball`) |
| Status | Playable in 3.0 |
| Production maps | 9 (`A-SB`, `BR-SB`, `LDp-SB`, `SV-SB`, …) |
| Players | 2 up to `maxPlayers`; free for all, no teams |

Everyone fights for themselves with snowballs only, and one hit kills. Snowballs come back over time and for every
kill, so the fight never stops. The player with the most kills when time runs out wins, unless someone reaches the
score goal first.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S17, S18, S19, S20.

- Everyone fights for themselves; everyone can hurt everyone.
- Each player has a personal color (S17), shown on the armor, in chat and in the tab list. Armor dyes: Aqua
  `#3AB3DA`, Blue `#3C44AA`, Dark Aqua `#169C9C`, Dark Green `#5E7C16`, Dark Purple `#8932B8`, Dark Red `#B02E26`
  (the same red as Red), Gold `#9D9D97` (light gray), Green `#80C71F`, Light Purple `#C74EBD`, Red `#B02E26`, Yellow
  `#FED83D` (3.0 differs for Dark Red and Gold: S17).
- The spawns are `PlayerSpawn`, then `PlayerSpawn1`, `PlayerSpawn2`, … by number. At the start players are placed
  on them in turn.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- Leather helmet, chestplate, leggings and boots, dyed in your color, and 8 snowballs in the first hotbar slot.
  Nothing else: no weapon, no bow, no food. The armor never breaks.
- **Snowballs.** You never carry more than 16.
  - Every 5 seconds, every player in the fight gets 3 more. The first of these top-ups comes right as the fight
    starts, so everyone begins the fight with 11 (3.0 differs: Not decided yet, item 1).
  - A kill gives 3 more.
  - After a death you have the kit's 8 again.
- **Supply chests.** Right-clicking one during the fight with fewer than 16 snowballs puts 16 in the first slot at
  once, with the slowness and countdown of S9: "Restocking snowballs… \<seconds>", then "Snowballs restocked!".

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs 2 players. Players can't throw until the fight starts, so nobody starts short of snowballs
  and no snowball thrown in the countdown lands after the start.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S14, S15, S24.

- **One hit kills.** A snowball that hits an opponent kills them, and the kill goes to the thrower. Minecraft's hit
  cooldown still applies: a snowball that lands within about half a second (10 ticks) of other damage to the same
  player, such as a punch or a fall, does nothing (3.0 differs: Not decided yet, item 3). A punch does normal damage.
- **Score.** Every kill scores 1 point for the killer. The kill goes to the player Minecraft credits, so a fall
  right after an enemy's hit counts for that enemy. A death with no credited killer scores nothing.
- **Leaving under fire** (S24) gives the attacker a kill and a point, but no snowballs (Not decided yet, item 2).
- **Time limit:** `timeLimit` minutes, 10 by default. Chat warns at 30 seconds left and every second from 10 to 1.
- **Spawn protection.** After a respawn nobody can hurt you for about 8 seconds (between 7 and 8: the game counts
  whole seconds). It ends at once when you throw a snowball or hit someone. Falling into the void and `/kill` still
  get through.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11, S20.

- Everyone respawns at once, forever, at the next `PlayerSpawn` point in turn.
- After a respawn you have the kit again, with 8 snowballs, and the spawn protection above.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S26, S30, S31.

- When time runs out, the player with the most kills wins. A tie at the top, or nobody with a kill, is a draw.
- A player who reaches the score goal (`scoreGoal`, default 100) wins at once. A goal of 0 turns this off.
- Leaving never ends the game (3.0 differs: S26).
- The summary lists the highest K/D, the most kills and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S17, S21, S23, S24, S34.

- Players can always join a game that has started. A late joiner plays at once, at the next spawn in turn, with the
  kit's 8 snowballs.
- A player who played earlier in the game gets their own color back and keeps their kills.
- Leaving has no effect on the game.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`, for example
`/pvpadmin start A-SB timeLimit=15 scoreGoal=0`. The defaults come from `config.yml` (`modes.snowball.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `timeLimit` | 10 | 1 to 60 | Minutes until time runs out. |
| `scoreGoal` | 100 | 0 to 100 | Kills that win at once; 0 means no goal. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play at once. Off: they watch. |

## Map requirements

- **Spawns:** points named `PlayerSpawn`, `PlayerSpawn1`, `PlayerSpawn2`, …, matched in any case: the unnumbered
  one first, then by number. Numbers may skip. At least one is needed. Points with other names are not spawns.
- **The arena spawn** (`spawn`): where waiting players and spectators stand.
- **A region** keeps players in the arena. Without one, the game reserves its whole world.
- **Unassigned:** an arena without any `PlayerSpawn` point is set aside at startup with "missing points:
  PlayerSpawn"; one without an arena spawn with "the map has no spawn point".
- **Production maps:** 9, with 7 to 28 spawn points each, numbered without gaps and with no other points.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).
Texts are quoted from `messages.yml` without their color tags.

- **Sidebar.** The title is the mode's name (`hud.sidebar-title`), followed by "Waiting \<count>/\<max>"
  (`hud.sidebar-waiting`), "Starting in \<seconds>s" (`hud.sidebar-countdown`), or during the fight "Time \<time>"
  (`hud.sidebar-time`, as `9:41`). From the countdown on, it lists the five best players by score, each name in its
  color with the player's kills.
- **Boss bar.** "Starting in \<seconds>s" (`hud.bossbar-countdown`) during the countdown; during the fight "\<mode> ·
  \<time> left" (`hud.bossbar-time`), shrinking as time runs out.
- **Action bar** during the fight: "Spawn protection \<seconds>s · attacking ends it" (`hud.actionbar-protected`)
  while protected, otherwise "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`). At a supply chest:
  "Restocking snowballs… \<seconds>" (`restock.snowball.restocking`) for 3, 2 and 1, then "Snowballs restocked!"
  (`restock.snowball.restocked`) for 1 second. Spectators: "Spectating \<arena> · /pvp leave"
  (`hud.actionbar-spectating`).
- **Titles.** "\<seconds>" (`title.countdown`) for the last 5 seconds of the countdown, "Fight!" (`title.fight`) at
  the start, and "Victory" (`title.victory`), "Defeat" (`title.defeat`) or "Draw" (`title.draw`) at the end. A
  game stopped by staff or by a server stop shows no result title.
- **Sounds.** None.
- **Chat,** to the game's players and spectators only:
  - "The game starts in \<seconds> seconds!" (`game.countdown-started`);
  - "Game begins in \<seconds>" (`game.countdown-tick`) from 5 to 1, then "Game Start!" (`game.started`);
  - "Countdown cancelled: not enough players." (`game.countdown-cancelled`);
  - "\<time> remaining!" (`game.time-warning`) at 30 seconds left and from 10 to 1, with \<time> from `time.seconds`
    ("30 seconds") or `time.second` ("1 second");
  - "\<victim> was killed by \<killer>" (`game.death`) for a credited kill; any other death shows Minecraft's own
    death message, to the game only;
  - "\<player> left the fight and counts as killed by \<killer>." (`game.fled`), or "\<player> left the fight and
    counts as dead." (`game.fled-alone`);
  - "You joined \<arena> (\<mode>)." (`join.joined`) to a joiner, and "\<player> joined (\<count>/\<max>)."
    (`game.player-joined`) to the others, before and after the start;
  - "You left the game." (`leave.left`) to a leaver, and "\<player> left." (`game.player-left`) to the others.
- **End summary:** "Game over — \<mode> on \<arena>" (`summary.header`); "\<player> won with \<score> kills!"
  (`summary.player-won`, or `summary.player-won-one` for 1 kill), "It's a draw." (`summary.draw`) or "Everyone
  left, so the game on \<arena> ended." (`game.abandoned`); then "Highest K/D: \<entries>" (`summary.best-kd`), "Most
  kills: \<entries>" (`summary.most-kills`) and "Most deaths: \<entries>" (`summary.most-deaths`), the top 3 each,
  every entry "\<player> (\<value>)" (`summary.entry`). Then [Games] and [My stats] buttons. The result stays for
  `endingSeconds` (10 seconds), then everyone goes to the lobby.
- **Rules:** `/pvp rules snowball` shows "\<mode> rules" (`rules.header`) and `mode.snowball.rules`: "Everyone for
  themselves, with snowballs only. One hit kills. You get snowballs back over time and for every kill. The most
  kills when time runs out wins, or the first to the score goal (100 by default)."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Result.** The winner gets a win, and every other player who took part a loss. A draw (a tie at the time limit,
  nobody with a kill, or a game stopped by staff or by a server stop) is a draw for everyone. A player who left
  before the end and did not come back to play gets "left".
- **Kills and deaths:** every credited kill counts a kill, also one from leaving under fire; every death during the
  fight counts a death, whatever its cause.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| Spawn protection | For 7.5 seconds after a respawn you were healed to full, but a snowball still killed you | About 8 seconds in which nobody can hurt you; it ends when you throw or hit, and the action bar counts it down | Real protection, which can't be used to strike unanswered |
| Snowball cap | None | At most 16 | Keeps the supply in check |
| Tie at the time limit | Everyone tied for the most kills got a win, and no winner was announced | A draw | A tie has no winner |
| Supply chests | 24 arrows in the last slot, of no use here | Snowballs up to 16 | Chests give what the mode uses |
| Time limit and goal | The minutes had to be given when starting; 100 kills always won | `timeLimit`, default 10 minutes; `scoreGoal`, default 100, and 0 turns it off | A game can start with sensible defaults |
| Spawn points | Every point on the map was a spawn, in no fixed order | Only `PlayerSpawn`, `PlayerSpawn1`, …, by number; the production maps have no other points | Other points can't become spawns by accident |
| End summary | Its own: everyone tied for the most kills, the most deaths and the best K/D, on one line each with no limit; deaths counted only kills by another player; a player never killed had a K/D of "infinity" | Free For All's summary (S30): the top 3 of the highest K/D, the most kills and the most deaths, every death counted, and the kills as the K/D of a player without deaths | One summary for the modes without teams |
| Rules text | None: `/pvp rules snowball` printed nothing | See What players see | Snowball had no rules text |
| Wins and losses | A win for every player tied for the most kills, a loss for nobody | Won, lost or draw for every player; "left" for players who left before the end | Every mode records results the same way |
| Errors | A late joiner's arrival printed an empty chat line to everyone, and could hit an error at the end of the spawn list | Fixed | They were bugs |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S17, S18,
S20, S26, S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **Snowballs at the start of the fight.** 1.1.2 started the 5-second top-up together with the fight, so its first
   +3 came right after the kit's 8 were handed out at "Game Start!": everyone began with 11 snowballs and got the
   next 3 at 5 seconds (`Gamemode/Snowball.java:191-207`, `:266`, `:294`). 3.0 now: everyone begins with the kit's
   8; the first +3 comes after 5 seconds of play (`platform/AmmoKeeper.java:106-119`). Rule above: 1.1.2's. To
   decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
2. **No snowballs for a kill by leaving under fire.** 1.1.2 had no such kill: leaving never counted. 3.0 now: the
   attacker of a player who leaves under fire gets the kill and the point, but not the 3 snowballs a kill gives
   (`game/Combat.java:61-68`, `platform/AmmoKeeper.java:99-104`). Rule above: no snowballs, as 3.0 does (1.1.2 has
   no rule for it). To decide: give the snowballs too (change the code) or accept 3.0's (keep this document).
3. **A snowball right after other damage.** 1.1.2 left Minecraft's hit cooldown alone, so a snowball that landed
   within about half a second of other damage to the same player (a punch, a fall) was ignored
   (`Handlers/AllGameHandlers.java:154-158` only sets the damage of a hit that gets through). 3.0 now: a lethal
   snowball clears the victim's cooldown first, so it always kills (`platform/WeaponRules.java:48-56`). Rule above:
   1.1.2's. To decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).

## For developers

- **Mode class:** `src/main/java/com/mcmiddleearth/pvp/mode/snowball/Snowball.java`. Id `snowball`; legacy name
  `Snowball`.
- **Declarations** (`Snowball.java:32-44`):
  - layout: `noTeams()`, personal colors;
  - spawns: `SpawnPoints.numbered("PlayerSpawn")`;
  - kit: `LoadoutPolicy.fixed(Kits.SNOWBALL)`;
  - respawn: `Respawn.AT_SPAWN`, with `spawnProtection(SPAWN_PROTECTION_SECONDS)`, 8 (`Snowball.java:25`);
  - score: `killScore(1)`;
  - late join: `MidgameJoin.JOIN`;
  - win: `WinConditions.highestAt(Setting.TIME_LIMIT).orFirstTo(Setting.SCORE_GOAL)`;
  - weapons and supplies: `lethalSnowballs()`, `restock(Restock.SNOWBALLS)` (16 in slot 0);
  - ammunition: `SNOWBALLS = new Ammo("SNOWBALL", 0, 16, 3, false, 0, 5, 3)` (`Snowball.java:30`): slot 0, at most
    16, +3 per credited kill, back to the kit's 8 after a death, +3 every 5 seconds of play.
- **Settings** derived from the declarations: `timeLimit`, `scoreGoal` (0 allowed, since there is a time limit),
  `maxPlayers`, `midgameJoin`. Defaults: `config.yml`, `modes.snowball`.
- **ModeLogic hooks:** none; the mode is declarations only.
- **Engine and platform pieces it relies on:** `WeaponRules` (a snowball is lethal at hit time and resets the
  victim's hit cooldown, `WeaponRules.java:41-56`), `AmmoKeeper` (kill gifts, the timed refill, the cap),
  `SupplyChests`, `DamageRules` (spawn protection blocks all damage but the void and `/kill`,
  `DamageRules.java:67-90`), `GameEventRouter` (a throw or a hit ends the protection, `:188-208`; no throw while
  frozen, `:286-293`), `Combat.intoPlay` (protection after each respawn, `Combat.java:308-317`),
  `ui/ActionBarLines` (the protection line), `SpawnCursor`, `TeamPlacement.nextPersonalColor`, `SummaryLines`.
- **Texts:** `mode.snowball.name`, `mode.snowball.rules`, `restock.snowball.*`, `hud.actionbar-protected`.
- **Tests:** `src/test/java/com/mcmiddleearth/pvp/mode/snowball/SnowballTest.java` (declarations, defaults, the time
  limit and the goal, ties, protection), `SnowballPaperTest.java` (kit, one-hit kills, the refill and the cap of
  16, chests, protection and its action bar), `platform/AttackEndsSpawnProtectionTest.java`,
  `platform/AmmoKeeperTest.java`, `platform/WeaponRulesTest.java`, `platform/SupplyChestsPerModeTest.java`,
  `platform/FrozenItemUseTest.java`, `ProductionMapsTest.java` (all 9 production maps play).
