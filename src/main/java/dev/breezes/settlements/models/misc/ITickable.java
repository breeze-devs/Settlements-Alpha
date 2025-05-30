package dev.breezes.settlements.models.misc;

public interface ITickable {

    void tick(double delta);

    /**
     * Get the number of ticks that have elapsed since the last reset
     */
    double getTicksElapsed();

    long getTicksElapsedRounded();

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

    boolean isComplete();

    void forceComplete();

    String getRemainingCooldownsAsPrettyString();

}
