package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single step in a {@link SocialCueScript}
 * <p>
 * The sealed hierarchy ensures CueSteps are structurally incapable of
 * navigation or world/inventory mutation — the hard invariant that keeps the
 * SocialCue lane from silently growing into a second behavior system
 */
public sealed interface CueStep
        permits CueStep.Gesture, CueStep.Bubble, CueStep.Sound, CueStep.Gaze, CueStep.Wait {

    /**
     * Triggers a one-shot animation on the villager
     */
    record Gesture(@Nonnull AnimationArchetype archetype) implements CueStep {
    }

    /**
     * Pushes a short-lived ambient message onto the FLAVOR bubble channel
     */
    record Bubble(@Nonnull String text,
                  @Nonnull ClockTicks ttl) implements CueStep {
    }

    /**
     * Plays a world sound at the villager's position
     */
    record Sound(@Nonnull SoundEvent soundEvent,
                 float volume,
                 float pitch) implements CueStep {
    }

    /**
     * Requests that the villager's gaze snap to a target location for the
     * remainder of the active cue. Inserted into the LookQueries resolution
     * order between behavior LookState and nav-target fallback
     */
    record Gaze(@Nullable Location target) implements CueStep {
    }

    /**
     * Idles for a fixed duration before the next step fires
     */
    record Wait(@Nonnull ClockTicks duration) implements CueStep {
    }

}
