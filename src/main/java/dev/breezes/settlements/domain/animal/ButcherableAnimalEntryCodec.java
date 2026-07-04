package dev.breezes.settlements.domain.animal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ButcherableAnimalEntryCodec {

    public static final Codec<ButcherableAnimalEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("entity").forGetter(ButcherableAnimalEntry::entityId),
                    Codec.INT.fieldOf("minimum_keep_count").forGetter(ButcherableAnimalEntry::minimumKeepCount),
                    Codec.STRING.optionalFieldOf("species_noun").forGetter(entry -> Optional.ofNullable(entry.speciesNoun()))
            ).apply(instance, (entityId, minimumKeepCount, speciesNoun) -> ButcherableAnimalEntry.builder()
                    .entityId(entityId)
                    .minimumKeepCount(minimumKeepCount)
                    .speciesNoun(speciesNoun.orElse(null))
                    .build()));

}
