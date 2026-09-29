package com.mcmiddleearth.pvp.arena;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mcmiddleearth.pvp.PvpException;
import com.mcmiddleearth.pvp.text.Msg;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Every arena the plugin knows: the files in {@code arenas/}, plus the 1.1.2 maps in {@code maps/}, imported the
 * first time they are seen. An arena that exists is never imported again, so edits in the new format are never
 * overwritten; only {@link #reimport} reads a legacy map a second time. Nothing is ever written into {@code maps/}.
 */
public final class ArenaRegistry {

    /** Case-insensitive, then exact, so the order is stable even for ids that differ only in case. */
    private static final Comparator<Arena> BY_ID =
            Comparator.comparing(Arena::id, String.CASE_INSENSITIVE_ORDER).thenComparing(Arena::id);

    private final ArenaStore store;
    private final LegacyMapImporter importer;
    private final Path legacyMapsDir;
    private final Function<String, Optional<String>> modeForLegacyName;
    private final Logger logger;
    /**
     * Replaced as a whole by {@link #load()} and {@link #reimport}, so a reader never sees a half-built set.
     * Both run on the server thread; readers on any thread see a whole snapshot.
     */
    private volatile Map<String, Arena> arenas = Map.of();

    public ArenaRegistry(ArenaStore store, LegacyMapImporter importer, Path legacyMapsDir,
                         Function<String, Optional<String>> modeForLegacyName, Logger logger) {
        this.store = store;
        this.importer = importer;
        this.legacyMapsDir = legacyMapsDir;
        this.modeForLegacyName = modeForLegacyName;
        this.logger = logger;
    }

    /**
     * Reads every arena file, imports and saves each legacy map whose {@code arenas/<id>.json} does not exist yet,
     * and assigns a mode to unassigned imports whose 1.1.2 game mode has one now. Replaces the in-memory set. A
     * broken or unreadable file — arena or legacy map — is reported and left alone rather than overwritten, so an
     * admin's edits are never lost; deleting it imports the map again at the next start.
     */
    public LoadReport load() {
        Map<String, Arena> loaded = new HashMap<>();
        Map<String, String> failed = new TreeMap<>();
        readStored(loaded, failed);
        List<String> imported = importNew(loaded, failed);
        mapAgain(loaded, failed);
        logSkippedMapFiles();
        arenas = Map.copyOf(loaded);

        Map<String, String> unassigned = new TreeMap<>();
        Map<String, List<String>> unknownProperties = new TreeMap<>();
        for (Arena arena : loaded.values()) {
            if (!arena.playable()) {
                unassigned.put(arena.id(), Objects.requireNonNullElse(arena.unassignedReason(), "no reason recorded"));
            }
            if (arena.imported() != null && !arena.imported().unknownProperties().isEmpty()) {
                unknownProperties.put(arena.id(), arena.imported().unknownProperties());
            }
        }
        return new LoadReport(loaded.size(), imported, unassigned, unknownProperties, failed);
    }

    /** The arena with this id; an exact match first, then one that differs only in case. */
    public Optional<Arena> get(String id) {
        Map<String, Arena> current = arenas;
        Arena exact = current.get(id);
        if (exact != null) {
            return Optional.of(exact);
        }
        return current.values().stream().filter(arena -> arena.id().equalsIgnoreCase(id)).min(BY_ID);
    }

    /** Every arena, sorted by id ignoring case. */
    public List<Arena> all() {
        return arenas.values().stream().sorted(BY_ID).toList();
    }

    /** The arenas that have a mode, sorted like {@link #all()}. */
    public List<Arena> playable() {
        return all().stream().filter(Arena::playable).toList();
    }

    /**
     * Imports {@code maps/<id>} again (the exact file name first, then ignoring case), overwrites
     * {@code arenas/<id>.json} and replaces the arena in memory.
     *
     * @throws PvpException         {@link Msg#ERROR_UNKNOWN_ARENA} when there is no such legacy map
     * @throws UncheckedIOException when the map cannot be read or the arena cannot be saved
     */
    public Arena reimport(String id) {
        Path file = legacyFile(id).orElseThrow(() -> new PvpException(Msg.ERROR_UNKNOWN_ARENA, Map.of("arena", id)));
        try {
            Arena arena = importer.importFile(file);
            store.save(arena);
            Map<String, Arena> updated = new HashMap<>(arenas);
            updated.put(arena.id(), arena);
            arenas = Map.copyOf(updated);
            return arena;
        } catch (IOException e) {
            throw new UncheckedIOException("maps/" + file.getFileName() + " could not be imported: " + describe(e), e);
        } catch (IllegalArgumentException e) {
            // fileFor() rejected the arena's id: staff see the same failure type and wording as a read or save problem.
            // Its reason always restates this same id, which already appears in "maps/<file>" above, so it is
            // dropped here instead of being shown twice.
            throw new UncheckedIOException("maps/" + file.getFileName() + " could not be imported: not an arena id",
                    new IOException(describe(e), e));
        }
    }

    private void readStored(Map<String, Arena> loaded, Map<String, String> failed) {
        List<Path> files;
        try {
            files = store.files();
        } catch (IOException e) {
            failed.put("arenas/", describe(e));
            return;
        }
        for (Path file : files) {
            try {
                Arena arena = store.read(file);
                if (arena.id() == null || arena.id().isBlank()) {
                    throw new IOException("the file has no arena id");
                }
                Path expected;
                try {
                    expected = store.fileFor(arena.id());
                } catch (IllegalArgumentException e) {
                    expected = null;
                }
                // Compared as exact strings, not Path.equals: prod is Linux, where "as-tdm.json" and "AS-TDM.json"
                // are different files, even though Path.equals ignores case on Windows.
                if (expected == null) {
                    throw new IOException("the file holds arena \"" + arena.id() + "\", whose id is not a valid file name");
                }
                if (!expected.getFileName().toString().equals(file.getFileName().toString())) {
                    throw new IOException("the file holds arena \"" + arena.id() + "\"; name it " + arena.id() + ".json or change its id");
                }
                loaded.put(arena.id(), arena);
            } catch (IOException | RuntimeException e) {
                // One odd file must not stop the plugin from enabling: it is reported and skipped.
                failed.put(file.getFileName().toString(), describe(e));
            }
        }
    }

    private List<String> importNew(Map<String, Arena> loaded, Map<String, String> failed) {
        List<Path> files;
        try {
            files = LegacyMapImporter.legacyFiles(legacyMapsDir);
        } catch (IOException e) {
            failed.put("maps/", describe(e));
            return List.of();
        }
        List<String> imported = new ArrayList<>();
        for (Path file : files) {
            String id = file.getFileName().toString();
            if (loaded.containsKey(id)) {
                continue;
            }
            try {
                if (!Files.notExists(store.fileFor(id))) {
                    // arenas/<id>.json exists but failed to load (readStored didn't put it in `loaded`): it must
                    // never be overwritten, so an admin's edits are never lost. notExists() is conservative (an
                    // unknown state counts as "exists" too). Deleting the file imports the map again at the next
                    // start.
                    continue;
                }
                Arena arena = importer.importFile(file);
                store.save(arena);
                loaded.put(arena.id(), arena);
                imported.add(arena.id());
            } catch (IOException | RuntimeException e) {
                failed.put("maps/" + id, describe(e));
            }
        }
        return imported;
    }

    /** Picks up modes added since the import: an unassigned import whose 1.1.2 game mode now has one. */
    private void mapAgain(Map<String, Arena> loaded, Map<String, String> failed) {
        for (Arena arena : List.copyOf(loaded.values())) {
            if (arena.playable() || arena.imported() == null) {
                continue;
            }
            String gmType = arena.imported().legacyGmType();
            if (gmType == null || gmType.isBlank()
                    || !LegacyMapImporter.noModeReason(gmType).equals(arena.unassignedReason())) {
                // Not the importer's own missing-mode reason: a hand-set reason (or one already re-mapped once,
                // such as "the map has no spawn point") must never be overridden.
                continue;
            }
            Optional<String> mode = modeForLegacyName.apply(gmType);
            if (mode.isEmpty()) {
                continue;
            }
            // The same rule as the importer: a map without a spawn stays unassigned, now for that reason.
            Arena updated = arena.spawn() != null ? arena.withMode(mode.get()) : arena.unassigned(LegacyMapImporter.NO_SPAWN);
            if (updated.equals(arena)) {
                continue;
            }
            try {
                store.save(updated);
                loaded.put(updated.id(), updated);
                if (updated.playable()) {
                    logger.info("Arena " + arena.id() + " now plays " + mode.get() + " (1.1.2 game mode \"" + gmType + "\")");
                }
            } catch (IOException | RuntimeException e) {
                failed.put(store.fileFor(arena.id()).getFileName().toString(), describe(e));
            }
        }
    }

    /** Reports, once at startup, the names in {@code maps/} that were skipped because they are clearly not maps. */
    private void logSkippedMapFiles() {
        try {
            List<String> skipped = LegacyMapImporter.skippedJunkNames(legacyMapsDir);
            if (!skipped.isEmpty()) {
                logger.info("Skipped files in maps/ that are not maps: " + String.join(", ", skipped));
            }
        } catch (IOException e) {
            // maps/ could not be listed: importNew() already reported that failure above.
        }
    }

    private Optional<Path> legacyFile(String id) {
        List<Path> files;
        try {
            files = LegacyMapImporter.legacyFiles(legacyMapsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("maps/ could not be listed: " + describe(e), e);
        }
        Optional<Path> exact = files.stream().filter(file -> file.getFileName().toString().equals(id)).findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return files.stream().filter(file -> file.getFileName().toString().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * A message worth showing an admin. A Jackson parse or mapping failure keeps its original message plus the
     * source location and, when known, the field/index path (for example {@code teamSpawns.red[0].x}) instead of
     * the raw {@code getMessage()}, which buries that under an appended "at [Source: ...]". A
     * {@link FileSystemException} with no system-supplied reason keeps its type, since its own message is then
     * just the file name. Anything else keeps its own message, or its class name when that is null.
     */
    private static String describe(Exception e) {
        if (e instanceof JsonProcessingException json) {
            String original = json.getOriginalMessage();
            StringBuilder text = new StringBuilder(original != null ? original : json.getClass().getSimpleName());
            JsonLocation location = json.getLocation();
            if (location != null && location != JsonLocation.NA) {
                text.append(" (line ").append(location.getLineNr()).append(", column ").append(location.getColumnNr()).append(')');
            }
            if (json instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
                text.append(", at ").append(path(mapping.getPath()));
            }
            return text.toString();
        }
        if (e instanceof FileSystemException fse && fse.getReason() == null) {
            return e.getClass().getSimpleName() + ": " + fse.getFile();
        }
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName() : message;
    }

    /** {@code teamSpawns.red[0].x}: field names joined with '.', an index written as {@code [i]} with no separator before it. */
    private static String path(List<JsonMappingException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : references) {
            if (reference.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }
}
