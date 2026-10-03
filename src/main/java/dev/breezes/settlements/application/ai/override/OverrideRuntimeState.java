package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.Tickable;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-villager override lane state.
 */
@Getter
public final class OverrideRuntimeState {

    /**
     * Cadence for policy evaluation.
     * Deliberately short to evaluate frequently to handle invitations.
     */
    private static final ClockTicks EVALUATION_INTERVAL = ClockTicks.of(5);

    @Nullable
    private OverrideRunner runner;

    /**
     * The policy under which {@link #runner} was admitted.
     */
    @Nullable
    private OverridePolicy owningPolicy;

    private final ITickable evaluationCooldown = Tickable.staggered(EVALUATION_INTERVAL);

    public boolean hasRunner() {
        return this.runner != null;
    }

    public void installRunner(@Nonnull OverrideRunner runner, @Nonnull OverridePolicy owningPolicy) {
        this.runner = runner;
        this.owningPolicy = owningPolicy;
    }

    public void clearRunner() {
        this.runner = null;
        this.owningPolicy = null;
    }

}
