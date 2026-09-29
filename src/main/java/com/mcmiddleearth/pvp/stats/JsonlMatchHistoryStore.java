package com.mcmiddleearth.pvp.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmiddleearth.pvp.storage.AsyncWriter;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * Match records as JSON Lines, one file per month: {@code matches/<yyyy-MM>.jsonl}, one compact JSON object per
 * line. The month comes from {@code endedAt} in UTC, so the file does not depend on the server's time zone.
 */
public final class JsonlMatchHistoryStore implements MatchHistoryStore {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    private final Path matchesDir;
    private final ObjectMapper compactMapper;
    private final AsyncWriter writer;

    /** {@code compactMapper} must write one line ({@link com.mcmiddleearth.pvp.storage.Json#compact()}). */
    public JsonlMatchHistoryStore(Path matchesDir, ObjectMapper compactMapper, AsyncWriter writer) {
        this.matchesDir = matchesDir;
        this.compactMapper = compactMapper;
        this.writer = writer;
    }

    @Override
    public void append(MatchRecord record) {
        byte[] json;
        try {
            json = compactMapper.writeValueAsBytes(record);
        } catch (JsonProcessingException e) {
            // A record of strings, numbers, booleans and lists always serializes; failing here is a programming error.
            throw new IllegalStateException("match record " + record.gameId() + " could not be serialized", e);
        }
        byte[] line = Arrays.copyOf(json, json.length + 1);
        line[json.length] = '\n';
        writer.append(fileFor(record), line);
    }

    /** {@code matches/<yyyy-MM>.jsonl} of the month the game ended in, in UTC. */
    private Path fileFor(MatchRecord record) {
        Instant endedAt;
        try {
            endedAt = Instant.parse(record.endedAt());
        } catch (DateTimeParseException e) {
            // A record built by this plugin always has a parseable endedAt; failing here is a programming error.
            throw new IllegalStateException(
                    "match record " + record.gameId() + " has a malformed endedAt: " + record.endedAt(), e);
        }
        return matchesDir.resolve(MONTH.format(endedAt) + ".jsonl");
    }
}
