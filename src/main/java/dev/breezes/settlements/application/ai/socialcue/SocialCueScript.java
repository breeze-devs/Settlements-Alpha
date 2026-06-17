package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * An ordered, bounded timeline of {@link CueStep}s that drives a single social cue.
 * <p>
 * Generalizes {@code ChoreographyTimeline}: where choreography is synchronized across two
 * villager roles, a SocialCueScript is self-contained and driven by the arbiter, not a shared session.
 * <p>
 * Total duration is the sum of all {@link CueStep.Wait} durations in the script. Non-wait steps
 * fire immediately when the script reaches their index, so durations are colocated with the
 * waits that follow them rather than with the action steps themselves.
 * <p>
 * Construction is intentionally funneled through {@link #of(List)} so {@code totalDuration}
 * and the prefix-sum array are always derived together — the Lombok builder is private to
 * prevent callers from producing a half-initialized instance with a null totalDuration.
 */
@Builder(access = AccessLevel.PRIVATE)
@Getter
public final class SocialCueScript {

    /**
     * Maximum total script duration enforced by {@link SocialCueArbiter} at admission time.
     * Keeps the expression lane hard-bounded to prevent it from masquerading as real behavior time.
     */
    public static final ClockTicks MAX_DURATION = ClockTicks.seconds(5);

    @Singular
    private final List<CueStep> steps;

    private final ClockTicks totalDuration;

    /**
     * Prefix-sum of Wait offsets indexed by step position.
     * {@code waitOffsetPrefix[i]} holds the total Wait ticks contributed by steps 0..(i-1),
     * so {@code stepStartTick(i, start)} = {@code start + waitOffsetPrefix[i]} in O(1).
     * Length is {@code steps.size() + 1} so index {@code steps.size()} is also valid.
     */
    @Getter(AccessLevel.NONE)
    private final long[] waitOffsetPrefix;

    /**
     * Returns the step at the given index, or throws if out of range.
     */
    public CueStep stepAt(int index) {
        return this.steps.get(index);
    }

    public int stepCount() {
        return this.steps.size();
    }

    /**
     * Returns the absolute game-tick at which step {@code index} should fire.
     * Uses the precomputed prefix-sum so this is O(1) regardless of script length.
     */
    public long stepStartTick(int index, long scriptStartGameTime) {
        return scriptStartGameTime + this.waitOffsetPrefix[index];
    }

    /**
     * Sole construction path. Derives {@code totalDuration} and the Wait prefix-sum
     * in one pass so they are always consistent.
     */
    public static SocialCueScript of(@Nonnull List<CueStep> steps) {
        long[] prefix = new long[steps.size() + 1];
        long running = 0;
        for (int i = 0; i < steps.size(); i++) {
            prefix[i] = running;
            if (steps.get(i) instanceof CueStep.Wait wait) {
                running += wait.duration().getTicks();
            }
        }
        prefix[steps.size()] = running;

        return SocialCueScript.builder()
                .steps(steps)
                .totalDuration(ClockTicks.of(running))
                .waitOffsetPrefix(prefix)
                .build();
    }

}
