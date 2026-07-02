package dev.breezes.settlements.domain.generation.model.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.generation.model.building.DisplayInfoCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

/**
 * Datapack codec for {@link TraitDefinition}. {@code display_info} is the only optional field.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraitDefinitionCodec {

    public static final Codec<TraitDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TraitIdCodec.CODEC.fieldOf("id").forGetter(TraitDefinition::id),
                    DisplayInfoCodec.CODEC.optionalFieldOf("display_info").forGetter(
                            definition -> Optional.ofNullable(definition.displayInfo()))
            ).apply(instance, (id, displayInfo) -> new TraitDefinition(id, displayInfo.orElse(null))));

}
