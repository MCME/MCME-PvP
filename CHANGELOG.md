# Changelog

All notable changes to MCME-PvP are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [2.0.0] - 2026-09-23

Port to Paper 26.2. Gameplay is unchanged from 1.1.2, the build that ran in production before the
26.2 upgrade.

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
- Unused dependencies: json-simple, org.json, spigot-command-api, and the bundled Brigadier (Paper
  provides it).

### Fixed

- `plugin.yml`: `/winter` and `/summer` declared `usages:` instead of `usage:`.
