package dev.breezes.settlements.domain.generation.model.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link TraitSlot}. Unknown names fail the decode (strict policy) rather
 * than silently dropping the offending element while keeping the rest of the entry.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraitSlotCodec {

    public static final Codec<TraitSlot> CODEC = Codec.STRING.comapFlatMap(TraitSlotCodec::parse, Enum::name);

    private static DataResult<TraitSlot> parse(String raw) {
        try {
            return DataResult.success(TraitSlot.valueOf(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown trait slot '" + raw + "'");
        }
    }

}
