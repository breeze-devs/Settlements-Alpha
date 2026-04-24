package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugRenderer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementDebugClientEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(@Nonnull RenderLevelStageEvent event) {
        SettlementDebugOverlayState settlementDebugOverlayState = SettlementsDagger.client().settlementDebugOverlayState();
        SettlementDebugRenderer.render(event, settlementDebugOverlayState);
    }

}
