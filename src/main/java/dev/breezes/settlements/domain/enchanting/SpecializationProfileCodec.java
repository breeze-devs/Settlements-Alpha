package dev.breezes.settlements.domain.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Datapack codec for {@link SpecializationProfile}. {@code description} defaults to an empty
 * string and {@code enchantment_weights} defaults to an empty map, matching the old
 * {@code @Builder.Default} behavior.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpecializationProfileCodec {

    public static final Codec<SpecializationProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(SpecializationProfile::id),
                    Codec.STRING.fieldOf("display_name").forGetter(SpecializationProfile::displayName),
                    Codec.STRING.optionalFieldOf("description", "").forGetter(SpecializationProfile::description),
                    Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                            .optionalFieldOf("enchantment_weights", Map.of())
                            .forGetter(SpecializationProfile::enchantmentWeights)
            ).apply(instance, SpecializationProfile::new));

}
