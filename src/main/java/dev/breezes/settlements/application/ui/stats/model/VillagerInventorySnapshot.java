package dev.breezes.settlements.application.ui.stats.model;

import dev.breezes.settlements.domain.inventory.BackpackEntry;
import lombok.Builder;

import java.util.List;

@Builder
public record VillagerInventorySnapshot(
        List<BackpackEntry> entries
) {

    public VillagerInventorySnapshot {
        entries = List.copyOf(entries);
    }

    public int distinctKinds() {
        return this.entries.size();
    }

    public int totalItemCount() {
        return this.entries.stream()
                .mapToInt(BackpackEntry::count)
                .sum();
    }

}
