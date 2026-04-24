package dev.breezes.settlements.application.settlement.persistence;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@ServerSide
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementMetadataQueueService {

    private final ConcurrentLinkedQueue<SettlementMetadata> pendingMetadata = new ConcurrentLinkedQueue<>();

    public void enqueue(@Nonnull SettlementMetadata metadata) {
        this.pendingMetadata.add(metadata);
    }

    public List<SettlementMetadata> drain() {
        if (this.pendingMetadata.isEmpty()) {
            return List.of();
        }

        List<SettlementMetadata> drained = new ArrayList<>();
        SettlementMetadata metadata;
        while ((metadata = this.pendingMetadata.poll()) != null) {
            drained.add(metadata);
        }
        return drained;
    }

}
