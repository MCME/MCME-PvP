package com.mcmiddleearth.pvp.arena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.pvp.storage.Json;
import com.mcmiddleearth.pvp.team.Palette;
import com.mcmiddleearth.pvp.team.TeamSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a 1.1.2 map file ({@code maps/<id>}: one line of JSON, no extension) into an {@link Arena}. It only
 * reads: 3.0 never writes into {@code maps/}, so going back to 2.x needs nothing but the old jar. The file is read
 * as a JSON tree, so keys that older builds wrote (and 1.1.2 choked on) never fail an import. A point,
 * {@code importantPoints} or {@code regionPoints} that is present but not shaped as one does fail the import: a
 * failed map is reported and retried at every start, so fixing the file heals it, whereas a silently wrong arena
 * would be saved once and never re-derived.
 */
public final class LegacyMapImporter {

    /** Why an imported arena is unassigned. ArenaRegistry uses the same texts when it maps an arena again. */
    static final String NO_GAME_MODE = "the map has no game mode";
    static final String NO_SPAWN = "the map has no spawn point";

    /**
     * The reason an import gets when no 3.0 mode matches its 1.1.2 game mode (yet). The text is stored in arena
     * files, and {@code ArenaRegistry} matches it to map those arenas again once the mode exists, so its wording
     * must never change.
     */
    static String noModeReason(String gmType) {
        return "no 3.0 mode for \"" + gmType + "\" yet";
    }

    /** The keys 1.1.2 writes. Any other key is reported, such as fbt, lobbySign and mapSpectatorSpawn. */
    private static final Set<String> KNOWN_KEYS = Set.of("gmType", "name", "title", "max", "curr", "spawn",
            "importantPoints", "regionPoints", "resourcePackURL");
    /** A team spawn: a team name, "spawn" and an optional number (RedSpawn, blueSpawn2), in any case. */
    private static final Pattern TEAM_SPAWN = Pattern.compile("([a-z]+)spawn(\\d{0,9})", Pattern.CASE_INSENSITIVE);
    private static final Set<String> TEAM_IDS = Palette.TEAMS.stream().map(TeamSpec::id).collect(Collectors.toUnmodifiableSet());
    /** Exact junk names a file browser or editor leaves behind, matched case-insensitively. */
    private static final Set<String> JUNK_NAMES = Set.of("desktop.ini", "thumbs.db");
    /** Junk suffixes: a backup or swap file left by an editor, or a Windows "~" backup, matched case-insensitively. */
    private static final Set<String> JUNK_SUFFIXES = Set.of("~", ".bak", ".tmp", ".orig", ".swp");
    /** Junk substrings: a duplicate a file manager made, matched case-insensitively. */
    private static final Set<String> JUNK_SUBSTRINGS = Set.of(" - copy", " - kopie");
    /** 1.1.2 built every region from y 0 to 1000, whatever heights its points had. */
    private static final int LEGACY_MIN_Y = 0;
    private static final int LEGACY_MAX_Y = 1000;
    private static final String DEFAULT_WORLD = "world";

    private final Function<String, Optional<String>> modeForLegacyName;
    private final Clock clock;
    private final ObjectMapper mapper = Json.compact();

    /**
     * @param modeForLegacyName the 3.0 mode id for a 1.1.2 gmType ("Team Deathmatch"), if a mode exists for it
     * @param clock             stamps {@link ImportInfo#importedAt()}
     */
    public LegacyMapImporter(Function<String, Optional<String>> modeForLegacyName, Clock clock) {
        this.modeForLegacyName = modeForLegacyName;
        this.clock = clock;
    }

    /**
     * The regular files directly in {@code mapsDir}, sorted by name, skipping folders ({@code backup/}), names
     * ending in {@code .new} (1.1.2 left them behind after a failed save), hidden files and names that are clearly
     * not maps, such as {@code desktop.ini} or {@code AS-TDM - Copy} ({@link #isJunkName}). An absent folder gives
     * an empty list.
     */
    public static List<Path> legacyFiles(Path mapsDir) throws IOException {
        if (!Files.isDirectory(mapsDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(mapsDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> isMapName(file.getFileName().toString()))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * The regular files directly in {@code mapsDir} that {@link #legacyFiles} leaves out because they are clearly
     * not maps ({@link #isJunkName}); hidden files and {@code .new} files are not included, since those are already
     * silently, and correctly, skipped. Sorted by name. An absent folder gives an empty list.
     */
    public static List<String> skippedJunkNames(Path mapsDir) throws IOException {
        if (!Files.isDirectory(mapsDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(mapsDir)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(name -> !name.startsWith(".") && !name.endsWith(".new") && isJunkName(name))
                    .sorted()
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Reads {@code file} into an arena whose id is the file name. Never writes; malformed JSON throws, and so does
     * a point, {@code importantPoints} or {@code regionPoints} that is present but not shaped as one.
     */
    public Arena importFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        JsonNode root = mapper.readTree(bytes);
        if (root == null || !root.isObject()) {
            throw new IOException(file.getFileName() + " does not hold a JSON object");
        }
        String id = file.getFileName().toString();
        String gmType = textOrNull(root.get("gmType"));

        NamedPoint spawn = point("spawn", root.get("spawn"), NamedPoint::fromLegacyFeet);
        if (spawn == null) {
            // The 2022-04 build (1.0.8) saved the spawn under this name.
            spawn = point("mapSpectatorSpawn", root.get("mapSpectatorSpawn"), NamedPoint::fromLegacyFeet);
        }
        Map<String, NamedPoint> points = importantPoints(root.get("importantPoints"));

        String modeId = null;
        String reason;
        if (gmType == null || gmType.isBlank()) {
            reason = NO_GAME_MODE;
        } else {
            Optional<String> mode = modeForLegacyName.apply(gmType);
            modeId = mode.orElse(null);
            reason = mode.isPresent() ? null : noModeReason(gmType);
        }
        if (modeId != null && spawn == null) {
            modeId = null;
            reason = NO_SPAWN;
        }

        ImportInfo imported = new ImportInfo(id, sha256(bytes), clock.instant().toString(), gmType, unknownKeys(root));
        return new Arena(Arena.SCHEMA_VERSION, id, modeId, reason, title(root, id), root.path("max").asInt(0),
                textOrNull(root.get("resourcePackURL")), spawn, points, teamSpawns(points),
                region(root.get("regionPoints")), imported);
    }

    private static boolean isMapName(String name) {
        return !name.startsWith(".") && !name.endsWith(".new") && !isJunkName(name);
    }

    /**
     * A name that is clearly not a map, whatever it contains: {@code desktop.ini}, {@code Thumbs.db}, a backup or
     * swap file ({@code ~}, {@code .bak}, {@code .tmp}, {@code .orig}, {@code .swp}) or a file manager's duplicate
     * ({@code " - Copy"}, {@code " - kopie"}), matched in any case. Never by extension otherwise: production map
     * files have none.
     */
    private static boolean isJunkName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (JUNK_NAMES.contains(lower)) {
            return true;
        }
        for (String suffix : JUNK_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        for (String substring : JUNK_SUBSTRINGS) {
            if (lower.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private static String title(JsonNode root, String id) {
        for (String key : List.of("title", "name")) {
            String text = textOrNull(root.get(key));
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return id;
    }

    /**
     * Important points by name, as standing positions (1.1.2 stored the block below the admin's feet). Absent or
     * JSON-null {@code node} gives no points; a present {@code node} that is not an object throws.
     */
    private static Map<String, NamedPoint> importantPoints(JsonNode node) throws IOException {
        Map<String, NamedPoint> points = new TreeMap<>();
        if (node == null || node.isNull()) {
            return points;
        }
        if (!node.isObject()) {
            throw new IOException("importantPoints is not a JSON object");
        }
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            NamedPoint point = point("importantPoints." + entry.getKey(), entry.getValue(), NamedPoint::fromLegacyFloor);
            if (point != null) {
                points.put(entry.getKey(), point);
            }
        }
        return points;
    }

    /**
     * RedSpawn → red, BlueSpawn2 → blue, ...: when the unnumbered {@code <Team>Spawn} exists it is the team's only
     * spawn (1.1.2's Team Deathmatch used only {@code RedSpawn}/{@code BlueSpawn}, and maps that once belonged to
     * other modes can still carry numbered ones); otherwise the numbered points, by number.
     */
    private static Map<String, List<NamedPoint>> teamSpawns(Map<String, NamedPoint> points) {
        Map<String, TreeMap<Integer, NamedPoint>> numbered = new TreeMap<>();
        points.forEach((name, point) -> {
            Matcher matcher = TEAM_SPAWN.matcher(name);
            if (!matcher.matches()) {
                return;
            }
            String team = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!TEAM_IDS.contains(team)) {
                return;
            }
            int number = matcher.group(2).isEmpty() ? -1 : Integer.parseInt(matcher.group(2));
            // points is sorted by name, so when two names share a number (RedSpawn1, RedSpawn01) the first wins.
            numbered.computeIfAbsent(team, key -> new TreeMap<>()).putIfAbsent(number, point);
        });
        Map<String, List<NamedPoint>> spawns = new TreeMap<>();
        numbered.forEach((team, byNumber) -> spawns.put(team,
                byNumber.containsKey(-1) ? List.of(byNumber.get(-1)) : List.copyOf(byNumber.values())));
        return spawns;
    }

    /**
     * The polygon of 3 or more regionPoints, in the first point's world; the points' heights are ignored, as in
     * 1.1.2. Absent or JSON-null {@code node} gives no region. A present {@code node} that is not an array, or a
     * point in it that is not an object or lacks a numeric x/z, throws.
     */
    private static Region region(JsonNode node) throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new IOException("regionPoints is not a JSON array");
        }
        if (node.size() < 3) {
            return null;
        }
        List<Region.Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            String where = "regionPoints[" + i + "]";
            JsonNode point = node.get(i);
            if (!point.isObject()) {
                throw new IOException(where + " is not a JSON object");
            }
            vertices.add(new Region.Vertex(coordinate(where, point, "x"), coordinate(where, point, "z")));
        }
        return new Region(world(node.get(0)), vertices, LEGACY_MIN_Y, LEGACY_MAX_Y);
    }

    /**
     * A {"world", "x", "y", "z"} object (int coordinates), or null when {@code node} is absent or JSON null. A
     * present {@code node} that is not an object, or whose x/y/z is missing or not a number, throws naming
     * {@code where}.
     */
    private static NamedPoint point(String where, JsonNode node, LegacyPoint factory) throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new IOException(where + " is not a JSON object");
        }
        return factory.create(world(node), coordinate(where, node, "x"), coordinate(where, node, "y"), coordinate(where, node, "z"));
    }

    /** A required int coordinate of {@code point}; throws naming {@code where} and {@code axis} when it is missing or not a number. */
    private static int coordinate(String where, JsonNode point, String axis) throws IOException {
        JsonNode value = point.get(axis);
        if (value == null || !value.isNumber() || !value.canConvertToInt()) {
            throw new IOException(where + ": \"" + axis + "\" is missing or not a number");
        }
        return value.asInt(); // a decimal is truncated, as 1.1.2's Jackson did
    }

    private static String world(JsonNode point) {
        String world = textOrNull(point.get("world"));
        return world == null || world.isBlank() ? DEFAULT_WORLD : world;
    }

    private static List<String> unknownKeys(JsonNode root) {
        List<String> unknown = new ArrayList<>();
        root.fieldNames().forEachRemaining(name -> {
            if (!KNOWN_KEYS.contains(name)) {
                unknown.add(name);
            }
        });
        unknown.sort(Comparator.naturalOrder());
        return unknown;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every Java runtime provides SHA-256", e);
        }
    }

    /** NamedPoint::fromLegacyFeet or NamedPoint::fromLegacyFloor. */
    @FunctionalInterface
    private interface LegacyPoint {
        NamedPoint create(String world, int x, int y, int z);
    }
}
