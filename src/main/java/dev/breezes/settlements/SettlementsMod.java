package dev.breezes.settlements;

import dev.breezes.settlements.bootstrap.registry.activities.ActivityRegistry;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.bootstrap.registry.blocks.BlockRegistry;
import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.bootstrap.registry.memory.MemoryModuleTypeRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import dev.breezes.settlements.bootstrap.registry.schedules.ScheduleRegistry;
import dev.breezes.settlements.bootstrap.registry.sensors.SensorTypeRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundEventRegistry;
import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.bootstrap.registry.tabs.CreativeTabRegistry;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.network.core.PacketRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

import javax.annotation.Nonnull;

@Mod(SettlementsMod.MOD_ID)
public final class SettlementsMod {

    public static final String MOD_ID = "settlements";
    public static final String MOD_NAME = "Settlements";

    public SettlementsMod() {
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();

        // Link our registries to the mod event bus
        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        ParticleTypeRegistry.register(modEventBus);
        DataComponentRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        MemoryModuleTypeRegistry.register(modEventBus);
        SensorTypeRegistry.register(modEventBus);
        SoundEventRegistry.register(modEventBus);
        StructureRegistry.register(modEventBus);
        ActivityRegistry.register(modEventBus);
        ScheduleRegistry.register(modEventBus);

        processAnnotations(modEventBus);
    }

    private void processAnnotations(@Nonnull IEventBus modEventBus) {
        // Generate configuration files from annotations
        ConfigAnnotationProcessor.process();

        // Bind packet handlers from annotations
        modEventBus.addListener(PacketRegistry::bindPacketHandlers);
    }

}
