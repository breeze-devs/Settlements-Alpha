package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.client.VillagerFishingHookRenderer;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.VanillaVillagerModel;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering.SettlementsVillagerRenderer;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WolfRenderer;
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
        EntityRenderers.register(EntityRegistry.BASE_VILLAGER.get(), SettlementsVillagerRenderer::new);
        EntityRenderers.register(EntityRegistry.SETTLEMENTS_CAT.get(), CatRenderer::new);
        EntityRenderers.register(EntityRegistry.SETTLEMENTS_WOLF.get(), WolfRenderer::new);
        EntityRenderers.register(EntityRegistry.VILLAGER_FISHING_HOOK.get(), VillagerFishingHookRenderer::new);

        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }


    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VanillaVillagerModel.LAYER, VanillaVillagerModel::createBodyLayer);
    }

}
