MCME-PVP
===========

## Requirements

- Paper 26.2 and Java 25
- PluginUtils 2.0.3 or newer, WorldEdit 7.4.5 or newer, PlaceholderAPI 2.12.3 or newer
- On the Velocity proxy: `bungee-plugin-message-channel = true` in `velocity.toml`. PvP uses it to
  send players back to the main server and to announce games on other servers.
- On Paper 26.2, set `gamerule locator_bar false` in the PvP world(s). Otherwise the locator bar shows
  every player's direction, colored by team.

## Building

Requires JDK 25 and Maven 3.9 or newer.

    mvn clean verify

The jar is written to `target/MCME-PVP-{version}-RELEASE-{commit}.jar`. For a development build, run
`mvn clean verify -Dbuild.type=DEV`.

## PVP for MCME
This project is once again in development!

### Bug fixes
* Team Conquest - apparently it's bugged, but I don't know what specifically
* Rinbgearer - RB's glowstone helmet stays on during invisibility

### To be implemented
* /pvp help - guide for setting up maps and starting games
* Keep track of sessions for players so they can be reassigned to the same team on rejoin
* /pvp assign \<player> \<team> to assign a player to a certain team in a running game
* pipe smoke texture
* xxxEvent point to get events on maps (look wether to hardcode or not)

### Code improvements
* Simplify the command files so they're easier to read
* Tests? This might be a bit ambitious

### New Features
* UI for map creation and editing (with showing spawns)
* Teleportation to maps
* Brigadier command parser
* An actual queue for PVP
* An updated permission system
* A death run gamemode

Needed commands for wrapper:

Shell: mv plugins\update\MCME-Events-0.1.jar plugins\MCME-Events-0.1.jar

MSDOS: MOVE /y plugins\update\MCME-Events-0.1.jar plugins\MCME-Events-0.1.jar