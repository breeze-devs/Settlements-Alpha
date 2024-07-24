package dev.breezes.settlements.models.misc;

import dev.breezes.settlements.util.Ticks;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

@AllArgsConstructor
public class Tickable implements ITickable {

    protected final long maxTicks;
    protected long currentTicks;

    public Tickable(long maxTicks) {
        this(maxTicks, maxTicks);
    }

    public static Tickable of(@Nonnull Ticks maxTicks) {
        return new Tickable(maxTicks.getTicks());
    }

    @Override
    public void tick(long delta) {
        this.currentTicks -= delta;
    }

    @Override
    public void reset() {
        this.currentTicks = this.maxTicks;
    }

    @Override
    public boolean isComplete() {
        return this.currentTicks <= 0;
    }

}
