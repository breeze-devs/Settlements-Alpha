package dev.breezes.settlements.domain.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Datapack codec for {@link OreRegenEntry}. {@code host} is optional and defaults to
 * {@link OreRegenEntry.HostFilter#ANY}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class OreRegenEntryCodec {

    private static final Codec<Double> POSITIVE_WEIGHT = Codec.DOUBLE.comapFlatMap(
            weight -> weight > 0
                    ? DataResult.success(weight)
                    : DataResult.error(() -> "weight must be > 0, got " + weight),
            Function.identity());

    public static final Codec<OreRegenEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("block").forGetter(OreRegenEntry::getBlockId),
                    POSITIVE_WEIGHT.fieldOf("weight").forGetter(OreRegenEntry::getWeight),
                    OreRegenEntry.HostFilter.CODEC.optionalFieldOf("host", OreRegenEntry.HostFilter.ANY).forGetter(OreRegenEntry::getHost)
            ).apply(instance, (blockId, weight, host) -> OreRegenEntry.builder()
                    .blockId(blockId)
                    .weight(weight)
                    .host(host)
                    .build()));

}
