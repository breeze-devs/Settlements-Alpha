package dev.breezes.settlements.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.client.ModModelLayers;
import dev.breezes.settlements.entities.client.RhinoModel;
import dev.breezes.settlements.entities.client.RhinoRenderer;
import dev.breezes.settlements.entities.villager.model.BaseVillagerModel;
import dev.breezes.settlements.entities.villager.model.rendering.BaseVillagerRenderer;
import dev.breezes.settlements.registry.EntityRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(EntityRegistry.RHINO.get(), RhinoRenderer::new);
        EntityRenderers.register(EntityRegistry.BASE_VILLAGER.get(), BaseVillagerRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.RHINO_LAYER, RhinoModel::createBodyLayer);
        event.registerLayerDefinition(BaseVillagerModel.LAYER, BaseVillagerModel::createBodyLayer);
    }


}
