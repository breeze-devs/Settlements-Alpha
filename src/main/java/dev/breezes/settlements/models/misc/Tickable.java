package dev.breezes.settlements.models.misc;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Tickable implements ITickable {

    protected final long maxStartingTicks;
    protected long currentTicks;

    public Tickable(long maxStartingTicks) {
        this(maxStartingTicks, maxStartingTicks);
    }

    @Override
    public void tick(long delta) {
        this.currentTicks -= delta;
    }

    @Override
    public void reset() {
        this.currentTicks = this.maxStartingTicks;
    }

    @Override
    public boolean isComplete() {
        return this.currentTicks <= 0;
    }

}
