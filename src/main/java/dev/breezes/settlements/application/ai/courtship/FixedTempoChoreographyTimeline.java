package dev.breezes.settlements.application.ai.courtship;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * A choreography driven by a fixed, equal-duration beat grid.
 * <p>
 * Separating duration from beat logic here lets the session registry express its COURTSHIP
 * timeout as a multiple of {@link #totalDurationTicks()} without knowing beat details.
 */
@Builder
@RequiredArgsConstructor
public final class FixedTempoChoreographyTimeline implements ChoreographyTimeline {

    private final List<CourtshipBeat> beats;
    private final int ticksPerBeat;

    @Override
    public int beatCount() {
        return this.beats.size();
    }

    @Override
    public long beatStartTick(int beatIndex, long courtshipStartGameTime) {
        return courtshipStartGameTime + (long) beatIndex * this.ticksPerBeat;
    }

    @Override
    public long totalDurationTicks() {
        return (long) this.beats.size() * this.ticksPerBeat;
    }

    @Override
    public CourtshipBeat beatAt(int beatIndex) {
        int clamped = Math.clamp(beatIndex, 0, this.beats.size() - 1);
        return this.beats.get(clamped);
    }

}
