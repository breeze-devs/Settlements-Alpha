package dev.breezes.settlements.models.misc;

public interface ITickable {

    void tick(long delta);

    default boolean tickAndCheck(long delta) {
        if (this.isComplete()) {
            return true;
        }

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

    default boolean tickCheckAndReset(long delta) {
        boolean result = this.tickAndCheck(delta);
        if (result) {
            this.reset();
        }
        return result;
    }

    void reset();

    boolean isComplete();

    String getRemainingCooldownsAsPrettyString();

}
