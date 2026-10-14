# One In The Quiver

| | |
|---|---|
| Mode id | `oitq` (1.1.2 name: `OneInTheQuiver`) |
| Status | Playable in 3.0 |
| Production maps | 11 (`AS-OITQ`, `HO-OITQ`, `LT-OITQ`, `WE-OITQ`, …) |
| Players | 2 up to `maxPlayers`; free for all, no teams |

Everyone fights for themselves with an axe, a bow and a single arrow. A fully drawn arrow kills in one shot, so
every arrow counts: you get another one for each kill and each death, never more than five. The first player to
the score goal wins.

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

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S11.

- Leather helmet, chestplate, leggings and boots, dyed in your color.
- An iron axe in the first hotbar slot, a bow in the second and 1 arrow in the last (ninth) slot.
- Armor, axe and bow never break. There is no sword, no shield and no food.
- **Arrows.** You never carry more than 5. A kill, with the bow or the axe, gives 1 more arrow. After a death you
  come back with the arrows you had when you died, plus 1. A shot arrow is gone: it can't be picked up. Arrows
  stuck in a block disappear after 9 seconds.
- **Supply chests** never open during a game and give nothing.

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs 2 players. Players can't draw the bow until the fight starts, so nobody loses their arrow
  early.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S10, S14, S15, S24.

- **One-shot arrows.** A fully drawn bow shoots a critical arrow, the one that trails sparkles. A critical arrow
  kills any opponent in one hit. Any other arrow does normal bow damage. The axe does normal damage.
- **Score.** Every kill scores 1 point for the killer. The kill goes to the player Minecraft credits, so a fall
  right after an enemy's hit counts for that enemy. A death with no credited killer scores nothing.
- **Leaving under fire** (S24) gives the attacker a kill and a point, but no arrow (Not decided yet, item 1).
- There is no time limit.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11, S20.

- Everyone respawns at once, forever, at the next `PlayerSpawn` point in turn.
- After a respawn you have the full kit again, with the arrows the rule above gives.
- There is no spawn protection.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S26, S30, S31.

- The first player to reach the score goal (`scoreGoal`, default 21) wins at once.
- There is no other way to win. Leaving never ends the game (3.0 differs: S26), and a game with one player left goes
  on until staff stop it.
- The summary lists the highest K/D, the most kills and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S17, S21, S23, S24, S34.

- Players can always join a game that has started. A late joiner plays at once, at the next spawn in turn, with the
  kit and its single arrow.
- A player who played earlier in the game gets their own color back and keeps their kills.
- Leaving has no effect on the game.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`, for example
`/pvpadmin start LT-OITQ scoreGoal=15`. The defaults come from `config.yml` (`modes.oitq.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `scoreGoal` | 21 | 1 to 100 | Kills that win; it can't be 0, since there is no time limit. |
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play at once. Off: they watch. |

## Map requirements

- **Spawns:** points named `PlayerSpawn`, `PlayerSpawn1`, `PlayerSpawn2`, …, matched in any case: the unnumbered
  one first, then by number. Numbers may skip. At least one is needed.
- **The arena spawn** (`spawn`): where waiting players and spectators stand.
- **A region** keeps players in the arena. Without one, the game reserves its whole world.
- **Unassigned:** an arena without any `PlayerSpawn` point is set aside at startup with "missing points:
  PlayerSpawn"; one without an arena spawn with "the map has no spawn point".
- **Production maps:** 11, with 14 to 27 spawn points each. Six skip numbers: `AS-OITQ` (no `PlayerSpawn1`),
  `BH-OITQ` (no 16), `DAM-OITQ` (no 3 or 8), `HO-OITQ` (no 16), `OIE-OITQ` (no 1 or 16) and `RI-OITQ` (no 7, 9, 11,
  14, 16 or 17). `AS-OITQ` also carries a `RedBlock` point, which is ignored.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).
Texts are quoted from `messages.yml` without their color tags.

- **Sidebar.** The title is the mode's name (`hud.sidebar-title`), followed by "Waiting \<count>/\<max>"
  (`hud.sidebar-waiting`) or "Starting in \<seconds>s" (`hud.sidebar-countdown`). From the countdown on, it lists
  the five best players by score, each name in its color with the player's kills.
- **Boss bar.** "Starting in \<seconds>s" (`hud.bossbar-countdown`) during the countdown; none during the fight.
- **Action bar** during the fight: "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`). Spectators:
  "Spectating \<arena> · /pvp leave" (`hud.actionbar-spectating`).
- **Titles.** "\<seconds>" (`title.countdown`) for the last 5 seconds of the countdown, "Fight!" (`title.fight`) at
  the start, and "Victory" (`title.victory`), "Defeat" (`title.defeat`) or "Draw" (`title.draw`) at the end. A
  game stopped by staff or by a server stop shows no result title.
- **Sounds.** None.
- **Chat,** to the game's players and spectators only:
  - "The game starts in \<seconds> seconds!" (`game.countdown-started`);
  - "Game begins in \<seconds>" (`game.countdown-tick`) from 5 to 1, then "Game Start!" (`game.started`);
  - "Countdown cancelled: not enough players." (`game.countdown-cancelled`);
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
- **Rules:** `/pvp rules oitq` (or `oneinthequiver`) shows "\<mode> rules" (`rules.header`) and
  `mode.oitq.rules`: "Everyone for themselves, with an axe, a bow and one arrow. A fully drawn arrow kills in one
  shot. Every kill or death gives you another arrow, up to 5. The first to the score goal (21 kills by default)
  wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- **Result.** The winner gets a win, and every other player who took part a loss. A game stopped by staff or by a
  server stop is a draw for everyone. A player who left before the end and did not come back to play gets "left".
- **Kills and deaths:** every credited kill counts a kill, also one from leaving under fire; every death during the
  fight counts a death, whatever its cause.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S12, S15, S19, S21, S23, S24, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| One-shot arrows | Every arrow killed in one hit, however little the bow was drawn | Only a fully drawn (critical) arrow; any other does normal bow damage | As the rules always said: "if the bow is fully drawn" |
| Arrows for a kill | A kill left you with exactly one arrow | One more arrow per kill, with the bow or the axe, up to 5 | As the rules always said: up to 5 |
| Arrows after a death | Back to one | The arrows you had, plus one, up to 5 | As the rules always said |
| Score goal | Always 21 | The `scoreGoal` setting, default 21, 1 to 100 | Staff can choose a shorter or longer game |
| Rules text | "Everyone gets an axe, a bow, and one arrow, which kills in 1 shot if the bow is fully drawn." … "First to 21 kills wins." | See What players see | Says the goal is a default |
| Spawn numbers | A missing number left a hole in the spawn list: a player sent there hit an error (on `AS-OITQ` and `OIE-OITQ` the second player at the start, so those games failed to start), and the highest numbers were never used | Every `PlayerSpawn` point is used; numbers may skip | It was a bug, on 6 of the 11 maps |
| Wins and losses | Never recorded, not even for the winner | Won, lost or draw for every player; "left" for players who left before the end | Every mode records results the same way |
| Errors | A late joiner could hit an error at the end of the spawn list; a map missing `PlayerSpawn` failed at the start | Fixed; such a map is set aside at startup | They were bugs |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S17, S18,
S20, S26, S33.

1.1.2 paths are in its `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 paths are in
`src/main/java/com/mcmiddleearth/pvp/`.

1. **No arrow for a kill by leaving under fire.** 1.1.2 had no such kill: leaving never counted. 3.0 now: the
   attacker of a player who leaves under fire gets the kill and the point, but not the arrow a kill gives
   (`game/Combat.java:61-68`, `platform/AmmoKeeper.java:99-104`). Rule above: no arrow (3.0's, as no 1.1.2 rule
   exists). To decide: give the arrow too (change the code) or accept 3.0's (keep this document).

## For developers

- **Mode class:** `src/main/java/com/mcmiddleearth/pvp/mode/oitq/OneInTheQuiver.java`. Id `oitq`; legacy name
  `OneInTheQuiver`, matched ignoring spaces and case, so a map stored as `One In The Quiver` gets this mode too.
- **Declarations** (`OneInTheQuiver.java:28-40`):
  - layout: `noTeams()`, personal colors;
  - spawns: `SpawnPoints.numbered("PlayerSpawn")`;
  - kit: `LoadoutPolicy.fixed(Kits.OITQ)`;
  - respawn: `Respawn.AT_SPAWN`, no spawn protection;
  - score: `killScore(1)`;
  - late join: `MidgameJoin.JOIN`;
  - win: `WinConditions.firstTo(Setting.SCORE_GOAL)`;
  - weapons and supplies: `noChestRestock()`, `noArrowPickup()`, `oneShotArrows()`;
  - ammunition: `ARROWS = new Ammo("ARROW", 8, 5, 1, true, 1, 0, 0)` (`OneInTheQuiver.java:26`): slot 8, at most
    5, +1 per credited kill, kept on death plus 1, no timed refill.
- **Settings** derived from the declarations: `scoreGoal` (minimum 1 without a time limit,
  `ModeSettings.java:56-60`), `maxPlayers`, `midgameJoin`. Defaults: `config.yml`, `modes.oitq`.
- **ModeLogic hooks:** none; the mode is declarations only.
- **Engine and platform pieces it relies on:** `WeaponRules` (a critical arrow is lethal at hit time,
  `WeaponRules.java:68-89`; shot arrows get `PickupStatus.DISALLOWED`, `:59-65`), `AmmoKeeper` (the arrow count at
  death, kill gifts, the cap), `GameEventRouter` (no bow draw while frozen, `:261-293`), `SpawnCursor`,
  `TeamPlacement.nextPersonalColor`, `SummaryLines`.
- **Texts:** `mode.oitq.name`, `mode.oitq.rules`.
- **Tests:** `src/test/java/com/mcmiddleearth/pvp/mode/oitq/OneInTheQuiverTest.java` (declarations, goal, respawns,
  late joiners), `OneInTheQuiverPaperTest.java` (kit, one-shot and half-drawn arrows, arrows per kill and death,
  the cap of 5, no pickups, no chests), `platform/AmmoKeeperTest.java`, `platform/WeaponRulesTest.java`,
  `platform/FrozenItemUseTest.java`, `ProductionMapsTest.java` (all 11 production maps play).
