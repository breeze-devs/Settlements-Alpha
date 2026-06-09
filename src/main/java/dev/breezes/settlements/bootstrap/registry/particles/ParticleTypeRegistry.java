package dev.breezes.settlements.bootstrap.registry.particles;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

}
