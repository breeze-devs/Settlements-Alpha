package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.network.features.schedule.packet.ClientBoundVillageDayTypePacket;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;

/**
 * Keeps every connected client's mirror of the village work rhythm current.
 */
@ServerSide
@ServerScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillageDayTypeSyncServerEvents {

    private static final long NO_DAY_BROADCAST = Long.MIN_VALUE;

    private final IWeekCycleProvider weekCycleProvider;

    private long lastBroadcastCalendarDay = NO_DAY_BROADCAST;

    /**
     * Broadcasts whenever the overworld's calendar day stops matching the last one sent.
     */
    @SubscribeEvent
    public void onServerTick(@Nonnull ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long calendarDay = WorldCalendar.calendarDayOf(server.overworld().getDayTime());
        if (calendarDay == this.lastBroadcastCalendarDay) {
            return;
        }

        this.lastBroadcastCalendarDay = calendarDay;
        PacketDistributor.sendToAllPlayers(this.packetFor(calendarDay));
    }

    /**
     * Sends the arriving player the current payload, since the broadcast is only when day changes.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
        if (this.lastBroadcastCalendarDay == NO_DAY_BROADCAST || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PacketDistributor.sendToPlayer(serverPlayer, this.packetFor(this.lastBroadcastCalendarDay));
    }

    private ClientBoundVillageDayTypePacket packetFor(long calendarDay) {
        return new ClientBoundVillageDayTypePacket(calendarDay, this.weekCycleProvider.getDayType(calendarDay));
    }

}
