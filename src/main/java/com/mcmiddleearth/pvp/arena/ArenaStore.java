package com.mcmiddleearth.pvp.arena;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.pvp.storage.AsyncWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The arena files, {@code arenas/<id>.json}. Saves are synchronous and atomic: arenas change only at startup and
 * on a staff command, and the registry must know the file is written before it moves on.
 */
public final class ArenaStore {

    private static final String EXTENSION = ".json";

    private final Path arenasDir;
    private final ObjectMapper mapper;

    public ArenaStore(Path arenasDir, ObjectMapper mapper) {
        this.arenasDir = arenasDir;
        this.mapper = mapper;
    }

    /**
     * {@code arenasDir/<id>.json}. Throws {@link IllegalArgumentException} when {@code arenaId} is not a single
     * plain file-name segment: blank, {@code /} or {@code \} in it, {@code .} or {@code ..}, or (catching
     * absolute and drive-qualified ids too) a resolved file that does not land directly inside {@code arenasDir}.
     */
    public Path fileFor(String arenaId) {
        if (arenaId.isBlank() || arenaId.contains("/") || arenaId.contains("\\")
                || arenaId.equals(".") || arenaId.equals("..")) {
            throw new IllegalArgumentException("not an arena id: " + arenaId);
        }
        Path candidate = arenasDir.resolve(arenaId + EXTENSION);
        if (!candidate.normalize().getParent().equals(arenasDir.normalize())) {
            throw new IllegalArgumentException("not an arena id: " + arenaId);
        }
        return candidate;
    }

    /** Reads one arena file; a malformed or incomplete file throws IOException. */
    public Arena read(Path file) throws IOException {
        return mapper.readValue(Files.readAllBytes(file), Arena.class);
    }

    /** Writes the arena to {@link #fileFor} at once, replacing the old file atomically. */
    public void save(Arena arena) throws IOException {
        AsyncWriter.writeAtomically(fileFor(arena.id()), mapper.writeValueAsBytes(arena));
    }

    /** The {@code *.json} files in the folder, sorted by name; empty when the folder does not exist. */
    public List<Path> files() throws IOException {
        if (!Files.isDirectory(arenasDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(arenasDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
