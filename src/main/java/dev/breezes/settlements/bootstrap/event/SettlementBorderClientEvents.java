package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementBorderClientEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(@Nonnull RenderLevelStageEvent event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent == null) {
            return;
        }

        clientComponent.settlementBorderRenderer().render(event);
    }

}
