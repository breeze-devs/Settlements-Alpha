package dev.breezes.settlements;

import dev.breezes.settlements.infrastructure.config.annotations.ConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.network.core.PacketRegistry;
import dev.breezes.settlements.bootstrap.registry.tabs.CreativeTabRegistry;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
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
//        BlockRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);

        processAnnotations(modEventBus);
    }

    private void processAnnotations(@Nonnull IEventBus modEventBus) {
        // Generate configuration files from annotations
        ConfigAnnotationProcessor.process();

        // Bind packet handlers from annotations
        modEventBus.addListener(PacketRegistry::bindPacketHandlers);
    }

}
