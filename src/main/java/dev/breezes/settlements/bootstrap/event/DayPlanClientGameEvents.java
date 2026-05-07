package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundOpenUiPacket;
import dev.breezes.settlements.presentation.ui.keybindings.SettlementsKeyMappings;
import dev.breezes.settlements.shared.util.VillagerRaycastUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.util.Optional;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DayPlanClientGameEvents {

    private static final String NO_VILLAGER_IN_RANGE_KEY = "ui.settlements.dayplan.no_villager_in_range";

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        while (SettlementsKeyMappings.OPEN_DAY_PLAN.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }

            Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(minecraft.player, 15.0);
            if (hitResult.isEmpty()) {
                minecraft.player.displayClientMessage(Component.translatable(NO_VILLAGER_IN_RANGE_KEY), true);
                continue;
            }

            PacketDistributor.sendToServer(new ServerBoundOpenUiPacket(UiChannel.DAY_PLAN, hitResult.get().getEntity().getId()));
            break;
        }
    }

}
