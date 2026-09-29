package com.mcmiddleearth.pvp.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes the plugin's files off the server thread, one at a time and in the order they were asked for.
 * {@link #write} replaces a file atomically (a temporary file in the same folder, then a move over the old
 * one), so a crash never leaves half a file; that guarantee covers {@code write} only. {@link #append} adds
 * one line: when the file's last line was torn by a crash or a full disk, the new line starts on a fresh
 * {@code \n} of its own, so the torn line loses only itself, never the next one. Unlike 1.1.2's DBmanager it
 * never makes a {@code backup/} folder.
 */
public final class AsyncWriter implements AutoCloseable {

    private static final long CLOSE_TIMEOUT_SECONDS = 10;

    private final Executor executor;
    /** The writer's own thread, or null when the caller supplied the executor. */
    private final ExecutorService ownThread;
    private final Logger logger;
    private volatile boolean closed;

    /** Writes on one daemon thread named "MCME-PvP writer". */
    public AsyncWriter(Logger logger) {
        this(Executors.newSingleThreadExecutor(AsyncWriter::newThread), true, logger);
    }

    /**
     * Writes on {@code executor}, which must run tasks in submission order. Tests pass {@code Runnable::run}.
     * The caller owns the executor: {@link #close()} does not shut it down.
     */
    public AsyncWriter(Executor executor, Logger logger) {
        this(executor, false, logger);
    }

    private AsyncWriter(Executor executor, boolean ownsExecutor, Logger logger) {
        this.executor = executor;
        this.ownThread = ownsExecutor ? (ExecutorService) executor : null;
        this.logger = logger;
    }

    /** Queues an atomic replace of {@code target}. A failure is logged at WARNING and does not stop later writes. */
    public void write(Path target, byte[] content) {
        byte[] copy = content.clone();
        submit(target, () -> writeAtomically(target, copy));
    }

    /**
     * Queues appending {@code line} to {@code target}, creating the file and its folders when needed. When
     * {@code target} already exists, isn't empty and doesn't end with {@code \n} (its last line was torn by a
     * crash or a full disk), {@code line} is written after a {@code \n} of its own, so the torn line is left
     * as it was.
     */
    public void append(Path target, byte[] line) {
        byte[] copy = line.clone();
        submit(target, () -> {
            createParent(target);
            byte[] toWrite = needsNewlinePrefix(target) ? withLeadingNewline(copy) : copy;
            Files.write(target, toWrite, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        });
    }

    /** True when {@code target} exists, isn't empty and its last byte isn't {@code \n}. */
    private static boolean needsNewlinePrefix(Path target) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(target, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size == 0) {
                return false;
            }
            channel.position(size - 1);
            ByteBuffer lastByte = ByteBuffer.allocate(1);
            channel.read(lastByte);
            return lastByte.get(0) != '\n';
        } catch (NoSuchFileException e) {
            return false;
        }
    }

    private static byte[] withLeadingNewline(byte[] line) {
        byte[] prefixed = new byte[line.length + 1];
        prefixed[0] = '\n';
        System.arraycopy(line, 0, prefixed, 1, line.length);
        return prefixed;
    }

    /**
     * Writes {@code <target>.tmp} next to the target, forces it to disk, then moves it over the target; a plain
     * replacing move is the fallback on file systems that refuse atomic moves. When the write or the move fails, the
     * temporary file is deleted and the failure rethrown.
     */
    public static void writeAtomically(Path target, byte[] content) throws IOException {
        createParent(target);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                // On disk before the move, so a power loss leaves the old file or the whole new one, never an empty one.
                channel.force(true);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
    }

    /**
     * Finishes the queued writes, waiting up to 10 seconds. Later writes run at once on the caller's thread, so a
     * save during shutdown is not lost. Every write comes from the server thread, which also calls this method,
     * so once the queue has drained a synchronous write can never overtake a queued one; after the timeout or an
     * interrupt a queued write may still land after it. Only the writer's own thread is drained: an executor
     * passed to the constructor is its caller's to finish.
     */
    @Override
    public void close() {
        closed = true;
        if (ownThread == null) {
            return;
        }
        ownThread.shutdown();
        try {
            if (!ownThread.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warning("Files were still being written after " + CLOSE_TIMEOUT_SECONDS
                        + " seconds; the last writes may be missing");
            }
        } catch (InterruptedException e) {
            logger.warning("Interrupted while waiting for files to be written; the last writes may be missing");
            Thread.currentThread().interrupt();
        }
    }

    private void submit(Path target, IoAction action) {
        Runnable task = () -> run(target, action);
        if (closed) {
            task.run();
            return;
        }
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            // close() shut the thread down after the check above: write here rather than lose the data.
            task.run();
        }
    }

    private void run(Path target, IoAction action) {
        try {
            action.run();
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not write " + target + ": " + e, e);
        }
    }

    private static void createParent(Path target) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static Thread newThread(Runnable task) {
        Thread thread = new Thread(task, "MCME-PvP writer");
        thread.setDaemon(true); // never keeps the server from stopping
        return thread;
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
