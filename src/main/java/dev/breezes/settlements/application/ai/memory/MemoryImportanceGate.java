package dev.breezes.settlements.application.ai.memory;

import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import lombok.AllArgsConstructor;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

/**
 * Heuristic gate for deciding which observations are important enough for memory promotion.
 */
@AllArgsConstructor(onConstructor_ = @Inject)
public class MemoryImportanceGate {

    public static final float PROMOTION_THRESHOLD = 2.0F;

    public float score(Observation observation, VillagerProfessionKey profession, GeneticsProfile genetics) {
        return this.score(observation, profession, genetics, List.of());
    }

    public float score(Observation observation,
                       VillagerProfessionKey profession,
                       GeneticsProfile genetics,
                       List<Observation> recentObservations) {
        int similarPeerCount = 0;
        for (Observation recent : recentObservations) {
            if (recent.type() == observation.type()) {
                similarPeerCount++;
            }
        }
        return this.score(observation, profession, genetics, similarPeerCount);
    }

    public float score(Observation observation,
                       VillagerProfessionKey profession,
                       GeneticsProfile genetics,
                       int similarPeerCount) {
        float base = observation.baseImportance();
        float novelty = this.computeNovelty(similarPeerCount);
        float geneModifier = this.computeGeneModifier(observation, genetics);
        float professionRelevance = this.computeProfessionRelevance(observation, profession);

        return (base * novelty * geneModifier) + professionRelevance;
    }

    public boolean shouldPromote(float score) {
        return score >= PROMOTION_THRESHOLD;
    }

    private float computeNovelty(int similarPeerCount) {
        if (similarPeerCount == 0) {
            return 1.5F;
        }
        if (similarPeerCount == 1) {
            return 1.0F;
        }
        if (similarPeerCount <= 3) {
            return 0.5F;
        }
        return 0.1F;
    }

    private float computeGeneModifier(Observation observation, GeneticsProfile genetics) {
        float intelligenceBonus = (float) genetics.getGeneValue(GeneType.INTELLIGENCE) * 0.2F;
        float modifier = 1.0F + intelligenceBonus;

        if (observation.type() == ObservationType.THREAT) {
            modifier *= 1.5F - ((float) genetics.getGeneValue(GeneType.WILL) * 0.6F);
        }
        if (observation.type() == ObservationType.SOCIAL || observation.type() == ObservationType.GOSSIP_RECEIVED) {
            modifier *= 1.0F + ((float) genetics.getGeneValue(GeneType.CHARISMA) * 0.3F);
        }

        return Math.max(0.1F, modifier);
    }

    private float computeProfessionRelevance(Observation observation, VillagerProfessionKey profession) {
        if (observation.type() == ObservationType.THREAT) {
            return 1.0F;
        }

        String normalizedContent = observation.content().toLowerCase(Locale.ROOT);
        String professionId = profession.id().toLowerCase(Locale.ROOT);
        if (normalizedContent.contains(professionId)) {
            return 0.75F;
        }

        // TODO: [agent] improve in the future
        return switch (professionId) {
            case "farmer" ->
                    containsAny(normalizedContent, "crop", "wheat", "sugarcane", "cow", "chicken", "honey") ? 0.5F : 0.0F;
            case "fisherman" -> containsAny(normalizedContent, "fish", "water", "cat") ? 0.5F : 0.0F;
            case "librarian" -> containsAny(normalizedContent, "book", "enchant", "library") ? 0.5F : 0.0F;
            case "mason" -> containsAny(normalizedContent, "stone", "ore", "mine") ? 0.5F : 0.0F;
            case "shepherd" -> containsAny(normalizedContent, "sheep", "wool", "wolf") ? 0.5F : 0.0F;
            case "butcher" -> containsAny(normalizedContent, "pig", "meat", "livestock", "smoker") ? 0.5F : 0.0F;
            case "cleric" -> containsAny(normalizedContent, "potion", "soul sand") ? 0.5F : 0.0F;
            default -> 0.0F;
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

}
