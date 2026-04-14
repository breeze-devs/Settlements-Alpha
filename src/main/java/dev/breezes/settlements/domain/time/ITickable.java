package dev.breezes.settlements.domain.time;

public interface ITickable {

    void tick(double delta);

    /**
     * Get the number of ticks that have elapsed since the last reset
     */
    double getTicksElapsed();

    long getTicksElapsedRounded();

    default long getTicksRemainingRounded() {
        return 0L;
    }

    default boolean tickAndCheck(double delta) {
        this.tick(delta);
        return this.isComplete();
    }

    default boolean checkAndReset() {
        boolean result = this.isComplete();
        if (result) {
            this.reset();
        }
        return result;
    }

    default boolean tickCheckAndReset(double delta) {
        boolean result = this.tickAndCheck(delta);
        if (result) {
            this.reset();
        }
        return result;
    }

    void reset();

    // Values above 1.0 extend beyond the natural duration while values below 1.0 shorten it.
    default void resetWithMultiplier(double multiplier) {
        this.reset();
    }

    boolean isComplete();

    void forceComplete();

    String getRemainingCooldownsAsPrettyString();

}
