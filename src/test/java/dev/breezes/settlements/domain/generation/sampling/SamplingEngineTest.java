package dev.breezes.settlements.domain.generation.sampling;

import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplingEngineTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");
    private static final int ANY_POPULATION = 10;

    private final SamplingEngine engine = new SamplingEngine();

    @Test
    void dominantTraitIsPrimary() {
        Map<TraitId, Float> scores = orderedScores(
                entry(LUMBER, 0.9f),
                entry(FARMING, 0.1f)
        );

        for (long seed = 0; seed < 100; seed++) {
            SettlementProfile profile = sampleWithTierAndSlots(scores, seed, ScaleTier.HAMLET, 1);
            assertEquals(LUMBER, profile.primary(), "Failed for seed " + seed);
        }
    }

    @Test
    void hamletSingleSlot() {
        SettlementProfile profile = sampleWithTierAndSlots(sampleScores(), 2L, ScaleTier.HAMLET, 1);

        assertEquals(ScaleTier.HAMLET, profile.scaleTier());
        assertEquals(1, profile.allTraits().size());
        assertEquals(0, profile.secondary().size());
        assertEquals(0, profile.flavor().size());
    }

    @Test
    void hamletTwoSlots() {
        SettlementProfile profile = sampleWithTierAndSlots(sampleScores(), 3L, ScaleTier.HAMLET, 2);

        assertEquals(ScaleTier.HAMLET, profile.scaleTier());
        assertEquals(2, profile.allTraits().size());
        assertEquals(1, profile.secondary().size());
        assertEquals(0, profile.flavor().size());
    }

    @Test
    void noDuplicateTraits() {
        SettlementProfile profile = sampleWithTierAndSlots(sampleScores(), 4L, ScaleTier.TOWN, 5);

        Set<TraitId> unique = new HashSet<>(profile.allTraits());
        assertEquals(profile.allTraits().size(), unique.size());
    }

    @Test
    void determinism() {
        Map<TraitId, Float> scores = sampleScores();

        SettlementProfile first = this.engine.sample(scores, ScaleTier.VILLAGE, ANY_POPULATION, new Random(12345L));
        SettlementProfile second = this.engine.sample(scores, ScaleTier.VILLAGE, ANY_POPULATION, new Random(12345L));

        assertEquals(first, second);
    }

    @Test
    void zeroScoresFallback() {
        SettlementProfile profile = sampleWithTierAndSlots(orderedScores(
                entry(LUMBER, 0.0f),
                entry(FARMING, 0.0f),
                entry(DEFENSE, 0.0f)
        ), 5L, ScaleTier.HAMLET, 1);

        assertEquals(FARMING, profile.primary());
        assertEquals(1.5f, profile.adjustedWeights().get(FARMING), 0.0001f);
    }

    @Test
    void defenseAsPrimary() {
        SettlementProfile profile = sampleWithTierAndSlots(orderedScores(
                entry(DEFENSE, 0.95f),
                entry(FARMING, 0.03f),
                entry(LUMBER, 0.02f)
        ), 6L, ScaleTier.HAMLET, 1);

        assertEquals(DefenseLevel.HIGH, profile.defenseLevel());
    }

    @Test
    void defenseAbsent() {
        SettlementProfile profile = sampleWithTierAndSlots(orderedScores(
                entry(FARMING, 0.8f),
                entry(LUMBER, 0.2f)
        ), 7L, ScaleTier.TOWN, 4);

        assertEquals(DefenseLevel.NONE, profile.defenseLevel());
        assertFalse(profile.hasTrait(DEFENSE));
    }

    @Test
    void defenseAsSecondary() {
        Map<TraitId, Float> scores = orderedScores(
                entry(FARMING, 0.7f),
                entry(DEFENSE, 0.25f),
                entry(LUMBER, 0.05f)
        );

        SettlementProfile profile = this.engine.sample(scores, ScaleTier.HAMLET, ANY_POPULATION, new StubRandom(8L)
                .withInt(1)
                .withFloat(0.40f)
                .withFloat(0.00f)
        );

        assertEquals(DEFENSE, profile.secondary().getFirst());
        assertEquals(DefenseLevel.MODERATE, profile.defenseLevel());
    }

    @Test
    void wealthInRange() {
        SettlementProfile profile = this.engine.sample(sampleScores(), ScaleTier.VILLAGE, ANY_POPULATION, new Random(9L));

        assertTrue(profile.wealthLevel() >= 0.0f);
        assertTrue(profile.wealthLevel() <= 1.0f);
    }

    @Test
    void adjustedWeightsPrimaryBoosted() {
        Map<TraitId, Float> scores = orderedScores(
                entry(FARMING, 0.8f),
                entry(LUMBER, 0.2f)
        );

        SettlementProfile profile = sampleWithTierAndSlots(scores, 11L, ScaleTier.HAMLET, 1);

        assertEquals(1.2f, profile.adjustedWeights().get(profile.primary()), 0.0001f);
    }

    @Test
    void slotAllocationVillage() {
        SettlementProfile threeSlotProfile = sampleWithTierAndSlots(sampleScores(), 12L, ScaleTier.VILLAGE, 3);
        SettlementProfile fourSlotProfile = sampleWithTierAndSlots(sampleScores(), 13L, ScaleTier.VILLAGE, 4);

        assertEquals(ScaleTier.VILLAGE, threeSlotProfile.scaleTier());
        assertEquals(3, threeSlotProfile.allTraits().size());
        assertEquals(1, threeSlotProfile.secondary().size());
        assertEquals(1, threeSlotProfile.flavor().size());

        assertEquals(ScaleTier.VILLAGE, fourSlotProfile.scaleTier());
        assertEquals(4, fourSlotProfile.allTraits().size());
        assertEquals(2, fourSlotProfile.secondary().size());
        assertEquals(1, fourSlotProfile.flavor().size());
    }

    @Test
    void slotAllocationTown() {
        SettlementProfile fourSlotProfile = sampleWithTierAndSlots(sampleScores(), 14L, ScaleTier.TOWN, 4);
        SettlementProfile fiveSlotProfile = sampleWithTierAndSlots(sampleScores(), 15L, ScaleTier.TOWN, 5);

        assertEquals(ScaleTier.TOWN, fourSlotProfile.scaleTier());
        assertEquals(4, fourSlotProfile.allTraits().size());
        assertEquals(2, fourSlotProfile.secondary().size());
        assertEquals(1, fourSlotProfile.flavor().size());

        assertEquals(ScaleTier.TOWN, fiveSlotProfile.scaleTier());
        assertEquals(5, fiveSlotProfile.allTraits().size());
        assertEquals(2, fiveSlotProfile.secondary().size());
        assertEquals(2, fiveSlotProfile.flavor().size());
    }

    @Test
    void defenseAsFlavor() {
        Map<TraitId, Float> scores = orderedScores(
                entry(FARMING, 0.60f),
                entry(LUMBER, 0.30f),
                entry(DEFENSE, 0.10f)
        );

        List<TraitId> draws = this.engine.drawTraitsForTest(scores, 3, new StubRandom()
                .withFloat(0.40f)
                .withFloat(0.00f)
                .withFloat(0.00f)
        );

        assertEquals(List.of(FARMING, LUMBER, DEFENSE), draws);

        SettlementProfile profile = this.engine.sample(scores, ScaleTier.VILLAGE, ANY_POPULATION, new StubRandom(16L)
                .withInt(1)
                .withFloat(0.40f)
                .withFloat(0.00f)
                .withFloat(0.00f)
        );

        assertEquals(DEFENSE, profile.flavor().getFirst());
        assertEquals(DefenseLevel.LOW, profile.defenseLevel());
    }

    @Test
    void determineScaleTierUsesWeightedThresholds() {
        assertEquals(ScaleTier.VILLAGE, this.engine.determineScaleTier(new StubRandom().withFloat(0.10f)));
        assertEquals(ScaleTier.TOWN, this.engine.determineScaleTier(new StubRandom().withFloat(0.60f)));
        assertEquals(ScaleTier.HAMLET, this.engine.determineScaleTier(new StubRandom().withFloat(0.95f)));
    }

    private SettlementProfile sampleWithTierAndSlots(Map<TraitId, Float> scores, long seed, ScaleTier tier, int slots) {
        return this.engine.sample(scores, tier, ANY_POPULATION, new StubRandom(seed)
                .withInt(slots - tier.minTraitSlots())
        );
    }

    private static Map<TraitId, Float> sampleScores() {
        return orderedScores(
                entry(FARMING, 0.45f),
                entry(LUMBER, 0.25f),
                entry(FISHING, 0.15f),
                entry(MINING, 0.10f),
                entry(DEFENSE, 0.05f)
        );
    }

    @SafeVarargs
    private static Map<TraitId, Float> orderedScores(Map.Entry<TraitId, Float>... entries) {
        Map<TraitId, Float> scores = new LinkedHashMap<>();
        for (Map.Entry<TraitId, Float> entry : entries) {
            scores.put(entry.getKey(), entry.getValue());
        }
        return scores;
    }

    private static Map.Entry<TraitId, Float> entry(TraitId traitId, float score) {
        return Map.entry(traitId, score);
    }

    private static final class StubRandom extends Random {

        private final List<Float> floats = new ArrayList<>();
        private final List<Integer> ints = new ArrayList<>();

        private StubRandom() {
            super(0L);
        }

        private StubRandom(long seed) {
            super(seed);
        }

        private StubRandom withFloat(float value) {
            this.floats.add(value);
            return this;
        }

        private StubRandom withInt(int value) {
            this.ints.add(value);
            return this;
        }

        @Override
        public float nextFloat() {
            if (!this.floats.isEmpty()) {
                return this.floats.removeFirst();
            }
            return super.nextFloat();
        }

        @Override
        public int nextInt(int origin, int bound) {
            if (!this.ints.isEmpty()) {
                return origin + this.ints.removeFirst();
            }
            return super.nextInt(origin, bound);
        }
    }

}
