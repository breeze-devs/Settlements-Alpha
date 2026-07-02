package dev.breezes.settlements.domain.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Datapack codec for {@link FishCatchEntry}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FishCatchEntryCodec {

    private static final Codec<Double> POSITIVE_WEIGHT = Codec.DOUBLE.comapFlatMap(
            weight -> weight > 0
                    ? DataResult.success(weight)
                    : DataResult.error(() -> "weight must be > 0, got " + weight),
            Function.identity());

    public static final Codec<FishCatchEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("entity").forGetter(FishCatchEntry::getEntityId),
                    ResourceLocation.CODEC.fieldOf("item").forGetter(FishCatchEntry::getItemId),
                    POSITIVE_WEIGHT.fieldOf("weight").forGetter(FishCatchEntry::getWeight)
            ).apply(instance, (entityId, itemId, weight) -> FishCatchEntry.builder()
                    .entityId(entityId)
                    .itemId(itemId)
                    .weight(weight)
                    .build()));

}
