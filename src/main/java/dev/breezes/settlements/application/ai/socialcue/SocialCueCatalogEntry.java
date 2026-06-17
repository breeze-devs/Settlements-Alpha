package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Wiring-layer descriptor for a single social cue type.
 * <p>
 * Mirrors {@code BehaviorCatalogEntry}: the Dagger module registers one entry per cue
 * via {@code @Provides @IntoSet}; the arbiter iterates them each tick to find candidates.
 * <p>
 * The {@code trigger} function receives the villager and returns the target entity UUID
 * (or empty if the cue cannot fire right now). Returning empty is cheap and idempotent;
 * the arbiter calls it every cycle.
 */
@Builder
@Getter
public final class SocialCueCatalogEntry {

    private final String key;

    @Singular
    private final Set<BehaviorChannel> channels;

    private final ClockTicks cooldown;

    private final ClockTicks perTargetCooldown;

    /**
     * Script factory invoked by the arbiter once the cue is admitted.
     * Receives the villager so scripts can encode the target in a Gaze step.
     */
    private final Function<BaseVillager, SocialCueScript> scriptFactory;

    /**
     * Predicate that returns a stable string context key if the cue should attempt to fire
     * (e.g. nearest-player UUID). The arbiter checks per-target cooldowns using this key.
     * Returning empty means "nothing to react to right now."
     */
    private final Function<BaseVillager, Optional<String>> trigger;

    /**
     * Optional callback invoked by the arbiter exactly once, immediately after the cue is
     * admitted (i.e. after {@link SocialCueRuntimeState#start} succeeds). Receives the
     * villager and the context key resolved by the trigger predicate.
     * <p>
     * Registry mutations (e.g. {@code gossipSessionRegistry.sendInvite}) must live here, not
     * inside the trigger. The arbiter may evaluate the trigger and then bail out before
     * admitting the cue (per-target cooldown check, script duration check); performing
     * mutations in the trigger would leak the session in those bail-out cases.
     */
    @Nullable
    private final BiConsumer<BaseVillager, String> onAdmit;

    /**
     * Optional callback invoked by the arbiter when this cue completes.
     * Receives the villager and the context key that was returned by {@link #trigger} at
     * admission time (stored in {@link SocialCue#getContextKey()}). Null means no action
     * on completion (the default for cosmetic cues like greet-player).
     * <p>
     * This seam is used by the gossip cues to write the knowledge copy once both villagers
     * have finished their cue scripts.
     */
    @Nullable
    private final BiConsumer<BaseVillager, String> onComplete;

    /**
     * Assembles a {@link SocialCue} from this entry, storing the context key on the cue
     * so the arbiter can pass it back to {@link #onComplete} when the cue finishes.
     *
     * @param villager   the villager being admitted
     * @param contextKey the context key returned by the trigger predicate
     */
    public SocialCue buildCue(@Nonnull BaseVillager villager, @Nullable String contextKey) {
        return SocialCue.builder()
                .key(this.key)
                .channels(this.channels)
                .cooldown(this.cooldown)
                .script(this.scriptFactory.apply(villager))
                .contextKey(contextKey)
                .build();
    }

    /**
     * Invokes the admit callback if one is registered.
     * Called by the arbiter immediately after the cue is admitted and started.
     */
    public void fireOnAdmit(@Nonnull BaseVillager villager, @Nullable String contextKey) {
        if (this.onAdmit != null && contextKey != null) {
            this.onAdmit.accept(villager, contextKey);
        }
    }

    /**
     * Invokes the completion callback if one is registered.
     * Called by the arbiter when the cue script finishes.
     */
    public void fireOnComplete(@Nonnull BaseVillager villager, @Nullable String contextKey) {
        if (this.onComplete != null && contextKey != null) {
            this.onComplete.accept(villager, contextKey);
        }
    }

}
