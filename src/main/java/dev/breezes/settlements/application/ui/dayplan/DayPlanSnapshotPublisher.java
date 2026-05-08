package dev.breezes.settlements.application.ui.dayplan;

import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSnapshot;
import dev.breezes.settlements.application.ui.sync.UiSnapshotPublisher;
import dev.breezes.settlements.application.ui.sync.session.UiSession;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.packet.ClientBoundDayPlanSnapshotPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DayPlanSnapshotPublisher implements UiSnapshotPublisher {

    private static final int PUBLISH_INTERVAL_TICKS = ClockTicks.seconds(2).getTicksAsInt();

    private final DayPlanSnapshotAssembler assembler;

    @Override
    public void onSessionOpened(@Nonnull UiSession session,
                                @Nonnull ServerPlayer player,
                                @Nonnull BaseVillager villager,
                                long gameTime,
                                long dayTime) {
        this.publish(session, player, villager, gameTime, dayTime);
    }

    @Override
    public void tick(@Nonnull UiSession session,
                     @Nonnull ServerPlayer player,
                     @Nonnull BaseVillager villager,
                     long gameTime,
                     long dayTime) {
        if (gameTime - session.getLastSentGameTime() < PUBLISH_INTERVAL_TICKS) {
            return;
        }
        this.publish(session, player, villager, gameTime, dayTime);
    }

    private void publish(@Nonnull UiSession session,
                         @Nonnull ServerPlayer player,
                         @Nonnull BaseVillager villager,
                         long gameTime,
                         long dayTime) {
        DayPlan dayPlan = villager.getDayPlan();
        if (dayPlan == null) {
            return;
        }
        DayPlanSnapshot snapshot = this.assembler.assemble(dayPlan, villager, dayTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundDayPlanSnapshotPacket(session.getSessionId(), snapshot));
        session.markSnapshotSent(gameTime);
    }

}
