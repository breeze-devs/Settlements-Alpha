package dev.breezes.settlements.shared.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Shared list Codec construction for persisted runtime state.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public final class ListCodecs {

    /**
     * Builds a list codec that keeps every element it can read and drops what it cannot.
     * <p>
     * An element is unreadable when its data fails to decode, and also when its type's constructor rejects the decoded
     * values with IllegalArgumentException or NullPointerException. An element that decodes partially is kept in that
     * partial form.
     * <p>
     * Apply it to the list itself, beneath any optional-field wrapping: a lenient optional field over a list that
     * reports an error substitutes its default for the whole list.
     *
     * @param elementLabel names one element in the log line, for example "villager knowledge entry"
     */
    public static <E> Codec<List<E>> lenient(Codec<E> elementCodec, String elementLabel) {
        // Every failing element was logged as it was decoded, so the list's combined error needs no second line
        return reportingElements(elementCodec, elementLabel).listOf().promotePartial(alreadyLogged -> {
        });
    }

    private static <E> Codec<E> reportingElements(Codec<E> codec, String elementLabel) {
        Decoder<E> decoder = new Decoder<>() {
            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                DataResult<Pair<E, T>> result;
                try {
                    result = codec.decode(ops, input);
                } catch (IllegalArgumentException | NullPointerException exception) {
                    // A precondition that throws mid-decode escapes the codec and aborts the decode of everything
                    // around the element, rather than dropping that one element
                    log.warn("Dropped a {} its type rejected: {}", elementLabel, exception.getMessage(), exception);
                    return DataResult.error(() -> "Rejected by its type: " + exception.getMessage());
                }

                if (result.isSuccess()) {
                    return result;
                }
                if (result.hasResultOrPartial()) {
                    return result.promotePartial(error -> log.warn("Kept a {} without its unreadable parts: {}",
                            elementLabel, error));
                }
                result.error().ifPresent(error -> log.warn("Dropped an unreadable {}: {}", elementLabel, error.message()));
                return result;
            }
        };
        return Codec.of(codec, decoder);
    }

}
