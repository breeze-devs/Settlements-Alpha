package dev.breezes.settlements.shared.codec;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import java.util.Optional;

/**
 * Shared lenient enum {@link Codec} construction for persisted runtime state.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public final class EnumCodecs {

    /**
     * Builds a codec that never errors: an unknown or renamed constant name silently decodes to
     * {@code fallback} instead of failing the surrounding record decode.
     */
    public static <E extends Enum<E>> Codec<E> lenient(Class<E> type, E fallback) {
        return Codec.STRING.xmap(name -> constant(type, name).orElse(fallback), Enum::name);
    }

    /**
     * Builds a codec that never errors: an unknown or renamed constant name decodes to fallback.
     */
    public static <E extends Enum<E>> Codec<E> lenient(Class<E> type, E fallback, String fieldLabel) {
        return Codec.STRING.xmap(name -> constant(type, name).orElseGet(() -> {
            log.warn("Unknown {} '{}' read as {}", fieldLabel, name, fallback);
            return fallback;
        }), Enum::name);
    }

    private static <E extends Enum<E>> Optional<E> constant(Class<E> type, String name) {
        try {
            return Optional.of(Enum.valueOf(type, name));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
