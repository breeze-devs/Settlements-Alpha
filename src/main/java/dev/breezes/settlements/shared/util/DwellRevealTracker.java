package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Turns frame-to-frame aim-target identity into a {@link DwellReveal}: a target must be held for
 * {@link #DWELL_THRESHOLD} before it reveals anything, and opacity then ramps in — and back out — over
 * {@link #FADE_WINDOW}. Timings live here so every look-driven reveal shares them.
 * <p>
 * Presence is derived from the observed key being non-null, so a caller supplies a key that stays equal for
 * exactly as long as what it names is genuinely the same target. Each reveal holds its own instance.
 * <p>
 * A crosshair target is not a steady signal: against a small or thin hit box, even perfectly held aim drops
 * onto whatever lies behind or beside it for single frames. Two mechanisms absorb that.
 * <p>
 * - Opacity ramps rather than being derived from the current frame, so no single frame moves it more than
 * one frame's worth. Derived opacity puts the flicker on screen at full amplitude.
 * <p>
 * - The target that earned the reveal keeps it while it is still being painted, however many other keys are
 * observed meanwhile; only a key that clears the dwell in its own right takes over. Restarting the dwell on
 * every key change is what makes the reveal blink — aim slips for one frame, the reveal blanks for a whole
 * threshold, then snaps back to full.
 */
@ClientSide
public final class DwellRevealTracker {

    private static final ClockTicks DWELL_THRESHOLD = ClockTicks.seconds(0.2);

    /**
     * How long the reveal takes to reach full opacity, and to fall back from it. One window for both
     * directions: an asymmetric ramp reads as two effects rather than as one reveal.
     */
    private static final ClockTicks FADE_WINDOW = ClockTicks.seconds(0.25);

    static final long DWELL_THRESHOLD_MILLIS = ClientMonotonicClock.millisIn(DWELL_THRESHOLD);
    static final long FADE_WINDOW_MILLIS = ClientMonotonicClock.millisIn(FADE_WINDOW);

    /**
     * {@link #lastObservedMillis} sentinel for a tracker that has not observed a frame yet, so the first
     * frame seeds its own timing instead of measuring against an instant from before a {@link #reset}.
     */
    private static final long NEVER_OBSERVED = Long.MIN_VALUE;

    /**
     * How long the reveal holds at full opacity after its target is gone, before the fade starts.
     * <p>
     * Per-reveal rather than shared, because each surface answers it differently: a world-space footprint is
     * worth seconds — looking away is how the player checks where it lands — where text under the crosshair
     * that outlives its target is just in the way.
     */
    private final long lingerMillis;

    /**
     * The target that has earned the reveal and is still being painted. Dropped once the fade completes,
     * which both bounds the jitter grace above and releases the level the key holds.
     */
    @Nullable
    private Object revealedKey;

    /**
     * The target currently serving a dwell, if it is not already the revealed one.
     */
    @Nullable
    private Object candidateKey;

    private long candidateStartMillis;
    private long lastRevealedMillis;
    private long lastObservedMillis = NEVER_OBSERVED;
    private float alpha;

    public DwellRevealTracker() {
        this(ClockTicks.ZERO);
    }

    public DwellRevealTracker(@Nonnull ClockTicks linger) {
        this.lingerMillis = ClientMonotonicClock.millisIn(linger);
    }

    public DwellReveal observe(@Nullable Object targetKey) {
        return observe(targetKey, ClientMonotonicClock.nowMillis());
    }

    public void reset() {
        this.revealedKey = null;
        this.candidateKey = null;
        this.lastObservedMillis = NEVER_OBSERVED;
        this.alpha = 0.0F;
    }

    @VisibleForTesting
    DwellReveal observe(@Nullable Object targetKey, long nowMillis) {
        if (this.lastObservedMillis == NEVER_OBSERVED) {
            this.lastObservedMillis = nowMillis;
        }

        boolean revealed = resolveRevealed(targetKey, nowMillis);
        if (revealed) {
            this.lastRevealedMillis = nowMillis;
        }

        // Held separately from `revealed` so the linger cannot be mistaken for the target still being
        // there: opacity holds, but nothing new is resolved for a target the player has already left.
        boolean holdingOpacity = revealed
                || (this.revealedKey != null && nowMillis - this.lastRevealedMillis < this.lingerMillis);
        advanceAlpha(holdingOpacity, nowMillis);
        return new DwellReveal(revealed, this.alpha);
    }

    private boolean resolveRevealed(@Nullable Object targetKey, long nowMillis) {
        if (targetKey == null) {
            this.candidateKey = null;
            return false;
        }
        if (targetKey.equals(this.revealedKey)) {
            this.candidateKey = null;
            return true;
        }

        if (!targetKey.equals(this.candidateKey)) {
            this.candidateKey = targetKey;
            this.candidateStartMillis = nowMillis;
        }
        if (nowMillis - this.candidateStartMillis < DWELL_THRESHOLD_MILLIS) {
            return false;
        }

        this.revealedKey = targetKey;
        this.candidateKey = null;
        return true;
    }

    private void advanceAlpha(boolean holdingOpacity, long nowMillis) {
        float target = holdingOpacity ? 1.0F : 0.0F;
        float step = (float) (nowMillis - this.lastObservedMillis) / FADE_WINDOW_MILLIS;
        this.lastObservedMillis = nowMillis;

        this.alpha = this.alpha < target
                ? Math.min(target, this.alpha + step)
                : Math.max(target, this.alpha - step);

        // Releasing the key here rather than on the frame the target is lost is what bounds the jitter
        // grace: it lasts exactly as long as there is still something on screen to protect.
        if (this.alpha <= 0.0F && !holdingOpacity) {
            this.revealedKey = null;
        }
    }

}
