package dev.breezes.settlements;

import com.mojang.logging.LogUtils;
import dev.breezes.settlements.configuration.Config;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.CreativeTabRegistry;
import dev.breezes.settlements.registry.EntityRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SettlementsMod.MOD_ID)
public final class SettlementsMod {

    public static final String MOD_ID = "settlements";
    public static final String MOD_NAME = "Settlements";

    private static final Logger LOGGER = LogUtils.getLogger();


    public SettlementsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Link our registries to the mod event bus
        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(BlockRegistry.CAT_MINT.getId(), BlockRegistry.POTTED_CAT_MINT);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        LOGGER.info("HELLO FROM ADD CREATIVE");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

}
