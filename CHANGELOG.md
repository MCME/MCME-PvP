# Changelog

All notable changes to MCME-PvP are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

3.0 replaces the one-game core with a new engine that runs several games at once. Until 3.0.0 there are only
3.0 development builds; 3.0.0 is released once the other game modes are ported.

### Added

- Several games at the same time. An arena is reserved by the space of its region, so arenas that share a
  place (such as the AS maps) block each other while a game runs; an arena without a region reserves its world.
- Team Deathmatch with 2 to 4 teams (one spawn set per team on the arena), and Free For All with a time limit
  in minutes and an optional score goal. Joining after the start follows each mode's rule and can be switched
  off per game.
- `messages.yml`, new in 3.0, holds the player-facing texts, in MiniMessage (team names and the chat-prefix
  labels stay built in); a key missing from it falls back to the built-in text.
- Menus: `/pvp` opens a main menu (join, spectate, rules, stats, lobby); staff start games from a form with
  sliders and a toggle, and confirm Stop and Kick all. They are Paper dialogs, which need a 1.21.6 or newer
  client; every button also has a command.
- Clickable chat: `/pvp games` with [Join] [Spectate] (and [Countdown] [Stop] for staff), buttons under the
  "game opened" announcement and under the end-of-game summary, and a list of the open games for players who
  arrive. Buttons check the permission and the game again when clicked, and expire after 10 minutes.
- While a finished game shows its result, its players can join or spectate another game at once; they leave
  the finished game without a leave message.
- A HUD for each game: a sidebar with each team's players left or the top five scores, a boss bar for
  the countdown and the time limit, kills and deaths on the action bar, and titles for the countdown,
  the start, an elimination and the result. A game that staff stop, or that ends because the server stops,
  shows no result title; chat says which of the two ended it.
- A match record for every finished game in `matches/<yyyy-MM>.jsonl`.
- `lobbyProtection` (on by default) keeps 1.1.2's protected lobby for players outside games.
- Copper chests work as supply points, like chests.
- Staff actions taken from the menus (opening a game, starting a countdown, stopping a game, kicking all
  players) are written to the server log.

### Changed

- Nobody is put into a game automatically any more. 2.0.0 joined every online player when a game opened,
  joined a player who logged in while a game waited, and made a player who logged in during a game a
  spectator; 3.0 announces an opened game with [Join] buttons in chat instead, and shows players who
  arrive the list of open games.
- Arenas are stored as `arenas/<id>.json`. The 1.1.2 files in `maps/` are imported once, including the
  older formats 1.1.2 could not load, and are never written to; files that are clearly not maps, such as
  `desktop.ini`, `Thumbs.db`, `*.bak`, `*~` or `AS-TDM - Copy`, are skipped, and the log names them. A
  map whose mode 3.0 does not have yet is listed as unassigned.
- Player stats move to `players/<uuid>.json`, imported from `stats/` at a player's first finished or watched
  game; `stats/` is never written to. Before that, `/pvp stats` still reads the 1.1.2 file. Games won and
  lost now count in every mode.
- Commands use Paper's Brigadier API: `/pvp` (join, leave, spectate, games, rules, stats, lobby), `/pvpadmin`
  (start with `setting=value` pairs, countdown, stop, kick, kickall, lock, unlock, arena list, info and import)
  and `/t` for team chat. The 1.1.2 permission nodes still work as parents of the new ones. New permission
  nodes: `pvp.play` (everyone by default) and, for operators by default, `pvp.join.locked`, `pvp.staff.run`,
  `pvp.staff.kick`, `pvp.staff.arena` and `pvp.admin`, which grants all the others.
- `/pvpadmin kickall` needs `pvp.staff.run`, as 1.1.2's `/locker kickall` needed `pvp.gameCanRun`;
  moderators (`pvp.gameKick`) kick one player at a time. `pvp.mapCreate`, which 1.1.2 declared but
  never checked, now grants `pvp.staff.arena`.
- `/pvpadmin start` checks its settings before a game opens and says what is wrong: an unknown or repeated
  setting, a word without `=value` (such as `timeLimit`), an invalid or out-of-range value, or a max players
  too small to fit one player on every team.
- `config.yml` gets `configVersion: 1`, a configurable `lobby`, `mainServer` (where kick, kickall and lock
  send players), `startLocked` (PvP starts locked after a restart, as in 1.1.2), `countdownSeconds`,
  `endingSeconds` and default settings per mode; a 1.1.2 file is migrated on the first start. `lavaDamage`
  keeps its 1.1.2 meaning: when it is on, lava and burning also hurt players in the lobby and while their game
  waits, counts down or ends. 1.1.2's `noHunger` and `PVP.*` had no effect and are dropped.
- A `config.yml` migrated from 1.1.2 or 2.0.0 has the new keys but none of the explanations the shipped
  file has; read them in the default `config.yml` in the jar, or in the one a fresh install writes.
- `/pvp leave` returns to the PvP lobby instead of the main server.
- `/pvp lobby` takes you to the PvP lobby, leaving your game if you are in one; it no longer hands out
  a sign for every map, as 1.1.2 did.
- A Team Deathmatch player who is eliminated still gets their team's win or loss, also when they leave or log
  out before the game ends.
- In Team Deathmatch, elimination is final: an eliminated player who leaves and joins the same game again
  watches it as a spectator. 1.1.2 let them play on with a fresh life.
- Leaving a running game or logging out within 10 seconds of being hurt by another player counts as a death,
  and as a kill for that player. In Team Deathmatch the player who left is out.
- With `lobbyProtection` on, anyone in Creative mode, normally staff, can break decorations and hit mobs
  outside games; players stay protected.
- A player who is not in a game respawns at the PvP lobby, also while games run (2.0.0 did so only while
  no game was running). This is a default at low priority: a spawn plugin such as EssentialsSpawn can
  still set the respawn point, and leaving the End keeps its usual respawn.
- The lobby reset also clears experience and levels, so experience from kills no longer piles up across games.
- Name suggestions for `/pvp stats` and `/pvpadmin kick` leave out players the sender can't see, such as
  vanished staff.
- PlaceholderAPI and Multiverse-Core are optional dependencies now; WorldEdit and PluginUtils are no longer
  needed. Multiverse-Core only affects world load order when it is present.
- Going back to 2.0.0 needs only its jar, as long as WorldEdit, PluginUtils and PlaceholderAPI (which
  2.0.0 requires) are still installed: `maps/` and `stats/` are as 2.0.0 left them. Games played under
  3.0 stay in `players/` and `matches/` and do not reach 2.0.0's stats.

### Removed

- The old `com.mcmiddleearth.mcme.pvp` code; `/PlugUp`, `/mapeditor`, `/locker`, `/event`, `/winter`,
  `/summer`, the world commands, `/pvp togglevoxel`, `/pvp pipe`, `/pvp map spawn` and `/pvp broadcast`
  (opening a game announces it, as quickstart did; nothing repeats the announcement); every game mode
  except Team Deathmatch and Free For All (they return before 3.0.0).
- Old commands with a 3.0 equivalent: `/pvp game quickstart <map> [minutes]` is now `/pvpadmin start
  <arena> [timeLimit=<minutes>]`, `/pvp game start` is `/pvpadmin countdown <arena>`, `/pvp game end`
  is `/pvpadmin stop <arena>`, `/pvp game getgames` is `/pvp games`, `/pvp kick <player>` is `/pvpadmin
  kick <player>`, `/pvp map list` is `/pvpadmin arena list`, `/locker lock`, which toggled the lock,
  is now `/pvpadmin lock` and `/pvpadmin unlock`, and `/locker kickall` is `/pvpadmin kickall`.
- Arenas can't be created or edited in game until the editor returns; `/pvpadmin arena` lists, shows and
  re-imports them.
- Not yet back in the 3.0 development builds: 1.1.2's command blocklist during games, spectator-only chat and
  arrow tinting.

## [2.0.0] - 2026-09-23

Port to Paper 26.2. Gameplay is unchanged from 1.1.2, the build that ran in production before the
26.2 upgrade, except that team colors now apply (see Fixed).

### Changed

- Requires Paper 26.2 and Java 25. Built against `paper-api 26.2.build.126-stable`.
- Requires PluginUtils 2.0.3 or newer, WorldEdit 7.4.5 or newer and PlaceholderAPI 2.12.3 or newer.
- `api-version` is now `26.2`.
- The bundled Jackson is 2.22.3. Map and stat files written by 1.1.2 load unchanged, and tests pin
  that format.
- Uses the enchantment and potion names introduced in 1.20.5 (`UNBREAKING`, `NAUSEA`, `SLOWNESS`).
- Jars are named `MCME-PVP-{version}-{RELEASE|SNAPSHOT|DEV}-{commit}.jar`, and `plugin.yml` reports
  the real version instead of the literal `${project.version}`.

### Removed

- The BungeeCord half (`PVPBungee`, `bungee.yml`). It never loaded: its main class named a package
  that does not exist. Kicking players to the main server and cross-server game announcements still
  work through the proxy's built-in `BungeeCord` channel, which Velocity provides when
  `bungee-plugin-message-channel = true`.
- Unused dependencies: json-simple, org.json, spigot-command-api and bungeecord-api.
- The explicit Brigadier dependency. The command parser still uses Brigadier, which the server has
  always provided; the jar never bundled it, and `paper-api` now brings it in for compiling.

### Fixed

- PvP failed to enable on Paper 26.2. The 12 color teams were meant to get their color from a console
  command in pre-1.13 syntax (`scoreboard teams option <team> color <color>`), which has not worked
  since 1.13 and makes Paper 26.2 abort the plugin's startup. Team colors are now set through the API,
  so name tags, glow outlines and vanilla death messages show the team color. Chat and tab-list colors
  were already set by PvP.
- The `collision` team is reused when it already exists instead of being registered again, which
  would abort the plugin's startup if the scoreboard kept it from an earlier start.
- `plugin.yml`: `/winter` and `/summer` declared `usages:` instead of `usage:`.
