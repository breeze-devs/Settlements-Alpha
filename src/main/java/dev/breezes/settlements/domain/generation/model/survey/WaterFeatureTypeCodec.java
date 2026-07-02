package dev.breezes.settlements.domain.generation.model.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link WaterFeatureType}. Unknown names fail the decode (strict policy)
 * rather than silently dropping the offending element while keeping the rest of the entry.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WaterFeatureTypeCodec {

    public static final Codec<WaterFeatureType> CODEC = Codec.STRING.comapFlatMap(WaterFeatureTypeCodec::parse, Enum::name);

    private static DataResult<WaterFeatureType> parse(String raw) {
        try {
            return DataResult.success(WaterFeatureType.valueOf(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown water feature type '" + raw + "'");
        }
    }

}
