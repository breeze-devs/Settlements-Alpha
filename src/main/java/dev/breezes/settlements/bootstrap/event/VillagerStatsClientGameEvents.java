package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundCloseVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.presentation.ui.keybindings.SettlementsKeyMappings;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsScreen;
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
public final class VillagerStatsClientGameEvents {

    private static final String NO_VILLAGER_IN_RANGE_KEY = "ui.settlements.stats.no_villager_in_range";

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        VillagerStatsClientState villagerStatsClientState = SettlementsDagger.client().villagerStatsClientState();

        // If the stats screen was closed unexpectedly but the session state was not cleared, clean it up
        if (villagerStatsClientState.hasActiveSession() && !(minecraft.screen instanceof VillagerStatsScreen)) {
            long sessionId = villagerStatsClientState.activeSessionId();
            villagerStatsClientState.forceClearSession();
            PacketDistributor.sendToServer(new ServerBoundCloseVillagerStatsPacket(sessionId));
        }

        while (SettlementsKeyMappings.OPEN_VILLAGER_STATS.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }

            Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(minecraft.player, 15.0);
            if (hitResult.isEmpty()) {
                minecraft.player.displayClientMessage(Component.translatable(NO_VILLAGER_IN_RANGE_KEY), true);
                continue;
            }

            PacketDistributor.sendToServer(new ServerBoundOpenVillagerStatsPacket(hitResult.get().getEntity().getId()));
            break;
        }
    }

}
