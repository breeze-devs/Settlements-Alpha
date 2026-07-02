package dev.breezes.settlements.domain.generation.history;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitIdCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Datapack codec for {@link HistoryEventDefinition}. {@code preconditions} defaults to
 * {@link EventPreconditions#NONE} and {@code probability_weight} defaults to {@code 1.0}, matching
 * the defaults the hand-rolled parser used to apply.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class HistoryEventDefinitionCodec {

    public static final Codec<HistoryEventDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(HistoryEventDefinition::id),
                    Codec.STRING.fieldOf("category").forGetter(HistoryEventDefinition::category),
                    Codec.INT.fieldOf("time_horizon_min").forGetter(HistoryEventDefinition::timeHorizonMin),
                    Codec.INT.fieldOf("time_horizon_max").forGetter(HistoryEventDefinition::timeHorizonMax),
                    Codec.STRING.listOf().optionalFieldOf("exclusive_tags", List.of()).forGetter(HistoryEventDefinition::exclusiveTags),
                    Codec.FLOAT.optionalFieldOf("probability_weight", 1.0f).forGetter(HistoryEventDefinition::probabilityWeight),
                    EventPreconditionsCodec.CODEC.optionalFieldOf("preconditions", EventPreconditions.NONE).forGetter(HistoryEventDefinition::preconditions),
                    Codec.unboundedMap(TraitIdCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("trait_modifiers", Map.<TraitId, Float>of())
                            .forGetter(HistoryEventDefinition::traitModifiers),
                    Codec.STRING.listOf().optionalFieldOf("visual_markers", List.of()).forGetter(HistoryEventDefinition::visualMarkers),
                    Codec.STRING.fieldOf("narrative_text").forGetter(HistoryEventDefinition::narrativeText)
            ).apply(instance, HistoryEventDefinition::new));

}
