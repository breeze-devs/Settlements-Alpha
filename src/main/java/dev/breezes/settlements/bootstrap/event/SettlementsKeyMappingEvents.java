package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.rendering.debug.DevTooling;
import dev.breezes.settlements.presentation.ui.keybindings.SettlementsKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Central registration point for all Settlements key mappings.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SettlementsKeyMappingEvents {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (DevTooling.isEnabled()) {
            event.register(SettlementsKeyMappings.OPEN_VILLAGER_STATS);
            event.register(SettlementsKeyMappings.OPEN_DAY_PLAN);
            event.register(SettlementsKeyMappings.TOGGLE_DEBUG_TUNING_OVERLAY);
        }
    }

}
