package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.util.RandomUtil;
import jakarta.inject.Inject;

/**
 * Builds an umbrella animator with its own gate delay.
 * Delay so villagers whose gates flip on the same tick do not animate in unison.
 */
public final class DefaultUmbrellaAnimatorFactory implements UmbrellaAnimatorFactory {

    /**
     * Longest delay. Actual delay samples from 0 to this value.
     */
    private static final int MAX_GATE_DELAY_TICKS = ClockTicks.seconds(2).getTicksAsInt();

    @Inject
    DefaultUmbrellaAnimatorFactory() {
    }

    @Override
    public UmbrellaAnimator create() {
        // Add random delay to prevent everyone from moving in lock step
        return new DefaultUmbrellaAnimator(RandomUtil.RANDOM.nextInt(MAX_GATE_DELAY_TICKS + 1));
    }

}
