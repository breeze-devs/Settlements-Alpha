package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;

/**
 * Bootstrap wiring between NeoForge's client event bus and the ballista's client services.
 */
@ClientSide
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaAimClientEvents {

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(@Nonnull InputEvent.InteractionKeyMappingTriggered event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimController().onInteractionKeyMappingTriggered(event);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolling(@Nonnull InputEvent.MouseScrollingEvent event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimController().onMouseScrolling(event);
        }
    }

    @SubscribeEvent
    public static void onKey(@Nonnull InputEvent.Key event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimController().onKey(event);
        }
    }

    @SubscribeEvent
    public static void onMouseButton(@Nonnull InputEvent.MouseButton.Post event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimController().onMouseButton(event);
        }
    }

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimController().onClientTick();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(@Nonnull RenderLevelStageEvent event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.ballistaAimGuideRenderer().render(event);
        }
    }

}
