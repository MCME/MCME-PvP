package com.mcmiddleearth.pvp.arena;

import java.util.List;
import java.util.Objects;

/** Where an arena came from. {@code importedAt} is ISO-8601 ({@code Instant.toString()}); {@code legacyGmType} is nullable. */
public record ImportInfo(String sourceFile, String sha256, String importedAt, String legacyGmType, List<String> unknownProperties) {
    public ImportInfo {
        unknownProperties = List.copyOf(Objects.requireNonNull(unknownProperties, "\"unknownProperties\" is missing; write [] for none"));
    }
}
