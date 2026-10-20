package dev.breezes.settlements.domain.entities;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared codec for {@link VillagerProfessionKey}, written as the profession's full registry id (e.g.
 * "minecraft:shepherd"). An id read without a namespace takes "minecraft".
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerProfessionKeyCodec {

    public static final Codec<VillagerProfessionKey> CODEC =
            ResourceLocation.CODEC.xmap(VillagerProfessionKey::new, VillagerProfessionKey::id);

}
