package dev.breezes.settlements.application.ai.memory;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.observation.Observation;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Decides which of the observations a villager perceived are kept as episodic memory.
 * <p>
 * Every observation offered is admitted, so perception range is currently the only thing standing
 * between an occurrence and a villager's memory of it.
 * <p>
 * TODO: give this an attention/salience policy — a villager that keeps every perception has no
 *  attention, and the bounded store then evicts by arrival order rather than by what mattered.
 *  The observation parameter is the seam that policy needs and is unused until it exists. Any
 *  policy must still honor {@code WorldEventType.forceRemember}: those events are admitted
 *  unconditionally no matter how selective the policy is.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MemoryAdmissionGate {

    /**
     * Whether the observation is worth keeping as an episodic memory.
     */
    public boolean admits(Observation observation) {
        return true;
    }

}
