package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.founding.SettlementFoundingConfig;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingService;
import dev.breezes.settlements.application.settlement.health.SettlementHealthService;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.persistence.SettlementStoreListener;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementDirectory;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link AttachmentRegistry#SETTLEMENT_DIRECTORY} as each server level loads, then synchronizes changes to
 * the level's stored settlement summaries.
 */
@ServerSide
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementDirectorySyncServerEvents implements SettlementStoreListener {

    private final SettlementStore settlementStore;
    private final SettlementHealthService healthService;
    private final SettlementFoundingConfig foundingConfig;

    @SubscribeEvent
    public void onLevelLoad(@Nonnull LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Map<UUID, SettlementSummary> summaries = new LinkedHashMap<>();
        // TODO: this sends the entire level's settlements to the client. Benchmark if this needs to be optimized.
        for (Settlement settlement : this.settlementStore.all(level)) {
            summaries.put(settlement.id(), this.summarize(settlement));
        }
        level.setData(AttachmentRegistry.SETTLEMENT_DIRECTORY,
                new SettlementDirectory(SettlementFoundingService.dimensionAllowsFounding(level), summaries));
    }

    @Override
    public void onSettlementChanged(@Nonnull ServerLevel level, @Nullable Settlement before, @Nullable Settlement after) {
        SettlementDirectory directory = level.getExistingDataOrNull(AttachmentRegistry.SETTLEMENT_DIRECTORY);
        // Absent only until the level's load builds it from the store, which then holds this change
        if (directory == null) {
            return;
        }

        boolean changed = after == null
                ? directory.remove(before.id())
                : directory.put(after.id(), this.summarize(after));
        if (changed) {
            level.syncData(AttachmentRegistry.SETTLEMENT_DIRECTORY);
        }
    }

    private SettlementSummary summarize(Settlement settlement) {
        return SettlementSummary.of(settlement, this.healthService.fadingDuration().orElse(null),
                this.foundingConfig.minimumResidents());
    }

}
