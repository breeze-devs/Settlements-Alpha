package dev.breezes.settlements.domain.crafting.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * Datapack codec for {@link CraftOutput}. The output is always a concrete item, never a tag.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CraftOutputCodec {

    public static final Codec<CraftOutput> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("item").forGetter(CraftOutput::itemId),
                    Codec.INT.fieldOf("count").forGetter(CraftOutput::count)
            ).apply(instance, CraftOutput::new));

}
