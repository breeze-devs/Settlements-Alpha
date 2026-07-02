package dev.breezes.settlements.domain.generation.history;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitIdCodec;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTagCodec;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureTypeCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * Datapack codec for {@link EventPreconditions}. Every field is optional, defaulting to the same
 * empty/zero values the compact constructor already falls back to for a {@code null} argument.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventPreconditionsCodec {

    public static final Codec<EventPreconditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(TraitIdCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("min_trait_weights", Map.<TraitId, Float>of())
                            .forGetter(EventPreconditions::minTraitWeights),
                    ResourceTagCodec.CODEC.listOf()
                            .xmap(Set::copyOf, list -> list.stream().toList())
                            .optionalFieldOf("required_resource_tags", Set.<ResourceTag>of())
                            .forGetter(EventPreconditions::requiredResourceTags),
                    WaterFeatureTypeCodec.CODEC.listOf()
                            .xmap(Set::copyOf, list -> list.stream().toList())
                            .optionalFieldOf("required_water_features", Set.<WaterFeatureType>of())
                            .forGetter(EventPreconditions::requiredWaterFeatures),
                    Codec.INT.optionalFieldOf("min_population", 0).forGetter(EventPreconditions::minPopulation)
            ).apply(instance, EventPreconditions::new));

}
