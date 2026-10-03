package dev.breezes.settlements.domain.time;

import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Random;

@AllArgsConstructor
public class Tickable implements ITickable {

    protected final double baseTicks;
    protected double currentTicks;

    public Tickable(double baseTicks) {
        this(baseTicks, baseTicks);
    }

    public static Tickable of(@Nonnull ClockTicks baseTicks) {
        return new Tickable(baseTicks.getTicks());
    }

    /**
     * Builds a periodic timer that first fires on a random tick within its first interval, then re-arms
     * at exactly that interval, so timers built on the same tick do not all fire together.
     *
     * @param interval the steady-state period; clamped up to 1 tick
     */
    public static Tickable staggered(@Nonnull ClockTicks interval) {
        return staggered(interval, RandomUtil.RANDOM);
    }

    /**
     * Same as {@link #staggered(ClockTicks)}, but drawing from a random source.
     */
    static Tickable staggered(@Nonnull ClockTicks interval, @Nonnull Random random) {
        long intervalTicks = Math.max(1L, interval.getTicks());
        // Drawn from [1, interval], not [0, interval): a starting count of 0 fires on the first tick just
        // like 1 does, which would double up that tick and leave the interval's last tick empty
        long firstFiringTick = random.nextLong(1L, intervalTicks + 1);
        return new Tickable(intervalTicks, firstFiringTick);
    }

    /**
     * A phase in [0, interval) picked by identityHash, for a caller staggering its own modulo check
     * against an absolute clock instead of holding a Tickable.
     *
     * @param interval     the period being staggered across; clamped up to 1 tick
     * @param identityHash a stable per-instance hash that picks the phase
     */
    public static long staggeredPhase(@Nonnull ClockTicks interval, int identityHash) {
        return Math.floorMod(identityHash, Math.max(1L, interval.getTicks()));
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
        long totalSeconds = remainingTicks / ClockTicks.TICKS_PER_SECOND;
        long minutes = totalSeconds / ClockTicks.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % ClockTicks.SECONDS_PER_MINUTE;
        return String.format("%02d:%02d", minutes, seconds);
    }

}
