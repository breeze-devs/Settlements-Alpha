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

    @Override
    public String getRemainingCooldownsAsPrettyString() {
        long remainingTicks = Math.max(currentTicks, 0);
        long totalSeconds = remainingTicks / Ticks.TICKS_PER_SECOND;
        long minutes = totalSeconds / Ticks.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % Ticks.SECONDS_PER_MINUTE;
        return String.format("%02d:%02d", minutes, seconds);
    }

}
