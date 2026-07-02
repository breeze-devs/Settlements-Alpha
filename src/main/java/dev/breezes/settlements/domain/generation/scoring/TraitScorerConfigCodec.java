package dev.breezes.settlements.domain.generation.scoring;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.common.BiomeIdCodec;
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
 * Datapack codec for {@link TraitScorerConfig}. Every weight map and tag set is optional and
 * defaults to empty, while the numeric fields default to {@code 0.0}. Unknown resource tags, water
 * features, biome ids or an invalid trait id fail the whole entry (strict policy) rather than being
 * silently dropped.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraitScorerConfigCodec {

    public static final Codec<TraitScorerConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TraitIdCodec.CODEC.fieldOf("trait").forGetter(TraitScorerConfig::trait),
                    Codec.FLOAT.optionalFieldOf("base_score", 0.0f).forGetter(TraitScorerConfig::baseScore),
                    Codec.unboundedMap(ResourceTagCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("resource_tag_weights", Map.<ResourceTag, Float>of())
                            .forGetter(TraitScorerConfig::resourceTagWeights),
                    ResourceTagCodec.CODEC.listOf().xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("required_tags", Set.<ResourceTag>of())
                            .forGetter(TraitScorerConfig::requiredTags),
                    ResourceTagCodec.CODEC.listOf().xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("veto_tags", Set.<ResourceTag>of())
                            .forGetter(TraitScorerConfig::vetoTags),
                    Codec.unboundedMap(WaterFeatureTypeCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("water_feature_weights", Map.<WaterFeatureType, Float>of())
                            .forGetter(TraitScorerConfig::waterFeatureWeights),
                    Codec.unboundedMap(BiomeIdCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("biome_weights", Map.<BiomeId, Float>of())
                            .forGetter(TraitScorerConfig::biomeWeights),
                    Codec.FLOAT.optionalFieldOf("elevation_delta_weight", 0.0f).forGetter(TraitScorerConfig::elevationDeltaWeight),
                    Codec.FLOAT.optionalFieldOf("elevation_delta_normalization", 0.0f).forGetter(TraitScorerConfig::elevationDeltaNormalization)
            ).apply(instance, TraitScorerConfig::new));

}
