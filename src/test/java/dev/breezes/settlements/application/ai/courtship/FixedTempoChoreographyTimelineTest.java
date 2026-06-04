package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedTempoChoreographyTimelineTest {

    private static final CourtshipBeat BEAT_A = new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.POINT, null);
    private static final CourtshipBeat BEAT_B = new CourtshipBeat(AnimationArchetype.IDLE, AnimationArchetype.EAT, null);
    private static final CourtshipBeat BEAT_C = new CourtshipBeat(AnimationArchetype.POINT, AnimationArchetype.IDLE, null);

    @Test
    void beatCount_returnsListSize() {
        // Arrange
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B, BEAT_C))
                .ticksPerBeat(50)
                .build();

        // Act
        int count = timeline.beatCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    void totalDurationTicks_isListSizeTimesTicksPerBeat() {
        // Arrange
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B, BEAT_C))
                .ticksPerBeat(50)
                .build();

        // Act
        long duration = timeline.totalDurationTicks();

        // Assert
        assertEquals(150L, duration);
    }

    @Test
    void beatStartTick_computesCorrectOffset() {
        // Arrange
        long start = 1000L;
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B, BEAT_C))
                .ticksPerBeat(50)
                .build();

        // Act & Assert
        assertEquals(1000L, timeline.beatStartTick(0, start));
        assertEquals(1050L, timeline.beatStartTick(1, start));
        assertEquals(1100L, timeline.beatStartTick(2, start));
    }

    @Test
    void beatAt_returnsCorrectElement() {
        // Arrange
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B, BEAT_C))
                .ticksPerBeat(50)
                .build();

        // Act & Assert
        assertEquals(BEAT_A, timeline.beatAt(0));
        assertEquals(BEAT_B, timeline.beatAt(1));
        assertEquals(BEAT_C, timeline.beatAt(2));
    }

    @Test
    void beatAt_clampsHighIndex() {
        // Arrange
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B))
                .ticksPerBeat(50)
                .build();

        // Act
        CourtshipBeat result = timeline.beatAt(99);

        // Assert — last element returned, not an exception
        assertEquals(BEAT_B, result);
    }

    @Test
    void beatAt_clampsLowIndex() {
        // Arrange
        FixedTempoChoreographyTimeline timeline = FixedTempoChoreographyTimeline.builder()
                .beats(List.of(BEAT_A, BEAT_B))
                .ticksPerBeat(50)
                .build();

        // Act
        CourtshipBeat result = timeline.beatAt(-1);

        // Assert — first element returned
        assertEquals(BEAT_A, result);
    }

}
