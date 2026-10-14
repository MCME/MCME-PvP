# Game modes

These documents describe how each game mode of MCME-PvP plays: its rules, settings, map points, texts and stats, and
how it differs from 1.1.2, the version players knew for years.

They are written for two readers:
- **Staff and designers**, who check a player's report ("the mode works differently now") against a mode's rules,
  and who change a rule by editing its document first.
- **Developers**, who implement, port or fix a mode from its document: each one says where the code lives, with file
  and line.

A document's **Rules** describe what the mode is meant to do. That is 1.1.2's behavior, unless a change from 1.1.2 was
confirmed on purpose. When 3.0 differs from 1.1.2 and the change was never confirmed, the Rules keep 1.1.2's rule, and
the difference is listed under **Not decided yet**. Those differences are the likely source of "the mode works
differently now" reports.

## The modes

| Mode | Id | 1.1.2 name | Status | Production maps | Not decided yet |
|---|---|---|---|---|---|
| [Team Deathmatch](tdm.md) | `tdm` | `TeamDeathmatch` | Playable | 35 | 2 |
| [Free For All](ffa.md) | `ffa` | `FreeForAll` | Playable | 16 | 3 |
| [Team Slayer](teamslayer.md) | `teamslayer` | `TeamSlayer` | Playable | 38 | 2 |
| [One In The Quiver](oitq.md) | `oitq` | `OneInTheQuiver` | Playable | 11 | 1 |
| [Snowball](snowball.md) | `snowball` | `Snowball` | Playable | 9 | 3 |
| [Infected](infected.md) | `infected` | `Infected` | Playable | 20 | 3 |
| [Death Run](deathrun.md) | `deathrun` | `DeathRun` | Playable | 5 (4 play) | 4 |
| [Ringbearer](ringbearer.md) | `ringbearer` | `Ringbearer` | Not in a release yet | 13 | 2 |
| [Capture the Flag](ctf.md) | `ctf` | `CaptureTheFlag` | Not in a release yet | 5 | 3 |
| [Egg Hunt](egghunt.md) | `egghunt` | `EggHunt` | Not in a release yet | 7 (once their eggs are listed) | 0 |
| [Team Conquest](teamconquest.md) | `teamconquest` | `TeamConquest` | Not ported | 9 | 20 |
| [Siege](siege.md) | `siege` | `Siege` | Not ported | 3 | 26 |
| [King of the Hill](kingofthehill.md) | `kingofthehill` | `KingOfTheHill` | Unused | 0 | 20 |

- **Playable:** ported to 3.0 and in its development builds.
- **Not in a release yet:** ported to 3.0 on the development branch, but in no released build yet.
- **Not ported:** the mode has production maps but no 3.0 code yet. Its maps are imported as unassigned arenas. Its
  document describes 1.1.2's mode, so that a port can start from it.
- **Unused:** 1.1.2 has code for the mode, but no map uses it.
- **Not decided yet** counts the items in the mode's own document. The shared items of
  [Rules every mode shares](#rules-every-mode-shares) come on top; each document names the ones that apply to it.

## How to use the documents

- **To change a mode,** edit its Rules first, then the code. For a rule every mode shares, edit it here first. Code
  that does something other than what its document says is a bug.
- **To check a player report,** read the mode's Rules, its "Not decided yet", and the shared rules below. A report that
  matches an item under "Not decided yet" is a known difference between 3.0 and 1.1.2, not a new bug.
- **To settle an item under "Not decided yet",** choose one of two ways:
  - keep 1.1.2's rule: change the code to follow the Rules, and remove the item;
  - accept 3.0's rule: write it into the Rules, add a row to "Changes from 1.1.2", and remove the item.

  Then renumber the remaining items and update the count in the table above. A shared item below is settled the same
  way: change its status here, and the "Shared items not decided yet" line of each mode document.
- **Every document has the same parts:** a short description, then Rules (teams and spawns, kit, countdown and start,
  playing, death and respawn, winning and the end, joining late and leaving, spectators), Settings, Map requirements,
  What players see, Stats, Changes from 1.1.2, Not decided yet, and For developers.
- **Paths.** 3.0's code is under `src/main/java/com/mcmiddleearth/pvp/`, and its texts are in
  `src/main/resources/messages.yml`. 1.1.2's code was under `src/main/java/com/mcmiddleearth/mcme/pvp/`; 3.0 removed
  it, and the 1.1.2 release keeps it.

## Rules every mode shares

These rules come from the engine, so they are the same in every mode. A mode document names the ones that apply to it
("Shared rules: S4, S5, S6") and describes only what is special to the mode. They also apply to a mode that isn't
ported yet: its port follows them.

Each shared rule has one of four statuses:
- **Confirmed change:** 3.0 changed 1.1.2's rule on purpose. 3.0's rule is the rule.
- **Same as 1.1.2.**
- **Not back yet:** 1.1.2's rule stays the rule. 3.0 doesn't have it yet, and it is to come back before 3.0.0.
- **Not decided yet:** 3.0 differs from 1.1.2, and the change was never confirmed. The rule is 1.1.2's. To decide:
  keep 1.1.2's rule (change the code) or accept 3.0's (change this page).

| Status | Shared rules |
|---|---|
| Confirmed change | S1, S2, S3, S4, S5, S9, S12, S15, S19, S21, S23, S24, S25, S27, S30, S31, S32, S34, S35, S36, S37, S38 |
| Same as 1.1.2 | S8 |
| Not back yet | S14, S28, S29 |
| Not decided yet | S6, S7, S10, S11, S13, S16, S17, S18, S20, S22, S26, S33 |

### Opening, joining and the countdown

**S1. Opening a game and joining it.** Confirmed change.
- Now: staff open a game on an arena (`/pvpadmin start`, or Start a game in `/pvp`), and players join it themselves:
  from `/pvp`, with the [Join] buttons under the "game opened" line and in `/pvp games`, or with `/pvp join [arena]`.
  Nobody is put into a game. A player who arrives on the server sees the list of open games. Several games run at once
  when their arenas don't overlap; an arena without a region takes its whole world.
- 1.1.2: one game at a time. `/pvp game quickstart` joined every online player, a player who logged in while the game
  waited joined it, and one who logged in during a game became a spectator with a "Click to join" offer.

**S2. Only the game sees the game.** Confirmed change.
- Now: a game's chat lines (joins, leaves, deaths, the countdown, time warnings, the result), its sidebar and its boss
  bar reach only its players and spectators. Players outside the game are left alone: nobody becomes a spectator
  because a game started.
- 1.1.2: every online player who didn't play became a spectator when the countdown started (King of the Hill's
  code aside), and the game's messages, death messages included, went to everyone on the server.

**S3. Waiting for the countdown.** Confirmed change.
- Now: a player who joins a waiting game goes to the arena's spawn point, with an empty inventory, healed, fed and in
  Adventure mode.
- 1.1.2: players waited wherever they were.

**S4. Players needed to start.** Confirmed change.
- Now: one player per team in modes with teams or sides, and 2 in modes without teams. With fewer, the countdown is
  refused: "At least \<min> players are needed to start; \<count> joined." (`error.not-enough-players`). A countdown
  that falls below that number, or leaves a team or side without players, stops, and the game waits again: "Countdown
  cancelled: not enough players." (`game.countdown-cancelled`) (`mode/ModeRules.java:258-260`,
  `game/Lifecycle.java:86-91`).
- 1.1.2: one player was enough to start any mode, and a team emptied during the countdown didn't stop it (in some
  modes the other team won at once).

**S5. The countdown.** Confirmed change.
- Now: the countdown lasts `countdownSeconds` (7 by default). When it starts, the teams (or sides and roles) are made,
  every player is placed on a spawn, gets the kit (and in a mode without teams their personal color) and is frozen:
  they can look around but not move, and can't use the kit (no bow drawn, nothing thrown) until the fight starts.
  Chat reads "The game starts in \<seconds> seconds!" (`game.countdown-started`), then "Game begins in \<seconds>"
  (`game.countdown-tick`) from 5 to 1, one line a second from 2 seconds in, then "Game Start!" (`game.started`).
  Titles show the last 5 seconds (`title.countdown`) and "Fight!" (`title.fight`), the boss bar reads "Starting in
  \<seconds>s" (`hud.bossbar-countdown`), and the sidebar shows from the countdown on. Nobody can be hurt during the
  countdown (for the void, see S7).
- 1.1.2: the same 7 seconds and countdown lines, but the kit (and the personal color) came at "Game Start!", and
  there were no titles and no boss bar; the sidebar appeared at "Game Start!".

**S6. The freeze.** Not decided yet.
- 1.1.2: frozen players were lifted 0.1 block and held in flight at speed 0, with Invisibility (level 16), so the
  other team couldn't see them. A player who joined during the countdown was not frozen and got the kit at once
  (`Gamemode/BasePluginGamemode.java:204-222`).
- Now: frozen players stand on their spawn, visible. A player who joins during the countdown is placed on a team and a
  spawn and frozen with the others (`platform/GameEventRouter.java:212-231`, `game/Game.java:247-251`).
- Rule: 1.1.2's.

**S7. The void and `/kill` before and after the fight.** Not decided yet.
- 1.1.2: while a game waited or counted down, and once it had ended, every damage to a player was cancelled, the void
  included, except lava and burning with `lavaDamage` on (`Handlers/AllGameHandlers.java:170-187`).
- Now: the void and `/kill` always get through, also while the game waits, counts down or shows its result. Other
  damage is blocked as in 1.1.2 (`platform/DamageRules.java:67-78`).
- Rule: 1.1.2's.

### Playing

**S8. Fighting basics.** Same as 1.1.2.
- Teammates can't hurt each other, and nobody's own arrow hurts them. Contact damage (cactus, berry bushes) is off;
  falls, lava, fire, drowning and the void hurt as usual. Food stays full.
- Nobody in a game can leave the arena's region: a move out ends where the player last stood inside, with "You aren't
  allowed to leave the map!" (`game.boundary`) at most every 3 seconds.
- Players can't drop items, move them in the inventory, or swap hands.
- A shot arrow can be picked up while the player carries fewer than 24 arrows, unless the mode says otherwise. An
  arrow stuck in a block disappears after 9 seconds.

**S9. Supply chests.** Confirmed change.
- Now: a chest never opens during a game; right-clicking one while the fight runs restocks the player. By default a
  player with fewer than 24 arrows gets 24 arrows in the last hotbar slot (each mode says what it gives), at once, and
  Slowness VI for 3 seconds. The action bar counts "Restocking arrows… 3", "2", "1" (`restock.arrow.restocking`), and
  when the slowness ends shows "Arrows restocked!" (`restock.arrow.restocked`) for 1 second. Copper chests work like
  chests.
- 1.1.2: the same restock, but the action bar read "Restocking Supplies... 3", "2", then "Restocked!" after 2 seconds;
  copper chests didn't exist yet.

**S10. Blocks, riding and target blocks.** Not decided yet.
- 1.1.2: during a game, beacons, plain anvils, chests, furnaces, trapped chests, crafting tables and undyed shulker
  boxes couldn't be used, and an arrow hitting a target block was cancelled, so it never powered it
  (`Handlers/AllGameHandlers.java:245-247`, `Handlers/ArrowHandler.java:51-58`).
- Now: chipped and damaged anvils, shulker boxes of every color, decorated pots and shelves are blocked too; players
  in a game can't ride anything, change armor stands or item frames, or swap gear with them; an arrow powers a target
  block as in vanilla, which matters on courses that use target blocks (`platform/GameEventRouter.java:75-76`,
  `:318-339`, `:363-368`, `:448-451`).
- Rule: 1.1.2's.

**S11. Death drops, experience and pickups.** Not decided yet.
- 1.1.2: nothing handled what a dying player dropped. Whether the kit dropped depended on the server's
  `keepInventory` game rule, which is unknown for the production server. Experience orbs could be collected, and any
  item could be picked up; only shot arrows had a rule (S8) (`Handlers/ArrowHandler.java:68-72`).
- Now: a dying player drops no items and no experience, and players in a game pick up nothing but shot arrows
  (`platform/GameEventRouter.java:103-104`, `:341-359`).
- Rule: 1.1.2's for experience and pickups.

**S12. Name tags and colors.** Confirmed change.
- Now: each game has its own scoreboard. Name tags and the tab list show the team or side color, or in a mode without
  teams the player's personal color.
- 1.1.2: the tab list and chat showed the colors, but name tags were not colored on the production server.

**S13. Teammates pass through each other.** Not decided yet.
- 1.1.2: during the fight the teams kept Minecraft's default, so teammates pushed each other; only frozen players
  were kept from colliding (`Gamemode/BasePluginGamemode.java:71`, `:204-222`, `:288-290`).
- Now: teammates don't collide, and players of different teams do; in modes without teams everyone collides
  (`platform/GameScoreboard.java:76-80`).
- Rule: 1.1.2's.

**S14. Arrows tinted in the shooter's color.** Not back yet.
- 1.1.2: every arrow a player shot took their team or personal color, so it left a colored trail
  (`Handlers/ArrowHandler.java:84`, colors in `Handlers/BukkitTeamHandler.java:215-229`).
- Now: arrows are not tinted.
- Rule: 1.1.2's.

**S15. The HUD.** Confirmed change.
- Now: the sidebar, on the game's own scoreboard, is titled with the mode's name (`hud.sidebar-title`), followed by
  "Waiting \<count>/\<max>" (`hud.sidebar-waiting`) while the game waits, "Starting in \<seconds>s"
  (`hud.sidebar-countdown`) during the countdown, and "Time \<time>" (`hud.sidebar-time`) in a mode with a time limit.
  Its lines are the mode's; each document lists them. The boss bar shows the countdown, then the time left, "\<mode> ·
  \<time> left" (`hud.bossbar-time`). The action bar shows "Kills \<kills> · Deaths \<deaths>" (`hud.actionbar-score`)
  unless the mode or a restock shows something else; spectators read "Spectating \<arena> · /pvp leave"
  (`hud.actionbar-spectating`), and a player who is out "You're out · spectating" (`hud.actionbar-out`). Titles: the
  countdown, "Fight!", "You're out" (`title.eliminated`) for a player who is out, and the result (S30).
- 1.1.2: chat lines and a sidebar only. Each mode had its own sidebar, often with the time as its title ("Time: 9m
  59s").

### Teams, colors and spawns

**S16. Team balancing.** Not decided yet. It applies to Team Deathmatch, Team Slayer and Capture the Flag. (In
Ringbearer, balancing by K/D is a confirmed change: 1.1.2's code dealt its players in join order.)
- 1.1.2: when the countdown started, players were sorted from the lowest lifetime K/D up, with a random amount between
  −0.3 and +0.3 added to each K/D in every comparison, then dealt out Red, Blue, Red, … The lowest K/D went to Red,
  Red got the odd player, and the same players got different teams from game to game
  (`Gamemode/BasePluginGamemode.java:149-158`).
- Now: no randomness. The highest K/D is placed first; each player (a party stays together) goes to the team with the
  fewest players, a tie to the team with the lower K/D total, then to the earlier team in color order. The same
  players always get the same teams, and the odd player can land on any team (`mode/rules/TeamBalancer.java:20-47`,
  `game/TeamPlacement.java:75-87`).
- Rule: 1.1.2's. For 3 and 4 teams 1.1.2 had no rule.

**S17. Personal colors.** Not decided yet. It applies to the modes without teams.
- 1.1.2: 11 colors handed out in join order: Aqua, Blue, Dark Aqua, Dark Green, Dark Purple, Dark Red, Gold, Green,
  Light Purple, Red, Yellow; the 12th player got Aqua again. Dark Red players wore the same red armor as Red players
  (`#B02E26`), and Gold players light gray (`#9D9D97`). A first-time late joiner got a random one of the 11, which
  could be someone else's (`Handlers/GearHandler.java:84-89`, `Gamemode/FreeForAll.java:317-318`).
- Now: the same 11 colors in the same order. Dark Red armor is `#8E1B1B` and Gold armor orange `#F9801D`, and a late
  joiner gets the next color in the rotation (`team/Palette.java:26-37`, `game/TeamPlacement.java:134-139`). Both
  give a returning player their color back.
- Rule: 1.1.2's.

**S18. Where players stand.** Not decided yet.
- 1.1.2: points were whole block positions, so players stood on the corner of a block (x.0, z.0). Most spawns put
  players on the block the point was stored on; some modes put them a block higher, or into that block, and
  spectators went to or above the arena's spawn (`Util/EventLocation.java:47-49`).
- Now: every point puts players in the centre of the block (x.5, z.5), standing on the block the point was stored on,
  and the arena's spawn at its own height (`arena/NamedPoint.java:10-18`). Both face south.
- Rule: 1.1.2's.

**S19. Map points.** Confirmed change.
- Now: each mode knows the points it needs. When the plugin starts, an arena that lacks one is left unassigned (not
  playable), with the reason, such as "missing points: BlueSpawn, RedSpawn"; its file keeps its mode, so it plays once
  the points are added. Numbered points (`PlayerSpawn`, `PlayerSpawn1`, `PlayerSpawn2`, …; `RedSpawn1`, …) all count:
  the unnumbered one first, then by number, and numbers may skip. Names match in any case. Imported points have no
  facing, so players face south.
- 1.1.2: a map that lacked a point failed when its game started. In Free For All and One In The Quiver a missing
  number left a hole in the spawn list, and a player sent there hit an error; the highest numbers were never used.

**S20. The order of numbered spawns.** Not decided yet. It applies to Free For All, One In The Quiver and Snowball.
- 1.1.2: the start placed players on the spawns in turn from `PlayerSpawn`. Respawns and late joiners used a second
  turn, which began at a random spawn (never the last one) when the fight started (`Gamemode/FreeForAll.java:215`,
  `:389-393`).
- Now: one turn for the countdown, respawns and late joiners, from `PlayerSpawn`, starting over after the last; so
  the first respawns go to the spawns after the last one used at the start (`game/SpawnCursor.java:27-36`).
- Rule: 1.1.2's random start of the respawn turn.

### Joining late and leaving

**S21. Joining late.** Confirmed change.
- Now: a player who joins after the countdown started follows the mode's late-join rule (each document gives it).
  Staff can switch late joining off for a game (`midgameJoin`); late joiners then watch: "The game has already
  started." (`join.midgame-spectating`). A full game (`maxPlayers` players) takes them as spectators: "The game is
  full." (`join.full-spectating`). The [Join] buttons in chat and in the menus follow the same rule.
- 1.1.2: each mode had its own rule, always on. Team Deathmatch offered "Click to join" at 75% of a team's starting
  size but allowed the join only at 100%.

**S22. Late joiners on equal teams.** Not decided yet. It applies to Team Deathmatch, Team Slayer (when no team trails
by 10 points), Ringbearer and Capture the Flag.
- 1.1.2: with equal teams, a late joiner went to Blue (at the start, ties went to Red).
- Now: to Red, the first team in color order (`game/TeamPlacement.java:115-118`, `:124-133`).
- Rule: 1.1.2's.

**S23. Leaving.** Confirmed change.
- Now: `/pvp leave`, `/pvp lobby`, Leave game in the `/pvp` menu, and logging out take a player out of the game and to
  the PvP lobby, with an empty inventory. They read "You left the game." (`leave.left`), and the game reads "\<player>
  left." (`game.player-left`). A player who left and comes back keeps the kills, deaths and score they had in this
  game. A staff kick (`/pvpadmin kick`) sends the player to the main server and never counts as a death.
- 1.1.2: `/pvp leave` sent the player to the main server.

**S24. Leaving under fire.** Confirmed change.
- Now: a player who leaves a running game, or logs out, within 10 seconds (200 ticks) of being hurt by another player
  of the game counts as killed by that player: a death for the leaver, and a kill, with the mode's kill score, for the
  attacker, as long as the attacker still plays in the game and is not a teammate. Chat: "\<player> left the fight and
  counts as killed by \<killer>." (`game.fled`), or without such an attacker "\<player> left the fight and counts as
  dead." (`game.fled-alone`). In a mode where the dead don't respawn, the leaver is out. The death counts before the
  leave, so it can end the game. A later hit replaces an earlier one, and a death or a respawn clears it. A staff
  kick, the kick-all and lock sweeps, and a game closing never count (`game/Combat.java:14`, `:61-68`).
- 1.1.2: leaving or logging out never counted.

**S25. Out for good.** Confirmed change.
- Now: a player who is out (eliminated, or in Death Run finished) and joins the same game again only watches: "You're
  out of this game, so you watch it." (`game.rejoin-eliminated`). They keep their team's result (S32).
- 1.1.2: a player who was out could leave and join again with a fresh life.

**S26. A game everyone left.** Not decided yet. It applies to the modes without teams: Free For All, One In The
Quiver, Snowball and Egg Hunt. (In modes with teams, a team without players loses at once.)
- 1.1.2: leaving never ended these modes. A game ran until the time ran out, or in One In The Quiver until staff ended
  it, also with nobody in it.
- Now: once every player has left, the game ends at once as a draw: "Everyone left, so the game on \<arena> ended."
  (`game.abandoned`). Spectators don't keep it going (`game/Lifecycle.java:92-96`).
- Rule: 1.1.2's.

### Watching

**S27. Watching a game.** Confirmed change.
- Now: anyone may watch a game that waits, counts down or runs: with `/pvp spectate <arena>`, [Spectate], or Spectate
  in `/pvp`. Spectators, and players who are out, are in Spectator mode at the arena's spawn point (unless the mode
  says otherwise). They can't leave the region, can't hurt or be hurt, and see the game's chat, sidebar and boss bar.
  `/pvp leave` ends watching.
- 1.1.2: everyone online who didn't play watched the one game (S2).

**S28. Spectator-only chat.** Not back yet.
- 1.1.2: during a game, a spectator's chat (a dead player's too) went only to the other spectators, as "Spectator
  \<name>: \<message>", so they couldn't call out positions; staff were exempt
  (`Gamemode/anticheat/AntiCheatListeners.java:153-170`).
- Now: spectators use the server's normal chat.
- Rule: 1.1.2's.

**S29. Commands blocked during a game.** Not back yet.
- 1.1.2: while a game ran, `/fill`, `/blockdata`, `/clear`, `/gamemode`, `/setblock`, `/xp`, `/worldjump`, `/mv` and
  `/mvtp` were refused with "You can't do that during a game!", `/tp` was refused for anyone but spectators, and
  spectators couldn't use `/me`, `/tell`, `/msg`, `/tellraw`, `/title`, `/w` or `/r`
  (`Gamemode/anticheat/AntiCheatListeners.java:111-149`).
- Now: no command is blocked; only permissions decide.
- Rule: 1.1.2's.

### The end, results and texts

**S30. The end of a game.** Confirmed change.
- Now: every player still in the game sees a title, "Victory", "Defeat" or "Draw" (`title.victory`, `title.defeat`,
  `title.draw`), and chat shows the summary: "Game over — \<mode> on \<arena>" (`summary.header`), the result, then the
  mode's lists (each document names them), each with the top 3 on one line as "\<player> (\<value>)", everyone who took
  part included and ties by name, then [Games] and [My stats] buttons. K/D is kills divided by deaths, rounded to 2
  decimals, or the kills when there are no deaths. The result stays for `endingSeconds` (10 by default); players may
  join or watch another game meanwhile. Then everyone goes to the PvP lobby with an empty inventory, no effects and no
  experience, healed, fed and in Adventure mode.
- 1.1.2: each mode wrote its own end lines to everyone online, often "Game over!" and "\<Team> Team Wins!" with lists
  of one name per line, and everyone went to the lobby at once.

**S31. A game stopped by staff or by a server stop.** Confirmed change.
- Now: `/pvpadmin stop <arena>` ends a game as a draw: everyone who played gets a played game, no win and no loss, and
  there is no result title. Chat reads "The game on \<arena> was stopped by staff." (`game.stopped`), or for a server
  stop "The game on \<arena> ended because the server is stopping." (`game.shutdown`).
- 1.1.2: `/pvp game end` told everyone online "\<mode> on \<map> was ended by a staff!"; what it recorded varied by mode.

**S32. Results and stats.** Confirmed change.
- Now, in every mode: when a game ends, each player who took part gets the result of their team or side (in a mode
  without teams, their own): won, lost or draw. A player who is out keeps their team's result, also after leaving or
  logging out. A player who left before the end while not out, and didn't come back to play, gets "left": a played
  game, no win, no loss. A spectator who watched to the end without playing gets a spectated game. A game closed
  before its fight started counts for nobody. Lifetime kills and deaths are added when the game ends, from the game's
  own counts: deaths during the fight, and kills credited to a player of the game who is not on the victim's team.
  1.1.2's list of the players each player killed is kept, but no longer grows. Every finished game also gets a match
  record in `matches/<yyyy-MM>.jsonl` (`game/MatchRecords.java:41-56`, `stats/StatsRecorder.java:28-54`).
- 1.1.2: each mode recorded wins and losses its own way, at the moment of the win, and some recorded none; kills and
  deaths were added at once, also during the countdown, for any killer.

**S33. Texts.** Not decided yet.
3.0 rewords the texts every mode shares. Those set by a confirmed change (leaving under fire, coming back after being
out, the restock, a stop and a server stop) are confirmed. For the others, 1.1.2 → 3.0:
- "You are on the Red Team!" → "You're on team Red." (`join.team`).
- "\<player> has joined the fight on Red Team!" (and "… has joined the fight!", "… as a Infected!") → "\<player> joined
  the fight on \<team>." (`game.player-joined-team`) once the countdown has started, or "\<player> joined
  (\<count>/\<max>)." (`game.player-joined`) while the game waits and in modes without teams; the joiner reads "You
  joined \<arena> (\<mode>)." (`join.joined`).
- "\<player> left the fight!" (to everyone) and "\<player> left" → "\<player> left." (`game.player-left`).
- "You are Spectating!" and "As a spectator, game participants won't see your chat." → "You're spectating \<arena>."
  (`join.spectating`), or for a player who is out "You're out! You're spectating until the game ends."
  (`game.eliminated`).
- "Can't join \<mode> midgame!" → "The game has already started." (`join.midgame-spectating`), and the player watches.
- "\<victim> was killed by \<killer>" keeps its words (`game.death`), with the names in their colors; other deaths keep
  Minecraft's own message (S2 sets who reads them).
- Team chat "[TEAM] \<name>" with the message in red or aqua → "[TEAM] \<player>: \<message>" (`team-chat.format`) in
  the team color.
- Gone: "Use /unstuck if you're stuck in a block!" (no such command existed).
- The countdown lines, "Game Start!" and the time warnings ("30 seconds remaining!", "10 seconds remaining!" … "1
  second remaining!") keep 1.1.2's words; "The game starts in \<seconds> seconds!" is new (S5).
- Rule: the texts are 3.0's, listed here so that the wording can be checked. To decide: keep 1.1.2's wording (change
  `messages.yml`) or accept 3.0's.

### Other shared rules

**S34. The lobby.** Confirmed change.
- Now, with `lobbyProtection` on (the default): players outside games take no damage except from the void, `/kill`,
  and lava or fire when `lavaDamage` is on; they can't hurt players or mobs (staff in Creative mode can still hit
  mobs and break decorations); they don't get hungry; and they respawn at the PvP lobby. Leaving a game clears
  experience and levels.
- 1.1.2: all damage was blocked in the lobby, the void included, and levels from kills piled up across games.

**S35. Blocks a game changes go back.** Confirmed change.
- Now: every block a game places or removes (Death Run's beacon, Capture the Flag's bases, Egg Hunt's eggs) goes back
  to what it was when the game closes, after the result; after a crash, at the next start. A game doesn't open, and
  its first countdown doesn't start, where a flag base or an egg would replace a block that holds data (a sign, a
  container, a banner, a head, a spawner).
- 1.1.2: nothing was put back: beacons, iron bases and emptied blocks stayed, and collected eggs were gone for good.

**S36. Settings.** Confirmed change.
- Now: staff set a game's settings in the Start game form or with `/pvpadmin start <arena> setting=value`; the
  defaults are in `config.yml` under `modes.<mode id>`. `timeLimit` is in minutes, 1 to 60. `scoreGoal` is 0 to 100,
  where 0 means no goal, and 1 to 100 in a mode without a time limit. `maxPlayers` is 2 to the arena's max players (2
  to 100 when it has none) and at least one per team; its default is the arena's max players (16 when it has none).
  `midgameJoin` is on or off (S21). Each document lists its mode's settings and defaults.
- 1.1.2: staff typed one number with `/pvp game quickstart <map> <number>` (minutes, or the score that wins, by mode),
  with no default and no limit; the most players was the map's `max`.

**S37. Rules texts.** Confirmed change.
- Now: `/pvp rules <mode>` accepts a mode id or a 1.1.2 name, such as `teamdeathmatch` or `oneinthequiver`, and
  every mode has its own rules text (`mode.<id>.rules`).
- 1.1.2: `/pvp rules` left out some modes, and some texts ran on into other modes' texts.

**S38. Waiting to respawn.** Confirmed change. It applies to modes with a respawn delay: Capture the Flag (and Siege,
once ported).
- Now: a player who dies waits the mode's delay in Spectator mode where they died (at the arena's spawn when they
  died outside the region), and their action bar counts "Respawning in \<seconds>" (`hud.actionbar-respawning`). They
  stay on their team: a team whose players all wait has not lost, and they get their team's result. They can't hurt
  or be hurt, and can't use team chat until they are back. Leaving and coming back doesn't skip the wait. When the
  wait is over they respawn at their team's spawn with the kit. The waits end with the game.
- 1.1.2: each mode had its own respawn queue; the dead waited as spectators at the arena's spawn and left their team
  meanwhile.
