package com.azuredoom.classescore.util;

import com.hypixel.hytale.server.core.Message;

import java.util.function.Consumer;

/**
 * Utility class for handling translation operations. Provides methods to translate message keys into {@code Message}
 * objects and optionally apply custom modifications to the translated messages.
 */
public final class TranslationUtil {

    private TranslationUtil() {}

    /**
     * Translates a message key into a {@code Message} object.
     *
     * @param key the key representing the message to be translated; must not be null
     * @return the translated {@code Message} object corresponding to the given key
     */
    public static Message translate(String key) {
        return Message.translation(key);
    }

    /**
     * Translates a message key into a {@code Message} object and allows additional modifications to the message using a
     * consumer.
     *
     * @param key     the key representing the message to be translated; must not be null
     * @param builder a {@code Consumer} to apply custom modifications to the translated {@code Message}; must not be
     *                null
     * @return the translated {@code Message} after applying modifications from the consumer
     */
    public static Message translate(String key, Consumer<Message> builder) {
        var msg = Message.translation(key);
        builder.accept(msg);
        return msg;
    }
}
