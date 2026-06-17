package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * The admittable unit of the SocialCue lane: a labeled, channel-claiming container
 * that wraps a {@link SocialCueScript}.
 * <p>
 * A SocialCue is admitted only when its channel claims are disjoint with the currently
 * running behavior's required channels.
 * <p>
 * Hard invariants (enforced structurally by {@link SocialCueScript} and {@link CueStep}):
 * <ul>
 *   <li>No navigation.</li>
 *   <li>No world/inventory mutation.</li>
 *   <li>Total duration ≤ {@link SocialCueScript#MAX_DURATION}.</li>
 * </ul>
 */
@Builder
@Getter
public final class SocialCue {

    private final String key;

    @Singular
    private final Set<BehaviorChannel> channels;

    private final ClockTicks cooldown;

    private final SocialCueScript script;

    /**
     * The context key returned by the catalog entry's trigger predicate at admission time.
     * Stored here so the arbiter can pass it to the catalog entry's {@code onComplete}
     * callback when the cue finishes, without re-evaluating the trigger.
     * Null for cues whose entries have no completion callback (cosmetic cues).
     */
    @Nullable
    private final String contextKey;

}
