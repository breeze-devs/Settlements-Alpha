package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.ai.brain.IBrain;

import javax.annotation.Nonnull;

/**
 * A memory write that applies an {@link ObservationReport} into a decaying spatial store
 * rather than overwriting the whole memory value.
 */
public final class ObservationUpdateWrite implements IMemoryWrite {

    private final MemoryType.DecayingSpatialMemoryType type;
    private final ObservationReport report;
    private final long nowTick;

    public ObservationUpdateWrite(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                                  @Nonnull ObservationReport report,
                                  long nowTick) {
        this.type = type;
        this.report = report;
        this.nowTick = nowTick;
    }

    @Override
    public void applyTo(@Nonnull IBrain brain) {
        brain.updateObservation(this.type, this.report, this.nowTick);
    }

}
