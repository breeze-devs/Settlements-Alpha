package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.persistence.SettlementMetadataQueueService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import dev.breezes.settlements.infrastructure.minecraft.persistence.SettlementSavedData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;
import java.util.List;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class SettlementMetadataPersistenceServerEvents {

    private final SettlementMetadataQueueService queueService;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        List<SettlementMetadata> drained = this.queueService.drain();
        if (drained.isEmpty()) {
            return;
        }

        SettlementSavedData savedData = SettlementSavedData.get(event.getServer());
        for (SettlementMetadata metadata : drained) {
            // Worldgen runs off-thread, so persistence must cross the thread boundary here
            // before touching DimensionDataStorage-backed SavedData.
            savedData.put(metadata);
        }

        log.debug("Persisted {} queued settlement metadata entries", drained.size());
    }

}
