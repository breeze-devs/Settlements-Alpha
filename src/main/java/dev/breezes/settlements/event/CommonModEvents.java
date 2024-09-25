package dev.breezes.settlements.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.packets.PacketHandler;
import dev.breezes.settlements.registry.EntityRegistry;
import lombok.CustomLog;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@CustomLog
public class CommonModEvents {

    // TODO: refactor this!!! ugly
    public static List<Runnable> loadCompleteTasks = new ArrayList<>();

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.BASE_VILLAGER.get(), BaseVillager.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(@Nonnull FMLCommonSetupEvent event) {
        // Register packets
        event.enqueueWork(() -> {
            log.debug("Registering packets...");
            PacketHandler.registerPackets();
            log.debug("Packet registration complete");

            // TODO: add more common setup tasks here
        });
    }

    @SubscribeEvent
    public static void onLoadComplete(@Nonnull FMLLoadCompleteEvent event) {
        log.debug("Running load complete tasks...");
        loadCompleteTasks.forEach(Runnable::run);
        log.debug("Load complete tasks complete");
    }

}
