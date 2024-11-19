package dev.breezes.settlements;

import dev.breezes.settlements.configurations.annotations.processors.ConfigAnnotationProcessor;
import dev.breezes.settlements.registry.CreativeTabRegistry;
import dev.breezes.settlements.registry.EntityRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

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

        // Generate configuration files from annotations
        ConfigAnnotationProcessor.process();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        // TODO: adapt annotations so that we can specify files and also rename this file
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigAnnotationProcessor.SPEC, "settlements-annotations.toml");
    }

}
