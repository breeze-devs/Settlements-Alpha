package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.SleepingZEmitter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nonnull;

/**
 * Drives the "Zzz" streams rising off sleeping villagers near the player.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SleepingZClientEvents {

    private static final double EMIT_RADIUS = 12.0;

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        // Stop ticking when game is paused
        if (minecraft.isPaused() || level.tickRateManager().isFrozen()) {
            return;
        }

        AABB searchBox = player.getBoundingBox().inflate(EMIT_RADIUS);
        double radiusSquared = EMIT_RADIUS * EMIT_RADIUS;

        for (BaseVillager villager : level.getEntitiesOfClass(BaseVillager.class, searchBox,
                candidate -> candidate.isSleeping() && candidate.distanceToSqr(player) <= radiusSquared)) {
            SleepingZEmitter.tickSleeper(villager);
        }
    }

}
