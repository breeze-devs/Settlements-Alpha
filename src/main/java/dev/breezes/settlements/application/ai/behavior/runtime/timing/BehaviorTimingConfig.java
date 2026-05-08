package dev.breezes.settlements.application.ai.behavior.runtime.timing;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.RandomRangeTickable;

public interface BehaviorTimingConfig {

    int preconditionCheckCooldownMin();

    int preconditionCheckCooldownMax();

    int behaviorCooldownMin();

    int behaviorCooldownMax();

    default ITickable createPreconditionCheckCooldownTickable() {
        return RandomRangeTickable.of(ClockTicks.seconds(this.preconditionCheckCooldownMin()), ClockTicks.seconds(this.preconditionCheckCooldownMax()));
    }

    default ITickable createBehaviorCooldownTickable() {
        return RandomRangeTickable.of(ClockTicks.seconds(this.behaviorCooldownMin()), ClockTicks.seconds(this.behaviorCooldownMax()));
    }

}
