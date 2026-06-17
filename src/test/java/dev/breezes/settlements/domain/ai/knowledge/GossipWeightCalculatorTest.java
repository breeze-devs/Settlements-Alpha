package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GossipWeightCalculator}: staleness decay, hop penalty,
 * and CHA-gene bonus. No Minecraft types used.
 */
class GossipWeightCalculatorTest {

    // -------------------------------------------------------------------------
    // Staleness
    // -------------------------------------------------------------------------

    @Test
    void staleness_freshFactRetainsNearlyFullWeight() {
        // Arrange – 0 ticks elapsed
        float staleness = GossipWeightCalculator.staleness(1000L, 1000L);

        // Assert – no time has passed, decay ≈ 1.0
        assertEquals(1.0f, staleness, 0.01f);
    }

    @Test
    void staleness_halvesAtHalvingTime() {
        // Arrange – elapsed = exactly STALENESS_HALVING_TICKS
        long origin = 0L;
        long current = (long) GossipWeightCalculator.STALENESS_HALVING_TICKS;

        // Act
        float staleness = GossipWeightCalculator.staleness(origin, current);

        // Assert – weight should be approximately halved
        assertEquals(0.5f, staleness, 0.02f);
    }

    @Test
    void staleness_decreasesAsTimeGrows() {
        // Arrange
        long origin = 0L;
        float early = GossipWeightCalculator.staleness(origin, 1_000L);
        float later = GossipWeightCalculator.staleness(origin, 10_000L);

        // Assert – more time = less staleness score
        assertTrue(early > later, "More elapsed time should yield lower staleness score");
    }

    // -------------------------------------------------------------------------
    // Hop penalty
    // -------------------------------------------------------------------------

    @Test
    void hopPenalty_noHopsIsNoDecay() {
        // Arrange & Act
        float penalty = GossipWeightCalculator.hopPenalty(0);

        // Assert – first-hand knowledge: (0.6)^0 = 1.0
        assertEquals(1.0f, penalty, 0.001f);
    }

    @Test
    void hopPenalty_oneHopAppliesDecayFactor() {
        // Arrange & Act
        float penalty = GossipWeightCalculator.hopPenalty(1);

        // Assert – (0.6)^1 = 0.6
        assertEquals((float) GossipWeightCalculator.HOP_DECAY_FACTOR, penalty, 0.001f);
    }

    @Test
    void hopPenalty_twoHopsSquaresDecayFactor() {
        // Arrange & Act
        float penalty = GossipWeightCalculator.hopPenalty(2);
        float expected = (float) (GossipWeightCalculator.HOP_DECAY_FACTOR * GossipWeightCalculator.HOP_DECAY_FACTOR);

        // Assert
        assertEquals(expected, penalty, 0.001f);
    }

    @Test
    void hopPenalty_clampedAtCap() {
        // Arrange – passing a hop count above the cap should not produce a lower penalty
        // than passing the cap itself (clamping prevents wild over-penalties)
        float atCap = GossipWeightCalculator.hopPenalty(KnowledgeEntry.MAX_HOP_COUNT);
        float aboveCap = GossipWeightCalculator.hopPenalty(KnowledgeEntry.MAX_HOP_COUNT + 1);

        // Assert – clamped; both give the same value
        assertEquals(atCap, aboveCap, 0.001f);
    }

    // -------------------------------------------------------------------------
    // CHA bonus
    // -------------------------------------------------------------------------

    @Test
    void charismaBonus_lowChaYieldsLowerBonus() {
        // Arrange
        GeneticsProfile lowCha = genetics(0.1);
        GeneticsProfile highCha = genetics(0.9);

        // Act
        float lowBonus = GossipWeightCalculator.charismaBonus(lowCha);
        float highBonus = GossipWeightCalculator.charismaBonus(highCha);

        // Assert
        assertTrue(highBonus > lowBonus, "High CHA should yield a larger bonus multiplier");
    }

    @Test
    void charismaBonus_midChaGivesNeutralModifier() {
        // Arrange – CHA = 0.5 → bonus = 0.7 + 0.5 * 0.6 = 1.0
        GeneticsProfile midCha = genetics(0.5);

        // Act
        float bonus = GossipWeightCalculator.charismaBonus(midCha);

        // Assert – mid-CHA is the neutral point
        assertEquals(1.0f, bonus, 0.001f);
    }

    // -------------------------------------------------------------------------
    // Composite
    // -------------------------------------------------------------------------

    @Test
    void compute_hearsayEntryIsAlwaysLowerWeightThanOriginal() {
        // Arrange
        GeneticsProfile neutralGenetics = genetics(0.5);
        float originalWeight = 3.0f;
        long originTick = 0L;
        long currentTick = 100L;
        int hop = 1;

        // Act
        float adjusted = GossipWeightCalculator.compute(
                originalWeight, originTick, currentTick, neutralGenetics, hop);

        // Assert – the hearsay entry is always worth less than the original
        assertTrue(adjusted < originalWeight,
                "Hearsay entry weight should always be less than the original weight");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static GeneticsProfile genetics(double charisma) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        genes.put(GeneType.CHARISMA, new Gene(charisma));
        return new GeneticsProfile(genes);
    }

}
