package dev.breezes.settlements.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.custom.RhinoEntity;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.registry.EntityRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.RHINO.get(), RhinoEntity.createAttributes().build());
        event.put(EntityRegistry.BASE_VILLAGER.get(), BaseVillager.createAttributes().build());
    }

}
