package dev.breezes.settlements.event;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.registry.EntityRegistry;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@CustomLog
public class CommonModEvents {

    // TODO: refactor this!!! ugly
    public static final List<Runnable> LOAD_COMPLETE_TASKS = new ArrayList<>();

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.BASE_VILLAGER.get(), BaseVillager.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(@Nonnull FMLCommonSetupEvent event) {
        // Register packets
        event.enqueueWork(() -> {
//            log.debug("Registering packets...");
//            PacketHandler.registerPackets();
//            log.debug("Packet registration complete");

            // TODO: add more common setup tasks here
        });
    }

    @SubscribeEvent
    public static void onLoadComplete(@Nonnull FMLLoadCompleteEvent event) {
        log.debug("Running load complete tasks...");
        LOAD_COMPLETE_TASKS.forEach(Runnable::run);
        log.debug("Load complete tasks complete");
    }

}
