package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.time.ClockTicks;

import javax.annotation.Nonnull;

/**
 * Defers server aim updates after local input to reduce snap-back from delayed replies.
 * All timestamps use the same tick counter.
 */
public final class BallistaIntentPrediction {

    /**
     * Allows time for the server's response to catch up with local input.
     */
    private static final ClockTicks HOLD = ClockTicks.seconds(1);

    private long heldUntil = Long.MIN_VALUE;

    private boolean serverIntentKept;
    private float keptYaw;
    private float keptPitch;

    /**
     * Restarts the hold for a local adjustment at the given tick.
     */
    public void predictedAt(long now) {
        this.heldUntil = now + HOLD.getTicks();
    }

    /**
     * Applies the server target, or buffers the latest target during the hold and returns aim unchanged.
     */
    public BallistaAim receive(@Nonnull BallistaAim aim, float intentYaw, float intentPitch, long now) {
        if (this.isHolding(now)) {
            this.serverIntentKept = true;
            this.keptYaw = intentYaw;
            this.keptPitch = intentPitch;
            return aim;
        }

        this.serverIntentKept = false;
        return aim.withIntent(intentYaw, intentPitch);
    }

    /**
     * Applies any buffered server target once the hold expires; otherwise returns aim unchanged.
     */
    public BallistaAim settle(@Nonnull BallistaAim aim, long now) {
        if (!this.serverIntentKept || this.isHolding(now)) {
            return aim;
        }

        this.serverIntentKept = false;
        return aim.withIntent(this.keptYaw, this.keptPitch);
    }

    private boolean isHolding(long now) {
        return now < this.heldUntil;
    }

}
