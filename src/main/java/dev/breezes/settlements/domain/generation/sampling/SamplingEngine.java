package dev.breezes.settlements.domain.generation.sampling;

import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SamplingEngine {

    private static final float VILLAGE_WEIGHT = 0.40f;
    private static final float TOWN_WEIGHT = 0.30f;
    private static final float PRIMARY_WEIGHT_MULTIPLIER = 1.5f;
    private static final TraitId FALLBACK_PRIMARY = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId DEFENSE_TRAIT = TraitId.of("settlements:settlement_traits/defense");
    private static final float FALLBACK_SCORE = 1.0f;

    /**
     * Samples the profile of a settlement of the given tier and initial population from its trait scores.
     */
    public SettlementProfile sample(@Nonnull Map<TraitId, Float> traitScores,
                                    @Nonnull ScaleTier scaleTier,
                                    int initialPopulation,
                                    @Nonnull Random random) {
        int traitSlots = random.nextInt(scaleTier.minTraitSlots(), scaleTier.maxTraitSlots() + 1);
        Map<TraitId, Float> positiveScores = this.positiveScoresOrFallback(traitScores);
        List<TraitId> draws = this.drawTraits(positiveScores, traitSlots, random);

        TraitId primary = draws.getFirst();
        List<TraitId> secondary = this.secondaryTraits(draws);
        List<TraitId> flavor = this.flavorTraits(draws);
        Map<TraitId, Float> adjustedWeights = this.adjustedWeights(positiveScores, primary);
        float wealth = random.nextFloat();
        DefenseLevel defenseLevel = this.determineDefenseLevel(primary, secondary, flavor);
        long seed = random.nextLong();

        return SettlementProfile.builder()
                .primary(primary)
                .secondary(secondary)
                .flavor(flavor)
                .adjustedWeights(adjustedWeights)
                .scaleTier(scaleTier)
                .initialPopulation(initialPopulation)
                .wealthLevel(wealth)
                .defenseLevel(defenseLevel)
                .seed(seed)
                .build();
    }

    /**
     * Rolls the scale tier of a settlement generated with its structure.
     */
    public ScaleTier determineScaleTier(@Nonnull Random random) {
        float roll = random.nextFloat();
        if (roll < VILLAGE_WEIGHT) {
            return ScaleTier.VILLAGE;
        }
        if (roll < VILLAGE_WEIGHT + TOWN_WEIGHT) {
            return ScaleTier.TOWN;
        }
        return ScaleTier.HAMLET;
    }

    @VisibleForTesting
    List<TraitId> drawTraitsForTest(@Nonnull Map<TraitId, Float> traitScores,
                                    int traitSlots,
                                    @Nonnull Random random) {
        return this.drawTraits(this.positiveScoresOrFallback(traitScores), traitSlots, random);
    }

    private Map<TraitId, Float> positiveScoresOrFallback(@Nonnull Map<TraitId, Float> traitScores) {
        Map<TraitId, Float> positiveScores = new LinkedHashMap<>();
        for (Map.Entry<TraitId, Float> entry : traitScores.entrySet()) {
            Float score = entry.getValue();
            if (score != null && score > 0.0f) {
                positiveScores.put(entry.getKey(), score);
            }
        }

        if (positiveScores.isEmpty()) {
            positiveScores.put(FALLBACK_PRIMARY, FALLBACK_SCORE);
        }
        return positiveScores;
    }

    private List<TraitId> drawTraits(@Nonnull Map<TraitId, Float> traitScores,
                                     int traitSlots,
                                     @Nonnull Random random) {
        List<TraitId> draws = new ArrayList<>(traitSlots);
        Map<TraitId, Float> remaining = new LinkedHashMap<>(traitScores);

        while (!remaining.isEmpty() && draws.size() < traitSlots) {
            TraitId drawn = this.drawOne(remaining, random);
            draws.add(drawn);
            remaining.remove(drawn);
        }

        return draws;
    }

    private TraitId drawOne(@Nonnull Map<TraitId, Float> weightedPool, @Nonnull Random random) {
        float totalWeight = 0.0f;
        for (float weight : weightedPool.values()) {
            totalWeight += weight;
        }

        float roll = random.nextFloat() * totalWeight;
        float cumulative = 0.0f;
        TraitId lastTrait = null;
        for (Map.Entry<TraitId, Float> entry : weightedPool.entrySet()) {
            lastTrait = entry.getKey();
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return lastTrait;
    }

    private List<TraitId> secondaryTraits(List<TraitId> draws) {
        return switch (draws.size()) {
            case 1 -> List.of();
            case 2, 3 -> List.of(draws.get(1));
            case 4, 5 -> List.of(draws.get(1), draws.get(2));
            default -> throw new IllegalStateException("Unsupported trait draw count: " + draws.size());
        };
    }

    private List<TraitId> flavorTraits(List<TraitId> draws) {
        return switch (draws.size()) {
            case 1, 2 -> List.of();
            case 3 -> List.of(draws.get(2));
            case 4 -> List.of(draws.get(3));
            case 5 -> List.of(draws.get(3), draws.get(4));
            default -> throw new IllegalStateException("Unsupported trait draw count: " + draws.size());
        };
    }

    private Map<TraitId, Float> adjustedWeights(@Nonnull Map<TraitId, Float> traitScores,
                                                @Nonnull TraitId primary) {
        Map<TraitId, Float> adjustedWeights = new LinkedHashMap<>();
        adjustedWeights.putAll(traitScores);
        adjustedWeights.compute(primary, (ignored, score) -> score * PRIMARY_WEIGHT_MULTIPLIER);
        return adjustedWeights;
    }

    private DefenseLevel determineDefenseLevel(TraitId primary, List<TraitId> secondary, List<TraitId> flavor) {
        if (primary == DEFENSE_TRAIT) {
            return DefenseLevel.HIGH;
        }
        if (secondary.contains(DEFENSE_TRAIT)) {
            return DefenseLevel.MODERATE;
        }
        if (flavor.contains(DEFENSE_TRAIT)) {
            return DefenseLevel.LOW;
        }
        return DefenseLevel.NONE;
    }

}
