# Ringbearer

| | |
|---|---|
| Mode id | `ringbearer` (1.1.2 name: `Ringbearer`) |
| Status | Playable in 3.0 (not in a release yet) |
| Production maps | 13 (`AV-RB`, `BE-RB`, `DAH-RB`, `DOS-RB`, `GL-RB`, `HO-RB`, `ID-RB`, `OMP-RB`, `RI-RB`, `RV1-RB`, `SB2-RB`, `SR-RB`, `WE-RB`) |
| Players | One per team up to `maxPlayers` (25 to 100 on the production maps); two teams, Red and Blue |

Two teams, Red and Blue, and each has a Ringbearer who carries the One Ring. The Ring charges on the bearer's
experience bar and, once full, makes them invisible for 25 seconds. Your team respawns as long as its Ringbearer has
never died; after that, everyone on the team has one life left. The last team with players left wins.

The rules every mode shares are in the [README](README.md#rules-every-mode-shares) (S1 to S38). Each part below names
the ones that apply and describes only what is special to this mode.

## Rules

### Teams and spawns

Shared rules: [README](README.md#rules-every-mode-shares) S12, S18, S19.

- Two teams, Red and Blue. When the countdown starts, players are spread over the teams: the teams are even in size,
  and lifetime K/D is spread over them (highest K/D first, each to the team with fewer players; on equal sizes to the
  team with the lower K/D total, then to Red).
- Red players spawn on the arena's Red team spawn (`RedSpawn` in the 1.1.2 map), Blue players on the Blue one. An
  arena may list several spawns per team; players then take them in turn. Every production map has one per team.
- **The bearers.** When the countdown starts, one random player of each team becomes its Ringbearer.

### Kit

Shared rules: [README](README.md#rules-every-mode-shares) S5, S8, S9, S11.

- **Standard kit** (everyone but the bearers): leather helmet, chestplate, leggings and boots dyed in the team color
  (Red `#B02E26`, Blue `#3C44AA`), an iron sword in the first hotbar slot, a bow in the second and 24 arrows in the
  last. Armor, sword and bow never break. No enchantments, no food, no shield.
- **Ringbearer kit** (the bearers): the standard kit with a glowstone block on the head instead of the leather helmet,
  and the Ring in the third hotbar slot: a gold nugget named "The Ring", with the lore "The One Ring of power...".
- After every respawn a player has their full kit again, with 24 arrows. A player who becomes bearer during the game
  gets the Ringbearer kit at once.
- Supply chests restock 24 arrows (S9).

### Countdown and start

Shared rules: [README](README.md#rules-every-mode-shares) S3, S4, S5, S6, S7.

- The countdown needs one player per team. The teams are made, and the bearers are picked and told.

### Playing

Shared rules: [README](README.md#rules-every-mode-shares) S8, S9, S10, S13, S14, S15, S24.

**The Ring.**

- It charges while its bearer is visible: the experience bar fills from empty to full in 140 seconds and stops there.
  When it is full, the bearer reads "The Ring is ready" on the action bar, once per charge.
- The bearer uses it by right-clicking with the Ring in the main hand, in the air or on a block.
  - Not full yet (also while invisible): "The Ring isn't charged yet." on the action bar, and nothing else happens.
  - Full: for 25 seconds the bearer has Invisibility (with its particles) and Nausea. Their armor and off-hand item are
    taken off and kept aside, their hand moves to an empty hotbar slot, and the experience bar empties. They read
    "You are now invisible!" and "Don't hold anything in your hand, or you'll be seen!", and hear a sound.
  - After 25 seconds they read "You are no longer invisible!". The armor (with the glowstone) and the off-hand item
    come back as they were. Nothing else of the kit is given again, so arrows used meanwhile stay used. The hand moves
    to the bow (second hotbar slot; 3.0 differs: Not decided yet, item 2), and the charge starts again.
- The bearer stays in their team's colors throughout, so their teammates still see them faintly while they are
  invisible.

**Respawns and the bearer.**

- A team respawns as long as its bearer has never died.
- The bearer's first death closes the team's respawns. The game reads "Red Team's Ringbearer has been killed! They
  can't respawn!", both teams see a title, and everyone in the game hears thunder. The old bearer respawns once more,
  as an ordinary player with the standard kit (no Ring, no glowstone).
- If the other team still respawns, its bearer of that moment starts to glow: an outline seen through walls and
  through invisibility, until that bearer dies (3.0 differs: Not decided yet, item 1).
- From then on every member of the dead bearer's team, the old bearer included, is out at their next death.

**A bearer who leaves.**

- A bearer who leaves the game (logs out, `/pvp leave`, or a kick) while their team still respawns hands the role to
  a random teammate who is still fighting. The new bearer gets the Ringbearer kit at once, with an empty charge.
- **Leaving under fire** (S24): for a bearer this is their death. Their team's respawns close, as after any bearer's
  death, and nobody takes the Ring over.
- A bearer who leaves with no teammate left hands the Ring to nobody; their team is empty, so the other team wins.

**Team chat.** `/t <message>` works for players still in the fight; players who are out have none.

### Death and respawn

Shared rules: [README](README.md#rules-every-mode-shares) S11.

- While its team still respawns, a player respawns at once at their team spawn, with the full kit. There is no spawn
  protection.
- A player whose team no longer respawns is out at their death, also when the death is a leave under fire: they watch
  the rest of the game as a spectator from the arena's spawn point.
- Kills score nothing: this mode has no points.

### Winning and the end

Shared rules: [README](README.md#rules-every-mode-shares) S30, S31.

- The last team with players left wins. A team loses once none of its players is left in the fight: out after its
  respawns closed, or gone from the game. This is checked after every death and every leave.
- If the last players of both teams go at the same moment, the first one counted decides; if everyone left, it is a
  draw.
- There is no time limit and no score.
- The summary lists the highest K/D and the most deaths.

### Joining late, leaving and coming back

Shared rules: [README](README.md#rules-every-mode-shares) S1, S21, S22, S23, S24, S25, S34.

- While both teams still respawn, a newcomer joins the team with fewer players left (on equal teams: S22), gets the
  standard kit at their team spawn and plays at once. Once any bearer has died, newcomers can only watch.
- A player who played for a team earlier in the game goes back to that team, also after its respawns closed, unless
  they were out (S25).
- Leaving: see "A bearer who leaves" above.

### Spectators

Shared rules: [README](README.md#rules-every-mode-shares) S2, S27, S28, S29.

- Players who are out watch from the arena's spawn point until the game ends.

## Settings

Staff choose these in the Start game form or with `/pvpadmin start <arena> setting=value`. The defaults come from
`config.yml` (`modes.ringbearer.<setting>`).

| Setting | Default | Range | Meaning |
|---|---|---|---|
| `maxPlayers` | the arena's max players (16 when it has none) | 2 to the arena's max players (2 to 100 when it has none) | Most players in the game; spectators don't count. |
| `midgameJoin` | on | on or off | On: late joiners play by the rules above. Off: they watch. |

There is no time limit and no score goal.

## Map requirements

- **`spawn`**: the arena's spawn point. Waiting players and spectators stand there, and players who are out watch from
  there.
- **Team spawns** `red` and `blue` (`teamSpawns` in the arena file): at least one each. The import makes them from the
  1.1.2 map's `RedSpawn` and `BlueSpawn`; every production map has exactly these two points. A `RedSpawn` in `points`
  alone does nothing.
- **Region**: optional. Players and spectators can't leave it.
- The mode places no blocks.
- An arena is unassigned with "missing points: BlueSpawn, RedSpawn" (the names it lacks) when a team has no spawn, and
  with "the map has no spawn point" when it has no `spawn`. Its file keeps the mode, so it plays once fixed.

## What players see

The shared parts of the HUD and the texts are in S15 and S33 of the [README](README.md#rules-every-mode-shares).

**At the countdown**

- The bearer: `role.bearer.you` "You are \<team> Team's Bearer! Stay alive as long as you can!".
- The bearer's teammates: `role.bearer.teammates` "\<player> is your team's Ringbearer!".
- Everyone: `game.countdown-started` "The game starts in \<seconds> seconds!", the team line `join.team` "You're on
  team \<team>.", then `game.countdown-tick` "Game begins in \<seconds>" from 5 to 1, and `game.started` "Game Start!".
- Titles `title.countdown` "\<seconds>" for the last 5 seconds and `title.fight` "Fight!"; the boss bar
  `hud.bossbar-countdown` "Starting in \<seconds>s".

**The Ring** (to the bearer)

- The experience bar is the charge; the level number stays 0.
- Action bar: `ring.ready` "The Ring is ready", `ring.not-charged` "The Ring isn't charged yet.".
- Chat: `ring.invisible` "You are now invisible!" (with the sound `entity.illusioner.mirror_move`),
  `ring.invisible-hint` "Don't hold anything in your hand, or you'll be seen!", `ring.visible` "You are no longer
  invisible!".
- The item: `item.ring.name` "The Ring", `item.ring.lore` "The One Ring of power...".

**A bearer's death**

- Chat to everyone in the game: `role.bearer.lost` "\<team> Team's Ringbearer has been killed! They can't respawn!".
- A title to both teams: `hud.title-role-lost` "\<team> lost its \<role>!", with `role.bearer.name` "Ringbearer", so
  "Red lost its Ringbearer!".
- The sound `entity.lightning_bolt.thunder` for everyone in the game, and the glowing outline, in the team's color, on
  the other bearer.

**Deaths, joins and leaves**

- `game.death` "\<victim> was killed by \<killer>"; a death without a killer shows its usual death message. Both go to
  the game's players and spectators only.
- To a player who is out: `game.eliminated` "You're out! You're spectating until the game ends.", the title
  `title.eliminated` "You're out", and the action bar `hud.actionbar-out` "You're out · spectating".
- Leaving under fire: `game.fled` "\<player> left the fight and counts as killed by \<killer>.", or `game.fled-alone`
  "\<player> left the fight and counts as dead." when that player has left too.
- `game.player-joined-team` "\<player> joined the fight on \<team>.", `game.player-left` "\<player> left.".
- A late joiner who can't play: `join.midgame-spectating` "The game has already started." or `game.rejoin-eliminated`
  "You're out of this game, so you watch it.".

**HUD**

- Sidebar: the title `hud.sidebar-title` "\<mode>" ("Ringbearer"), then `Red` and `Blue` with the players each team
  has left.
- Action bar while the game runs: `hud.actionbar-score` "Kills \<kills> · Deaths \<deaths>", unless a notice is up.
- Supply chests: `restock.arrow.restocking` "Restocking arrows… \<seconds>" (3, 2, 1), then `restock.arrow.restocked`
  "Arrows restocked!". Leaving the region: `game.boundary` "You aren't allowed to leave the map!".

**The end**

- `summary.header` "Game over — \<mode> on \<arena>", `summary.team-won` "\<team> won!" (or `summary.draw` "It's a
  draw."), `summary.best-kd` "Highest K/D: \<entries>" and `summary.most-deaths` "Most deaths: \<entries>", up to
  three players each as `summary.entry` "\<player> (\<value>)".
- Titles `title.victory` "Victory", `title.defeat` "Defeat" or `title.draw` "Draw". The result stays for
  `endingSeconds` (10) before everyone goes to the lobby.

**Rules text** (`mode.ringbearer.rules`): "Two teams, each with a Ringbearer who carries the One Ring: charged on the
experience bar, it makes the bearer invisible for a while. A team respawns as long as its Ringbearer has never died;
after that, everyone on the team has one life left. The last team with players left wins."

## Stats

Shared rules: [README](README.md#rules-every-mode-shares) S32.

- Every member of the winning team gets a win and every member of the losing team a loss, players who were out
  included. A draw counts as neither. A player who left the game (and was not out) gets neither.
- A leave under fire is a death, and a kill for the player who hurt them.

## Changes from 1.1.2

The confirmed changes every mode shares are in the [README](README.md#rules-every-mode-shares): S1, S2, S3, S4, S5,
S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S36, S37. The rows below are this mode's own.

| What | 1.1.2 | Now | Why |
|---|---|---|---|
| The Ring's charge | +0.006 a second, usable at 84% of the bar (141 s); the bar kept filling and broke the bearer's timer at 167 s | Full after 140 s of being visible, usable when full, stops at full; "The Ring is ready" once per charge | The overflow broke the bearers' charge and glowstone every second |
| Using the Ring too soon | "You must have at least 1 xp level to use the ring" | "The Ring isn't charged yet." on the action bar | The Ring never used XP levels |
| After the invisibility | A whole new bearer kit (arrows refilled); the bearer lost their team colors until their first death | The armor and off-hand item come back as they were; the bearer keeps their team colors | Going invisible should not refill arrows; losing the colors was a bug |
| A bearer who died while invisible | The 25 s timer gave them the Ring back, and their charge went on | They never get the Ring back | A former bearer could go invisible again |
| A bearer who leaves | Blue's new bearer was drawn using Red's team size, so Blue could stay without a bearer; a bearer who used `/pvp spectate` kept their team respawning | Any leave hands the role to a random teammate still fighting, for both teams | Fair for both teams |
| A bearer who leaves under fire | Not a rule | Counts as the bearer's death: the team's respawns close, and nobody takes the Ring over | Logging out mid-fight must not save a bearer's team |
| Starting a game | One player alone made "Game Start!" repeat forever | At least one player per team to start the countdown | 1.1.2 hung with a single player |
| A team that empties by leaving | Noticed only at the next death when its last player used `/pvp spectate` | Loses at once | The game could hang |
| Making teams | Alternated players in join order (its K/D sort had no effect) | Even teams with lifetime K/D spread over them | 1.1.2 meant to balance by K/D |
| Picking the bearers | At "Game Start!" | When the teams are made, at the start of the countdown | Bearers know their role before the fight |
| A bearer's death | Two chat lines | One chat line and a title for both teams | Easier to notice |
| Sounds | None | A sound when the Ring hides its bearer, and thunder when a bearer dies | Easier to notice |
| The Ring's lore | "The One Ring of power..." and "1 of 2" | "The One Ring of power..." | Simplified; both the name and the lore are `messages.yml` texts |
| Wins and losses | Only living winners got a win; losers got nothing; the eliminated got a spectated game | Everyone on each team gets the team's win or loss, the eliminated included | Every player shares their team's result |
| Rules text | Three lines | The text above (`mode.ringbearer.rules`) | Explains the charge and the one life left |

## Not decided yet

Shared items not decided yet that apply here: [README](README.md#rules-every-mode-shares) S6, S7, S10, S11, S13, S18,
S22, S33.

Code references are relative to `src/main/java/com/mcmiddleearth/pvp/`.

1. **A later bearer also glows.** 1.1.2: when the first bearer died, only the other team's bearer of that moment got
   the glow; a teammate who took the Ring over after that bearer left did not glow. 3.0 now: once any bearer has died,
   every bearer of a team that still respawns glows, redrawn every second, also one who took the Ring over later
   (`mode/ringbearer/RingbearerLogic.java:111-115`, `mode/ringbearer/RingbearerLogic.java:165-167`). Rule above:
   1.1.2's. To decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).
2. **The hand after the Ring.** 1.1.2: when the invisibility ended, the bearer's hand moved to the bow (second hotbar
   slot). 3.0 now: the hand stays on the empty slot it was moved to (`mode/ringbearer/RingbearerLogic.java:151-158`).
   Rule above: 1.1.2's. To decide: keep 1.1.2's rule (change the code) or accept 3.0's (change this document).

## For developers

- **Classes:** `mode/ringbearer/Ringbearer.java` (the declarations) and `mode/ringbearer/RingbearerLogic.java` (the
  Ring, the respawn gates and the glow; one instance per game).
- **ModeRules:**
  - layout: `teams(TeamLayout.fixed(2)).balanced()` (Red, Blue; `TeamBalancer.assign`);
  - spawns: `SpawnPoints.perTeam()` (the arena's `teamSpawns`);
  - kit: `LoadoutPolicy.withRoleKit(LoadoutPolicy.fixed(Kits.STANDARD), Kits.RINGBEARER)`;
  - role: `teamRole(new TeamRole("bearer", true))`: one holder per team, picked at the countdown, handed on when the
    holder leaves;
  - respawn: `Respawn.AT_SPAWN`; the gate and the eliminations are `RingbearerLogic`'s;
  - late join: `MidgameJoin.JOIN`, with `newcomersMayPlay` false once a gate has closed;
  - win: `WinConditions.lastTeamStanding()`;
  - settings: `maxPlayers`, `midgameJoin`; no spawn protection;
  - extras: `sidebar(SidebarStyle.PLAYERS_LEFT)`; supply chests give the default `Restock.ARROWS`.
- **Hooks** (`RingbearerLogic`): `tick` (one second of charge per visible bearer, the ready notice, the glow),
  `onItemUse` (tag `ring`: hide or "not charged"), `onDeath` (a bearer's first death closes the gate and clears the
  role; a member of a closed team is eliminated), `onLeave` (forgets the Ring and cancels a pending return),
  `newcomersMayPlay`. Constants: `CHARGE_SECONDS = 140`, `INVISIBLE_TICKS = 500`, `RING_SOUND`.
- **Engine and platform pieces:** picking holders `game/TeamPlacement.java` (`pickRoleHolder`), handing on
  `game/Joins.java` (`handOnTeamRole`), the flee rule `game/Combat.java` (`leaving`, `FLEE_TICKS = 200`), the kit
  `mode/loadout/Kits.java` (`RINGBEARER`), the experience bar, effects, glow and hotbar
  `platform/PaperPlayerActions.java`, the stash `platform/EquipmentStash.java`, texts `platform/TextAnnouncer.java`
  (`teamRoleChanged`), the title `platform/ui/HudRenderer.java`, sounds `platform/GameSounds.java`.
- **Tests:** `mode/ringbearer/RingbearerTest.java` (rules, charge, gates, results), `RingbearerJoinsTest.java`
  (handing on, the flee rule, late joins), `RingbearerPaperTest.java` (kit, experience bar, invisibility, glow), and
  `game/TeamRoleTest.java` for the role per team.
