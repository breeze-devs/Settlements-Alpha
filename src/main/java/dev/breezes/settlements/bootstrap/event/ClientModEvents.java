package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.client.VillagerFishingHookRenderer;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.SettlementsVillagerModel;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering.SettlementsVillagerRenderer;
import dev.breezes.settlements.infrastructure.rendering.particles.EggSplatParticle;
import dev.breezes.settlements.infrastructure.rendering.particles.StunnedStarParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import javax.annotation.Nullable;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        registerEntityRenderers();
        registerItemProperties(event);
        registerConfigScreen();
    }

    private static void registerEntityRenderers() {
        EntityRenderers.register(EntityRegistry.BASE_VILLAGER.get(), SettlementsVillagerRenderer::new);
        EntityRenderers.register(EntityRegistry.SETTLEMENTS_CAT.get(), CatRenderer::new);
        EntityRenderers.register(EntityRegistry.SETTLEMENTS_WOLF.get(), WolfRenderer::new);
        EntityRenderers.register(EntityRegistry.VILLAGER_FISHING_HOOK.get(), VillagerFishingHookRenderer::new);
        EntityRenderers.register(EntityRegistry.SETTLEMENTS_EGG.get(), ThrownItemRenderer::new);
    }

    private static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ItemRegistry.VILLAGER_FISHING_ROD.get(),
                ResourceLocation.withDefaultNamespace("cast"),
                ClientModEvents::villagerFishingRodCastPredicate));
    }

    private static float villagerFishingRodCastPredicate(ItemStack stack,
                                                         @Nullable ClientLevel level,
                                                         @Nullable LivingEntity entity,
                                                         int seed) {
        if (entity instanceof BaseVillager villager && villager.isBobberDeployed()) {
            return 1.0F;
        }
        return 0.0F;
    }

    private static void registerConfigScreen() {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }


    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SettlementsVillagerModel.LAYER, SettlementsVillagerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypeRegistry.EGG_SPLAT.get(), EggSplatParticle.Provider::new);
        event.registerSpriteSet(ParticleTypeRegistry.STUNNED_STAR.get(), StunnedStarParticle.Provider::new);
    }

}
