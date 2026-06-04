package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data for a single choreography beat describing what each role plays.
 * Used by CourtshipPresenter to dispatch role-specific effects.
 */
public record CourtshipBeat(
        @Nonnull AnimationArchetype presenterMotion,
        @Nonnull AnimationArchetype receiverMotion,
        @Nullable SoundEvent sound
) {

    /**
     * Returns the animation archetype for the given role, keeping role-to-motion mapping
     * co-located with the beat data rather than scattered across callers.
     */
    public AnimationArchetype motionFor(@Nonnull CourtshipRole role) {
        return role == CourtshipRole.PRESENTER ? this.presenterMotion : this.receiverMotion;
    }

}
