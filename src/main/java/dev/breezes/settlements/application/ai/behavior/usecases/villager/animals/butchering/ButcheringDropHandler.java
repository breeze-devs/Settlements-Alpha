package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.CustomLog;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.UUID;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@CustomLog
public class ButcheringDropHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // Check if a villager has reserved the drops
        boolean hasReservedVillager = event.getEntity().getPersistentData().hasUUID(ButcherLivestockBehavior.RESERVED_FOR_VILLAGER_KEY);
        if (!hasReservedVillager) {
            return;
        }

        UUID villagerUuid = event.getEntity().getPersistentData().getUUID(ButcherLivestockBehavior.RESERVED_FOR_VILLAGER_KEY);
        log.behaviorStatus("Reserving {} drops from {} for villager {}",
                event.getDrops().size(), event.getEntity().getClass().getSimpleName(), villagerUuid);

        for (ItemEntity drop : event.getDrops()) {
            drop.getPersistentData().putUUID(ButcherLivestockBehavior.RESERVED_FOR_VILLAGER_KEY, villagerUuid);
            drop.setPickUpDelay(ClockTicks.seconds(10).getTicksAsInt());
        }
    }

}
