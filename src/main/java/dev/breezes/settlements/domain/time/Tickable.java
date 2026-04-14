package dev.breezes.settlements.domain.time;

import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

@AllArgsConstructor
public class Tickable implements ITickable {

    protected final double baseTicks;
    protected double currentTicks;

    public Tickable(double baseTicks) {
        this(baseTicks, baseTicks);
    }

    public static Tickable of(@Nonnull Ticks baseTicks) {
        return new Tickable(baseTicks.getTicks());
    }

    @Override
    public void tick(double delta) {
        this.currentTicks -= delta;
    }

    @Override
    public double getTicksElapsed() {
        return this.baseTicks - this.currentTicks;
    }

    @Override
    public long getTicksElapsedRounded() {
        return Math.round(this.getTicksElapsed());
    }

    @Override
    public long getTicksRemainingRounded() {
        return Math.round(Math.max(this.currentTicks, 0));
    }

    @Override
    public void reset() {
        this.currentTicks = this.baseTicks;
    }

    @Override
    public void resetWithMultiplier(double multiplier) {
        this.currentTicks = this.baseTicks * multiplier;
    }

    @Override
    public boolean isComplete() {
        return this.currentTicks <= 0;
    }

    @Override
    public void forceComplete() {
        this.currentTicks = 0;
    }

    @Override
    public String getRemainingCooldownsAsPrettyString() {
        long remainingTicks = Math.round(Math.max(currentTicks, 0));
        long totalSeconds = remainingTicks / Ticks.TICKS_PER_SECOND;
        long minutes = totalSeconds / Ticks.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % Ticks.SECONDS_PER_MINUTE;
        return String.format("%02d:%02d", minutes, seconds);
    }

}
