package dev.breezes.settlements.domain.generation.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.common.BiomeIdCodec;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTagCodec;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureTypeCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Datapack codec for {@link BiomeSurveyDefinition}. {@code water_type} is the only nullable field
 * on {@link BiomeSurveyData}, expressed as absence (not a JSON {@code null}) per the strict
 * decoding policy.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BiomeSurveyDefinitionCodec {

    public static final Codec<BiomeSurveyDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeIdCodec.CODEC.fieldOf("biome").forGetter(BiomeSurveyDefinition::biome),
                    Codec.unboundedMap(ResourceTagCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("resource_densities", Map.of())
                            .forGetter(definition -> definition.survey().resourceDensities()),
                    WaterFeatureTypeCodec.CODEC.optionalFieldOf("water_type")
                            .forGetter(definition -> Optional.ofNullable(definition.survey().waterType())),
                    Codec.STRING.listOf()
                            .xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("template_tags", Set.of())
                            .forGetter(definition -> definition.survey().templateTags())
            ).apply(instance, (biome, resourceDensities, waterType, templateTags) ->
                    new BiomeSurveyDefinition(biome, new BiomeSurveyData(resourceDensities, waterType.orElse(null), templateTags))));

}
