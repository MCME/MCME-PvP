package com.mcmiddleearth.pvp.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Jackson mappers for the plugin's own files. Unknown properties are ignored so older builds can read newer files. */
public final class Json {

    private Json() {
    }

    /** Pretty-printed output, for arena and player files. */
    public static ObjectMapper pretty() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** One-line output, for the match history (one JSON object per line). */
    public static ObjectMapper compact() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
