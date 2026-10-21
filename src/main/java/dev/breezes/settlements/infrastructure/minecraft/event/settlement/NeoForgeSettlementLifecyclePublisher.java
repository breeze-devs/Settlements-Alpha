package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Posts each settlement lifecycle change on the NeoForge game bus as its {@link SettlementLifecycleEvent}.
 */
@CustomLog(topic = "settlement")
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class NeoForgeSettlementLifecyclePublisher implements SettlementLifecyclePublisher {

    @Override
    public void founded(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        post(new SettlementFoundedEvent(level, settlement));
    }

    @Override
    public void recorded(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        post(new SettlementRecordedEvent(level, settlement));
    }

    @Override
    public void renamed(@Nonnull ServerLevel level, @Nonnull Settlement settlement, @Nonnull String previousName,
                        @Nullable ServerPlayer renamer) {
        post(new SettlementRenamedEvent(level, settlement, previousName, renamer));
    }

    @Override
    public void disbanded(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        post(new SettlementDisbandedEvent(level, settlement));
    }

    private static void post(SettlementLifecycleEvent event) {
        // The change is already stored, so a failing listener must not make the writer believe it was not
        try {
            NeoForge.EVENT_BUS.post(event);
        } catch (RuntimeException exception) {
            // NeoForge's event bus logs a listener's exception with its stack trace before rethrowing it, so this line
            // adds only the settlement it concerns
            log.error("A listener failed on {} for settlement {} in {}", event.getClass().getSimpleName(),
                    event.getSettlement().id(), event.getLevel().dimension().location());
        }
    }

}
