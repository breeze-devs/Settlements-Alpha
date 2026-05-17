package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.presentation.ui.debug.DebugPoseOverlayScreen;
import dev.breezes.settlements.presentation.ui.keybindings.SettlementsKeyMappings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugPoseOverlayClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        while (SettlementsKeyMappings.OPEN_DEBUG_POSE_OVERLAY.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }

            minecraft.setScreen(new DebugPoseOverlayScreen(SettlementsDagger.client().debugPoseOverride()));
            break;
        }
    }

}
