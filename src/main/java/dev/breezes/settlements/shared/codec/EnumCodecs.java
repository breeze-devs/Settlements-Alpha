package dev.breezes.settlements.shared.codec;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Shared lenient enum {@link Codec} construction for persisted runtime state.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumCodecs {

    /**
     * Builds a codec that never errors: an unknown or renamed constant name silently decodes to
     * {@code fallback} instead of failing the surrounding record decode.
     */
    public static <E extends Enum<E>> Codec<E> lenient(Class<E> type, E fallback) {
        return Codec.STRING.xmap(name -> valueOfOr(type, name, fallback), Enum::name);
    }

    private static <E extends Enum<E>> E valueOfOr(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

}
