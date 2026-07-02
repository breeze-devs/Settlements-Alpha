package dev.breezes.settlements.domain.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link BiomeId}. {@link BiomeId#of(String)} throws on a malformed
 * "namespace:path" string; the codec surfaces that as a decode error so a malformed biome id
 * fails just the entry that references it.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BiomeIdCodec {

    public static final Codec<BiomeId> CODEC = Codec.STRING.comapFlatMap(BiomeIdCodec::parse, BiomeId::full);

    private static DataResult<BiomeId> parse(String raw) {
        try {
            return DataResult.success(BiomeId.of(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Invalid biome id '" + raw + "': " + exception.getMessage());
        }
    }

}
