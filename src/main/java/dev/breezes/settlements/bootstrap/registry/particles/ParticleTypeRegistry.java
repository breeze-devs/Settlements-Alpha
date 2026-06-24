package dev.breezes.settlements.bootstrap.registry.particles;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.rendering.particles.OrbParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ParticleTypeRegistry {

    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<SimpleParticleType> EGG_SPLAT =
            REGISTRY.register("egg_splat", () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> STUNNED_STAR =
            REGISTRY.register("stunned_star", () -> new SimpleParticleType(false));

    /**
     * Colorable magic orb particle. Carries color + motion mode via {@link OrbParticleOptions}
     * so a single registered type covers all colors and motion styles.
     */
    public static final Supplier<ParticleType<OrbParticleOptions>> ORB =
            REGISTRY.register("orb", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<OrbParticleOptions> codec() {
                    return OrbParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, OrbParticleOptions> streamCodec() {
                    return OrbParticleOptions.STREAM_CODEC;
                }
            });

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

}
