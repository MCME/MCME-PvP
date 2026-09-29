MCME-PVP
===========

## Requirements

- Paper 26.2 and Java 25
- Optional: PlaceholderAPI 2.12.3 or newer, for the `%mcmepvp_prefix%` and `%mcmepvp_color%` placeholders
- Optional: Multiverse-Core. When it is installed, it loads the arena worlds first, with its own
  generator settings.
- On the Velocity proxy: `bungee-plugin-message-channel = true` in `velocity.toml`. PvP uses it to
  send players back to the main server and to announce games on other servers.
- On Paper 26.2, set `gamerule locator_bar false` in the PvP world(s). Otherwise the locator bar shows
  every player's direction, colored by team.

## Building

Requires JDK 25 and Maven 3.9 or newer.

    mvn clean verify

The jar is written to `target/MCME-PVP-{version}-DEV-{commit}.jar`. While 3.0 is in development, every jar
is a development build; the first release will be 3.0.0.

In a linked git worktree, add `-Dgit.commit.id.abbrev=$(git rev-parse --short=7 HEAD)`: there the git
plugin silently reads the main checkout's HEAD, so the jar would be named after the wrong commit.

## What it does

Several PvP games can run at once. Two modes are ready: Team Deathmatch (2 to 4 teams) and Free For All (a
time limit and an optional score goal). Players join, spectate and see open games through Paper dialog menus
and clickable chat; staff start, count down and stop games the same way. Each game shows a HUD: a sidebar, a
boss bar for the countdown or time limit, and titles for the countdown, the start, an elimination and the
result.

## Commands

`/pvp`, `/pvpadmin` and `/t` are registered at runtime through Paper's Brigadier API.

- `/pvp`: the main menu. Sub-commands: `join [arena]`, `leave`, `spectate <arena>`, `games [page]`,
  `rules [mode]` (a mode id: `tdm` or `ffa`), `stats [player]`, `lobby`.
- `/pvpadmin`: staff commands. Sub-commands: `start [arena] [setting=value ...]`, `countdown <arena>`,
  `stop <arena>`, `kick <player>`, `kickall`, `lock`, `unlock`, `arena list`, `arena info <id>` and
  `arena import <id>`.
- `/t <message>`: chat with your team.

## Permissions

From `plugin.yml`; every node except `pvp.play` defaults to op:

- `pvp.play` (default: true): Join, leave and spectate games; see rules and stats; team chat.
- `pvp.join.locked`: Stay on the server while PvP is locked.
- `pvp.staff.run`: Open, count down and stop games; lock and unlock PvP; send everyone to the main server.
- `pvp.staff.kick`: Send one player to the main server.
- `pvp.staff.arena`: List, inspect and re-import arenas.
- `pvp.admin`: Everything. Also shows the "PvP Staff" chat prefix.

1.1.2's nodes (`pvp.join`, `pvp.gameKick`, `pvp.gameCanRun`, `pvp.mapCreate`, `pvp.adminPermission`, and the
group nodes `pvp.builder`, `pvp.moderator`, `pvp.manager`) still work, as parents of these, so existing
LuckPerms grants and groups keep working.

## Data

The plugin's data folder holds `config.yml`, `messages.yml`, and 3.0's own `arenas/`, `players/` and
`matches/<yyyy-MM>.jsonl`. The 1.1.2 folders `maps/` and `stats/` are only ever read, never written to:
`maps/` at startup, to import any map that has no `arenas/<id>.json` yet, and by `/pvpadmin arena import
<id>`; `stats/` for a player until their first finished or watched game copies their stats into `players/`.

See `CHANGELOG.md` for what changed since 2.0.0.