package dev.breezes.settlements.domain.time;

import dev.breezes.settlements.shared.util.RandomUtil;

import javax.annotation.Nonnull;

/**
 * Tickable that can be resets
 */
public class RandomRangeTickable extends Tickable {

    protected final double minStartingTicks;

    public RandomRangeTickable(double maxStartingValue, double minStartingValue) {
        super(maxStartingValue);
        this.minStartingTicks = minStartingValue;

        // Set current tick to random value between the min and max starting ticks
        this.reset();
    }

    public static RandomRangeTickable of(@Nonnull ClockTicks maxTicks, @Nonnull ClockTicks minTicks) {
        return new RandomRangeTickable(maxTicks.getTicks(), minTicks.getTicks());
    }

    public static RandomRangeTickable of(@Nonnull ClockTicks maxTicks) {
        return new RandomRangeTickable(maxTicks.getTicks(), maxTicks.getTicks());
    }

    /**
     * Resets the current ticks to a random value between the min and max starting ticks
     */
    @Override
    public void reset() {
        double low = Math.min(this.minStartingTicks, this.baseTicks);
        double high = Math.max(this.minStartingTicks, this.baseTicks);
        this.currentTicks = RandomUtil.RANDOM.nextDouble(low, high + 1);
    }

    @Override
    public void resetWithMultiplier(double multiplier) {
        double low = Math.min(this.minStartingTicks, this.baseTicks) * multiplier;
        double high = Math.max(this.minStartingTicks, this.baseTicks) * multiplier;
        this.currentTicks = RandomUtil.RANDOM.nextDouble(low, high + 1);
    }

}
