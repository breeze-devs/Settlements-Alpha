package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Owns the execution and cleanup of a single override for one villager.
 */
public interface OverrideRunner {

    /**
     * Begins execution.
     * <p>
     * Call once per runner, after admission checks have passed and any prior execution has been
     * stopped or suspended.
     */
    void start(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

    /**
     * Advances execution by one server tick after {@link #start}.
     */
    void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

    /**
     * Stops execution and releases any remaining resources.
     * Safe after natural completion or a previous stop (idempotent).
     */
    void stop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

    /**
     * Whether execution has ended or can no longer continue.
     * <p>
     * A true result still requires {@link #stop} to release any remaining resources.
     */
    boolean isComplete();

    /**
     * The priority tier assigned to this execution.
     */
    OverrideTier tier();

    /**
     * The behavior channels claimed by this execution.
     */
    Set<BehaviorChannel> occupiedChannels();

    /**
     * A short label identifying this runner's execution for logs and diagnostics.
     */
    String diagnosticId();

}
