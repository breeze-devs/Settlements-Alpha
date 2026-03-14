package dev.breezes.settlements.application.ai.behavior.runtime.timing;

import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Ticks;

public interface BehaviorTimingConfig {

    int preconditionCheckCooldownMin();

    int preconditionCheckCooldownMax();

    int behaviorCooldownMin();

    int behaviorCooldownMax();

    default ITickable createPreconditionCheckCooldownTickable() {
        return RandomRangeTickable.of(Ticks.seconds(this.preconditionCheckCooldownMin()), Ticks.seconds(this.preconditionCheckCooldownMax()));
    }

    default ITickable createBehaviorCooldownTickable() {
        return RandomRangeTickable.of(Ticks.seconds(this.behaviorCooldownMin()), Ticks.seconds(this.behaviorCooldownMax()));
    }

}
