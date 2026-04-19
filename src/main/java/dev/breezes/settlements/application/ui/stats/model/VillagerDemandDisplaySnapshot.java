package dev.breezes.settlements.application.ui.stats.model;

import javax.annotation.Nonnull;
import java.util.List;

public record VillagerDemandDisplaySnapshot(@Nonnull List<DemandDisplayEntry> entries) {

    public VillagerDemandDisplaySnapshot {
        entries = List.copyOf(entries);
    }

}
