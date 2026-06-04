package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

import javax.annotation.Nonnull;

import javax.inject.Inject;
import java.util.List;

/**
 * Holds a small library of distinct choreography routines selected at session creation.
 * <p>
 * Each routine is authored here so behavior code stays data-blind: it drives the session
 * through a timeline index without knowing which animations are playing.
 * <p>
 * Beat archetypes are chosen from the safe non-tool-swing set (IDLE, POINT, INTERACT, EAT)
 * so they look natural for a villager-to-villager social scene.
 */
@ServerScope
public final class CourtshipChoreographyLibrary {

    private static final int TICKS_PER_BEAT = 50;

    private final List<ChoreographyTimeline> routines;

    @Inject
    public CourtshipChoreographyLibrary() {
        this.routines = List.of(
                // Routine 0: Offer-and-accept — presenter offers attentively, receiver reacts warmly.
                FixedTempoChoreographyTimeline.builder()
                        .beats(List.of(
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.IDLE, null),
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.POINT, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.EAT, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.INTERACT, null),
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.IDLE, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.INTERACT, null)
                        ))
                        .ticksPerBeat(TICKS_PER_BEAT)
                        .build(),

                // Routine 1: Shared meal — villagers eat together, glancing up between bites.
                FixedTempoChoreographyTimeline.builder()
                        .beats(List.of(
                                new CourtshipBeat(AnimationArchetype.EAT, AnimationArchetype.EAT, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.POINT, null),
                                new CourtshipBeat(AnimationArchetype.EAT, AnimationArchetype.EAT, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.IDLE, null),
                                new CourtshipBeat(AnimationArchetype.EAT, AnimationArchetype.EAT, null)
                        ))
                        .ticksPerBeat(TICKS_PER_BEAT)
                        .build(),

                // Routine 2: Conversation — alternating gestures, as if swapping excited stories.
                FixedTempoChoreographyTimeline.builder()
                        .beats(List.of(
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.IDLE, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.POINT, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.IDLE, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.INTERACT, null),
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.POINT, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.IDLE, null)
                        ))
                        .ticksPerBeat(TICKS_PER_BEAT)
                        .build(),

                // Routine 3: Attentive gift — presenter shows something, receiver examines it closely.
                FixedTempoChoreographyTimeline.builder()
                        .beats(List.of(
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.POINT, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.INTERACT, null),
                                new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.EAT, SoundEvents.VILLAGER_YES),
                                new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.IDLE, null),
                                new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.POINT, null)
                        ))
                        .ticksPerBeat(TICKS_PER_BEAT)
                        .build()
        );
    }

    public int size() {
        return this.routines.size();
    }

    public ChoreographyTimeline get(int id) {
        int safeId = Math.clamp(id, 0, this.routines.size() - 1);
        return this.routines.get(safeId);
    }

    public int selectRandom(@Nonnull RandomSource random) {
        return random.nextInt(this.routines.size());
    }

}
