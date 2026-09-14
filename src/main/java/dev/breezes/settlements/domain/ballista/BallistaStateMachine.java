package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.animation.BallistaAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * Immutable ballista state with timed winding and firing transitions.
 * All timestamps use the same tick counter.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaStateMachine {

    private static final ClockTicks WIND_LENGTH = ClockTicks.of(BallistaAnimations.WIND_DURATION_TICKS);
    private static final ClockTicks FIRE_LENGTH = ClockTicks.of(BallistaAnimations.FIRE_DURATION_TICKS);
    private static final ClockTicks RELEASE_DELAY = ClockTicks.of(BallistaAnimations.FIRE_RELEASE_TICK);

    private static final BallistaStateMachine UNWOUND = new BallistaStateMachine(Stage.UNWOUND, 0L);
    private static final BallistaStateMachine COCKED = new BallistaStateMachine(Stage.COCKED, 0L);

    @Getter
    private final Stage stage;
    private final long startedAt;

    public static BallistaStateMachine unwound() {
        return UNWOUND;
    }

    public static BallistaStateMachine cocked() {
        return COCKED;
    }

    /**
     * Returns a winding state starting at the given tick.
     *
     * @throws IllegalStateException if this machine is not unwound
     */
    public BallistaStateMachine startWindingAt(long now) {
        if (this.stage != Stage.UNWOUND) {
            throw new IllegalStateException("Only an unwound ballista can start winding, got " + this.stage);
        }

        return new BallistaStateMachine(Stage.WINDING, now);
    }

    /**
     * Returns a firing state starting at the given tick.
     *
     * @throws IllegalStateException if this machine is not cocked
     */
    public BallistaStateMachine fireAt(long now) {
        if (this.stage != Stage.COCKED) {
            throw new IllegalStateException("Only a cocked ballista can fire, got " + this.stage);
        }

        return new BallistaStateMachine(Stage.FIRING, now);
    }

    /**
     * Returns COCKED after winding completes or UNWOUND after firing completes; otherwise returns this instance.
     */
    public BallistaStateMachine settledAt(long now) {
        return switch (this.stage) {
            case UNWOUND, COCKED -> this;
            case WINDING -> now - this.startedAt >= WIND_LENGTH.getTicks() ? COCKED : this;
            case FIRING -> now - this.startedAt >= FIRE_LENGTH.getTicks() ? UNWOUND : this;
        };
    }

    /**
     * Whether elapsed time can advance this state.
     */
    public boolean isTransient() {
        return this.stage == Stage.WINDING || this.stage == Stage.FIRING;
    }

    /**
     * The tick at which winding or firing started.
     *
     * @throws IllegalStateException if this machine is neither winding nor firing
     */
    public long getStartedAt() {
        if (!this.isTransient()) {
            throw new IllegalStateException("Only a winding or firing ballista has a start instant, got " + this.stage);
        }

        return this.startedAt;
    }

    /**
     * Whether the firing is before its scheduled shot release.
     */
    public boolean holdsShotAt(long now) {
        return this.stage == Stage.FIRING && now - this.startedAt < RELEASE_DELAY.getTicks();
    }

    /**
     * Whether the firing has reached its release tick, even if its full duration has elapsed.
     * Checks timing only; it does not track whether a projectile was spawned.
     */
    public boolean hasReleasedAt(long now) {
        return this.stage == Stage.FIRING && now - this.startedAt >= RELEASE_DELAY.getTicks();
    }

    /**
     * Returns the winding beat at this exact tick. Excludes the start tick and does not replay missed beats.
     */
    public Optional<Beat> beatAt(long now) {
        if (this.stage != Stage.WINDING) {
            return Optional.empty();
        }

        long elapsed = now - this.startedAt;
        if (elapsed <= 0 || elapsed > WIND_LENGTH.getTicks()) {
            return Optional.empty();
        }
        if (elapsed == WIND_LENGTH.getTicks()) {
            return Optional.of(Beat.LOCKS);
        }

        int clipTick = (int) elapsed;
        if (BallistaAnimations.WIND_STROKE_BEGIN_TICKS.contains(clipTick)) {
            return Optional.of(Beat.STROKE_BEGINS);
        }
        if (BallistaAnimations.WIND_STROKE_CATCH_TICKS.contains(clipTick)) {
            return Optional.of(Beat.STROKE_CATCHES);
        }
        return Optional.empty();
    }

    public enum Stage {

        UNWOUND,
        WINDING,
        COCKED,
        FIRING

    }

    /**
     * Audible milestones during winding.
     */
    public enum Beat {

        /**
         * The crank starts a stroke, drawing the pusher back.
         */
        STROKE_BEGINS,

        /**
         * The crank catches, holding the pusher between strokes.
         */
        STROKE_CATCHES,

        /**
         * Winding finishes and the machine is cocked.
         */
        LOCKS

    }

}
