package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CultivationLilyBloomTracker#observe} is the whole edge-detection state machine: whether a
 * given tick's validity reading should trigger the bloom animation. Every case here is a named
 * counterexample (T1) for a defect that would replay the bloom on a lily the player never watched
 * change.
 */
class CultivationLilyBloomTrackerTest {

    @Test
    void observe_firstObservationAlreadyValid_doesNotBloom() {
        // The trap this whole class exists to defeat: LIT/valid arrives already true on chunk load,
        // world join, and re-entering render distance. A tracker that bloomed here would replay the
        // bloom for every already-valid lily the player merely walks back into view of.
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();

        assertFalse(tracker.observe(true));
    }

    @Test
    void observe_firstObservationInvalid_doesNotBloom() {
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();

        assertFalse(tracker.observe(false));
    }

    @Test
    void observe_falseToTrueTransitionAfterFirstObservation_blooms() {
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();
        tracker.observe(false);

        assertTrue(tracker.observe(true));
    }

    @Test
    void observe_sustainedValid_doesNotRebloomOnSubsequentTicks() {
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();
        tracker.observe(false);
        tracker.observe(true);

        assertFalse(tracker.observe(true));
    }

    @Test
    void observe_trueToFalseTransition_doesNotBloom() {
        // Losing validity is not a bloom event -- only the false-to-true edge is.
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();
        tracker.observe(true);

        assertFalse(tracker.observe(false));
    }

    @Test
    void observe_secondFalseToTrueTransition_bloomsAgain() {
        // A lily that loses and regains validity blooms each time it regains it, not just the first.
        CultivationLilyBloomTracker tracker = new CultivationLilyBloomTracker();
        tracker.observe(false);
        tracker.observe(true);
        tracker.observe(false);

        assertTrue(tracker.observe(true));
    }

}
