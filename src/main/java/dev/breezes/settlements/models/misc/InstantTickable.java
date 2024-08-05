package dev.breezes.settlements.models.misc;

/**
 * Represents a tickable that is instant and does not have a duration
 */
public class InstantTickable implements ITickable {

    @Override
    public void tick(long delta) {
        // Do nothing
    }

    @Override
    public void reset() {
        // Do nothing
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String getRemainingCooldownsAsPrettyString() {
        return "00:00";
    }

}
