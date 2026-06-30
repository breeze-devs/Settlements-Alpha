package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.ai.brain.IBrain;

import javax.annotation.Nonnull;

/**
 * A memory write that applies a {@link SensedSiteReport} into a decaying spatial store
 * rather than overwriting the whole memory value.
 */
public final class SiteUpdateWrite implements IMemoryWrite {

    private final MemoryType.DecayingSpatialMemoryType type;
    private final SensedSiteReport report;
    private final long nowTick;

    public SiteUpdateWrite(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                           @Nonnull SensedSiteReport report,
                           long nowTick) {
        this.type = type;
        this.report = report;
        this.nowTick = nowTick;
    }

    @Override
    public void applyTo(@Nonnull IBrain brain) {
        brain.updateSites(this.type, this.report, this.nowTick);
    }

}
