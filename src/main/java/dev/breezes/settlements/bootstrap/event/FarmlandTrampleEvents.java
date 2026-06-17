package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.entities.SettlementsEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class FarmlandTrampleEvents {

    @SubscribeEvent
    public static void onFarmlandTrample(@Nonnull BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof SettlementsEntity) {
            event.setCanceled(true);
        }
    }

}
