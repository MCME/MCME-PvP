package com.mcmiddleearth.pvp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** A logger for tests: it keeps every record for assertions and prints nothing. Safe to use from several threads. */
public final class RecordingLog {

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getAnonymousLogger();

    public RecordingLog() {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    public Logger logger() {
        return logger;
    }

    /** The messages logged at exactly {@code level}, oldest first. */
    public List<String> messages(Level level) {
        return records.stream()
                .filter(record -> record.getLevel().equals(level))
                .map(LogRecord::getMessage)
                .toList();
    }
}
