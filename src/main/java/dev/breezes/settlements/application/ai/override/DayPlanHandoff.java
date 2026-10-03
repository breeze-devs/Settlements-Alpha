package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Day-plan suspension and retry operations for override handoffs.
 */
public interface DayPlanHandoff {

    /**
     * Interrupts the running day-plan behavior and runs its teardown before returning.
     * <p>
     * Does nothing if no day-plan behavior is running.
     *
     * @return whether a running behavior was interrupted
     */
    boolean suspendIfActive(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

    /**
     * Returns the current INTERRUPTED slot to PENDING, making it eligible for a later execution attempt.
     * <p>
     * Does nothing if there is no current interrupted slot.
     */
    void reAttemptInterruptedSlot(@Nonnull BaseVillager villager);

}
