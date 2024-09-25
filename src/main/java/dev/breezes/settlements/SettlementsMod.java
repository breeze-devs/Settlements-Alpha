package dev.breezes.settlements;

import dev.breezes.settlements.configurations.GeneralConfig;
import dev.breezes.settlements.configurations.annotations.processors.ConfigAnnotationProcessor;
import dev.breezes.settlements.registry.CreativeTabRegistry;
import dev.breezes.settlements.registry.EntityRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SettlementsMod.MOD_ID)
public final class SettlementsMod {

    public static final String MOD_ID = "settlements";
    public static final String MOD_NAME = "Settlements";

    public SettlementsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Link our registries to the mod event bus
        ItemRegistry.register(modEventBus);
//        BlockRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);

        // Generate configuration files from annotations
        ConfigAnnotationProcessor.process();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC);
        // TODO: adapt annotations so that we can specify files and also rename this file
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigAnnotationProcessor.SPEC, "settlements-annotations.toml");
    }

}
