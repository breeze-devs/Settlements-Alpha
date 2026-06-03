package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * A single reversible artifact created by a behavior that must be released when the
 * behavior ends on any path: normal stop, interrupt, death, or crash recovery.
 * <p>
 * Implementations are records (immutable value objects) so they can be
 * serialized directly to the per-villager crash-recovery ledger.
 * <p>
 * Not sealed: the codec dispatches explicitly over known types via
 * pattern-matching switch; keeping this open allows plain-Java test fakes
 * without polluting the permits clause with test-only classes.
 */
public interface TeardownObligation {

    /**
     * Block position that anchors this obligation for chunk-loading checks.
     * Reconciliation defers (does not count a failed attempt) when the chunk
     * at this position is unloaded, preventing premature abandonment caused
     * by the target simply being off-screen.
     */
    BlockPos targetPos();

    /**
     * Is the target still present, in a loaded chunk, AND still ours to act on?
     * <p>
     * A {@code false} result means "nothing to do" — target gone, replaced, or
     * re-owned by a player — and is never treated as an error.  Both
     * {@link TeardownScope#teardownAll} and Dim 3 reconciliation guard every
     * discharge with this check, so the world changing between creation and
     * teardown is always safe.
     */
    boolean stillValid(@Nonnull ServerLevel level);

    /**
     * Release the artifact.  Pre-guarded by {@link #stillValid}; must still be
     * defensive and idempotent in case the world changes on the same tick.
     */
    void discharge(@Nonnull ServerLevel level);

    /**
     * Whether this obligation is persisted to the per-villager Dim 3 ledger for
     * crash recovery.  All world-artifact obligations are durable.
     */
    boolean durable();

    /**
     * Human-readable description for logging and introspection.
     */
    String describe();

}
