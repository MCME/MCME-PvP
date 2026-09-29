package com.mcmiddleearth.pvp;

import java.util.Map;

/**
 * A request the plugin refuses, with a reason for the player: {@code messageKey} is a key in
 * messages.yml (see {@link com.mcmiddleearth.pvp.text.Msg}) and {@code args} fills its
 * {@code <name>} placeholders. The engine throws it; commands and menus turn it into chat.
 */
public final class PvpException extends RuntimeException {

    private final String messageKey;
    private final Map<String, String> args;

    public PvpException(String messageKey, Map<String, String> args) {
        super(messageKey + " " + args);
        this.messageKey = messageKey;
        this.args = Map.copyOf(args);
    }

    public PvpException(String messageKey) {
        this(messageKey, Map.of());
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, String> args() {
        return args;
    }
}
