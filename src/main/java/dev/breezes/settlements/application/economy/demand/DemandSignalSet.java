package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DemandSignalSet(@Nonnull List<DemandSignal> entries) {

    private static final DemandSignalSet EMPTY = new DemandSignalSet(List.of());

    public DemandSignalSet {
        entries = List.copyOf(entries);
    }

    public static DemandSignalSet empty() {
        return EMPTY;
    }

    public Optional<DemandSignal> find(@Nonnull ItemMatch key) {
        return this.entries.stream()
                .filter(entry -> entry.match().equals(key))
                .findFirst();
    }

    public DemandSignalSet upsert(@Nonnull DemandSignal entry) {
        List<DemandSignal> updated = new ArrayList<>(this.entries.size() + 1);
        boolean replaced = false;
        for (DemandSignal existing : this.entries) {
            if (existing.match().equals(entry.match())) {
                updated.add(entry);
                replaced = true;
                continue;
            }
            updated.add(existing);
        }
        if (!replaced) {
            updated.add(entry);
        }
        return new DemandSignalSet(updated);
    }

    public DemandSignalSet remove(@Nonnull ItemMatch key) {
        List<DemandSignal> updated = this.entries.stream()
                .filter(entry -> !entry.match().equals(key))
                .toList();
        return updated.size() == this.entries.size() ? this : new DemandSignalSet(updated);
    }

    public DemandSignalSet removeStale(long currentGameTime, long staleCutoffTicks) {
        if (staleCutoffTicks < 0) {
            throw new IllegalArgumentException("Demand-signal stale cutoff must be non-negative");
        }

        List<DemandSignal> updated = this.entries.stream()
                .filter(entry -> currentGameTime - entry.lastTouchedGameTime() <= staleCutoffTicks)
                .toList();
        return updated.size() == this.entries.size() ? this : new DemandSignalSet(updated);
    }

}
