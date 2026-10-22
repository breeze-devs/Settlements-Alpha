package dev.breezes.settlements.application.ui.stats.model;

import java.util.List;

public record VillagerDemandDisplaySnapshot(List<DemandDisplayEntry> entries) {

    public VillagerDemandDisplaySnapshot {
        entries = List.copyOf(entries);
    }

}
