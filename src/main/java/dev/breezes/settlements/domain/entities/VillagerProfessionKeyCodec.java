package dev.breezes.settlements.domain.entities;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared datapack codec for {@link VillagerProfessionKey}: JSON carries the full vanilla-style
 * resource location (e.g. {@code "minecraft:shepherd"}), while the domain key only keeps the path.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerProfessionKeyCodec {

    // The encode side re-attaches a "minecraft" namespace, which is lossy for non-vanilla
    // professions - acceptable because these keys are only ever decoded from datapacks, never
    // serialized back out.
    public static final Codec<VillagerProfessionKey> CODEC = ResourceLocation.CODEC.xmap(
            VillagerProfessionKey::fromResourceLocation,
            key -> key.toResourceLocation("minecraft"));

}
