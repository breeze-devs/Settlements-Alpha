package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.DaggerSettlementsComponent;
import dev.breezes.settlements.di.SettlementsComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.SettlementsCat;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import lombok.CustomLog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
        event.put(EntityRegistry.BASE_VILLAGER.get(), BaseVillager.createCustomAttributes());
        event.put(EntityRegistry.SETTLEMENTS_CAT.get(), SettlementsCat.createAttributes().build());
        event.put(EntityRegistry.SETTLEMENTS_WOLF.get(), SettlementsWolf.createAttributes().build());
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
        log.info("Running load complete tasks");
        LOAD_COMPLETE_TASKS.forEach(Runnable::run);

        // Config factories are populated during load-complete tasks, so the root
        // component must be created afterwards to observe the finalized startup state.
        SettlementsComponent component = DaggerSettlementsComponent.create();
        SettlementsDagger.initialize(component);
        registerReloadListeners(component);

        log.info("Root dagger component initialized after load complete");

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientComponent clientComponent = component.clientComponentFactory().create();
            SettlementsDagger.initializeClient(clientComponent);
            log.info("Client subcomponent initialized after root component");
        }

        log.info("Load complete tasks complete");
    }

    private static void registerReloadListeners(@Nonnull SettlementsComponent component) {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            log.info("Registering Dagger-managed data reload listeners");

            // These listeners must come from the root component because AddReloadListenerEvent
            // fires before the server subcomponent exists for a given world/session.
            event.addListener(component.enchantmentCostDataManager());
            event.addListener(component.specializationDataManager());
            event.addListener(component.fishCatchDataManager());
            event.addListener(component.biomeSurveyDataManager());
            event.addListener(component.traitDefinitionDataManager());
            event.addListener(component.traitScorerDataManager());
            event.addListener(component.historyEventDataManager());
            event.addListener(component.buildingDefinitionDataManager());
            event.addListener(component.generationDataValidationReloadListener());
            event.addListener(component.settlementTemplateReloadListener());
        });
    }

}
