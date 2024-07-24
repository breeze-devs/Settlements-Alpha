package dev.breezes.settlements.models.misc;

import dev.breezes.settlements.util.RandomUtil;

/**
 * Tickable that can be resets
 */
public class RandomRangeTickable extends Tickable {

    protected final long minStartingTicks;

    public RandomRangeTickable(long maxStartingValue, long minStartingValue) {
        super(maxStartingValue);
        this.minStartingTicks = minStartingValue;

        // Set current tick to random value between the min and max starting ticks
        this.reset();
    }

    /**
     * Resets the current ticks to a random value between the min and max starting ticks
     */
    @Override
    public void reset() {
        this.currentTicks = RandomUtil.RANDOM.nextLong(this.maxTicks, this.minStartingTicks + 1);
    }

}
