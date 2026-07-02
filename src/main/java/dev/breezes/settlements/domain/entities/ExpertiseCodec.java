package dev.breezes.settlements.domain.entities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link Expertise}. Decoding is case-insensitive (matches
 * {@link Expertise#fromString}); unknown names fail the decode rather than silently defaulting,
 * per the strict-decoding policy for all keyed catalogs.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExpertiseCodec {

    public static final Codec<Expertise> CODEC = Codec.STRING.comapFlatMap(
            ExpertiseCodec::parse,
            Expertise::getConfigName);

    private static DataResult<Expertise> parse(String raw) {
        try {
            return DataResult.success(Expertise.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown expertise tier '" + raw + "'");
        }
    }

}
