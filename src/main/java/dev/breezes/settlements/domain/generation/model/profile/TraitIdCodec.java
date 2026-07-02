package dev.breezes.settlements.domain.generation.model.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link TraitId}. {@link TraitId#of(String)} throws on a malformed
 * "namespace:path" string; the codec surfaces that as a decode error instead of an exception
 * escaping the reload loop, so a malformed trait id fails just the entry that references it.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraitIdCodec {

    public static final Codec<TraitId> CODEC = Codec.STRING.comapFlatMap(TraitIdCodec::parse, TraitId::full);

    private static DataResult<TraitId> parse(String raw) {
        try {
            return DataResult.success(TraitId.of(raw));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Invalid trait id '" + raw + "': " + exception.getMessage());
        }
    }

}
