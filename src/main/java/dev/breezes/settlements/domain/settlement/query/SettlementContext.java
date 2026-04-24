package dev.breezes.settlements.domain.settlement.query;

import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record SettlementContext(@Nonnull SettlementMetadata metadata) {

    public String settlementId() {
        return this.metadata.settlementId();
    }

}
