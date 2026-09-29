package com.mcmiddleearth.pvp.storage;

import com.mcmiddleearth.pvp.RecordingLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncWriterTest {

    @TempDir
    Path dir;

    private final RecordingLog log = new RecordingLog();

    @Test
    void writeAtomicallyReplacesTheFileAndLeavesNoTemporaryFile() throws IOException {
        Path file = dir.resolve("arenas/AS-TDM.json");

        AsyncWriter.writeAtomically(file, bytes("old"));
        AsyncWriter.writeAtomically(file, bytes("new"));

        assertEquals("new", Files.readString(file));
        assertEquals(List.of("AS-TDM.json"), names(file.getParent()), "no .tmp file and no backup/ folder");
    }

    @Test
    void aFailedTemporaryWriteLeavesNoTemporaryFile() throws IOException {
        Path file = dir.resolve("broken.json");
        Files.createDirectories(dir.resolve("broken.json.tmp"));

        assertThrows(IOException.class, () -> AsyncWriter.writeAtomically(file, bytes("x")));

        assertFalse(Files.exists(dir.resolve("broken.json.tmp")));
        assertFalse(Files.exists(file));
    }

    @Test
    void aFailedMoveLeavesNoTemporaryFile() throws IOException {
        Path target = dir.resolve("stats.json");
        Files.createDirectory(target);
        Files.writeString(target.resolve("child"), "kept");

        assertThrows(IOException.class, () -> AsyncWriter.writeAtomically(target, bytes("new")));

        assertFalse(Files.exists(dir.resolve("stats.json.tmp")));
        assertEquals("kept", Files.readString(target.resolve("child")));
    }

    @Test
    void appendCreatesTheFolderAndAddsToTheEnd() throws IOException {
        Path file = dir.resolve("matches/2026-09.jsonl");
        AsyncWriter writer = new AsyncWriter(Runnable::run, log.logger());

        writer.append(file, bytes("{\"n\":1}\n"));
        writer.append(file, bytes("{\"n\":2}\n"));

        assertEquals(List.of("{\"n\":1}", "{\"n\":2}"), Files.readAllLines(file));
    }

    @Test
    void appendNeverGluesOntoATornLastLine() throws IOException {
        Path file = dir.resolve("matches/2026-09.jsonl");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"torn\":");
        AsyncWriter writer = new AsyncWriter(Runnable::run, log.logger());

        writer.append(file, bytes("{\"ok\":1}\n"));

        assertEquals(List.of("{\"torn\":", "{\"ok\":1}"), Files.readAllLines(file));
        assertTrue(Files.readString(file).endsWith("\n"));
    }

    @Test
    void writesRunInTheOrderTheyWereQueued() throws IOException {
        Path file = dir.resolve("players/state.json");
        Path history = dir.resolve("matches/history.jsonl");

        try (AsyncWriter writer = new AsyncWriter(log.logger())) {
            for (int i = 0; i < 50; i++) {
                writer.write(file, bytes("v" + i));
                writer.append(history, bytes(i + "\n"));
            }
        }

        assertEquals("v49", Files.readString(file));
        assertEquals(IntStream.range(0, 50).mapToObj(Integer::toString).toList(), Files.readAllLines(history));
    }

    @Test
    void aFailedWriteIsLoggedWithItsPathAndLaterWritesStillRun() throws IOException {
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "a file where a folder is needed");
        Path impossible = blocker.resolve("child.json");
        Path fine = dir.resolve("fine.json");

        try (AsyncWriter writer = new AsyncWriter(log.logger())) {
            writer.write(impossible, bytes("lost"));
            writer.write(fine, bytes("kept"));
        }

        assertEquals("kept", Files.readString(fine));
        List<String> warnings = log.messages(Level.WARNING);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.getFirst().contains(impossible.toString()), warnings.getFirst());
    }

    @Test
    void aWriteTheExecutorRefusesRunsAtOnce() throws IOException {
        Path file = dir.resolve("saved.json");
        AsyncWriter writer = new AsyncWriter(task -> { throw new RejectedExecutionException("shut down"); },
                log.logger());

        writer.write(file, bytes("saved"));

        assertEquals("saved", Files.readString(file));
    }

    @Test
    void writesAfterCloseRunAtOnceOnTheCallersThread() throws IOException {
        List<Runnable> queued = new ArrayList<>();
        AsyncWriter writer = new AsyncWriter(queued::add, log.logger());

        writer.write(dir.resolve("before.json"), bytes("before"));
        assertEquals(1, queued.size());
        assertFalse(Files.exists(dir.resolve("before.json")), "a queued write waits for the executor");

        writer.close();
        writer.write(dir.resolve("after.json"), bytes("after"));
        writer.append(dir.resolve("after.jsonl"), bytes("line\n"));

        assertEquals("after", Files.readString(dir.resolve("after.json")));
        assertEquals("line\n", Files.readString(dir.resolve("after.jsonl")));
        assertEquals(1, queued.size(), "writes after close() bypass the executor");
    }

    @Test
    void closeFinishesEveryQueuedWrite() throws IOException {
        AsyncWriter writer = new AsyncWriter(log.logger());
        for (int i = 0; i < 20; i++) {
            writer.write(dir.resolve("players/" + i + ".json"), bytes("stats " + i));
        }

        writer.close();

        for (int i = 0; i < 20; i++) {
            assertEquals("stats " + i, Files.readString(dir.resolve("players/" + i + ".json")));
        }
    }

    @Test
    void theContentIsCopiedWhenTheWriteIsQueued() throws IOException {
        List<Runnable> queued = new ArrayList<>();
        AsyncWriter writer = new AsyncWriter(queued::add, log.logger());
        byte[] content = bytes("original");

        writer.write(dir.resolve("copy.json"), content);
        content[0] = 'X';
        queued.forEach(Runnable::run);

        assertEquals("original", Files.readString(dir.resolve("copy.json")));
    }

    @Test
    void theLineIsCopiedWhenTheAppendIsQueued() throws IOException {
        List<Runnable> queued = new ArrayList<>();
        AsyncWriter writer = new AsyncWriter(queued::add, log.logger());
        byte[] line = bytes("original\n");

        writer.append(dir.resolve("copy.jsonl"), line);
        line[0] = 'X';
        queued.forEach(Runnable::run);

        assertEquals(List.of("original"), Files.readAllLines(dir.resolve("copy.jsonl")));
    }

    @Test
    void itsOwnThreadIsADaemonNamedForThePlugin() {
        try (AsyncWriter writer = new AsyncWriter(log.logger())) {
            writer.write(dir.resolve("start.json"), bytes("starts the thread"));

            assertTrue(Thread.getAllStackTraces().keySet().stream()
                    .anyMatch(thread -> thread.getName().equals("MCME-PvP writer") && thread.isDaemon()));
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> names(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }
}
