package dev.breezes.settlements.event;

import dev.breezes.settlements.registry.EntityRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // TODO: replace with custom model when ready
        EntityRenderers.register(EntityRegistry.BASE_VILLAGER.get(), VillagerRenderer::new);

        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }


    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
//        event.registerLayerDefinition(BaseVillagerModel.LAYER, BaseVillagerModel::createBodyLayer);
    }

}
