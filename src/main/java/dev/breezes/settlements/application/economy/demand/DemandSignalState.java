package dev.breezes.settlements.application.economy.demand;

import javax.annotation.Nonnull;

public record DemandSignalState(@Nonnull DemandSignalSet demandSignalSet, int version) {

    private static final DemandSignalState EMPTY = new DemandSignalState(DemandSignalSet.empty(), 0);

    public DemandSignalState {
        if (version < 0) {
            throw new IllegalArgumentException("Demand-signal state version must be non-negative");
        }
    }

    public static DemandSignalState empty() {
        return EMPTY;
    }

    public DemandSignalState withDemandSignalSet(@Nonnull DemandSignalSet updatedDemandSignalSet) {
        if (this.demandSignalSet.equals(updatedDemandSignalSet)) {
            return this;
        }
        return new DemandSignalState(updatedDemandSignalSet, this.version + 1);
    }

}
