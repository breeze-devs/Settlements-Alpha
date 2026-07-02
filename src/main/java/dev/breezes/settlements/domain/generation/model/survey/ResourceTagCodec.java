package dev.breezes.settlements.domain.generation.model.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link ResourceTag}. Unknown names fail the decode (strict policy) rather
 * than silently dropping the offending element while keeping the rest of the entry.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceTagCodec {

    public static final Codec<ResourceTag> CODEC = Codec.STRING.comapFlatMap(ResourceTagCodec::parse, Enum::name);

    private static DataResult<ResourceTag> parse(String raw) {
        try {
            return DataResult.success(ResourceTag.valueOf(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown resource tag '" + raw + "'");
        }
    }

}
