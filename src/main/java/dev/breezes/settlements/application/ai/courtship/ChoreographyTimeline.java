package dev.breezes.settlements.application.ai.courtship;

/**
 * Extension seam for synchronized animations.
 * CourtshipPresenter dispatches through beatAt(index) so a richer implementation
 * replaces only this interface and its backing data — not the session machine or behaviors.
 */
public interface ChoreographyTimeline {

    int beatCount();

    long beatStartTick(int beatIndex, long courtshipStartGameTime);

    long totalDurationTicks();

    CourtshipBeat beatAt(int beatIndex);

}
