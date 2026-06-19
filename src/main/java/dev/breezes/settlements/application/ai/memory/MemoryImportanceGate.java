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

    /**
     * Additive bonus applied when the observing villager is the actor of the deed.
     * <p>
     * Sized to push a repeated own-RESOURCE deed (base 1.8, novelty tier 1.0, average genes)
     * reliably over the promotion threshold. Low-base lifecycle terminal events may still score below
     * threshold; the perception pipeline owns the separate self-terminal admission rule.
     */
    private static final float SELF_DEED_SALIENCE_BONUS = 0.5F;

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
        return this.score(observation, profession, genetics, similarPeerCount, false);
    }

    /**
     * Full scoring path. The {@code isSelfDeed} flag is set by the perception pipeline when
     * the observation's actor UUID matches the observing villager, giving meaningful own deeds
     * a salience bump so their stored weight reflects first-hand salience.
     */
    public float score(Observation observation,
                       VillagerProfessionKey profession,
                       GeneticsProfile genetics,
                       int similarPeerCount,
                       boolean isSelfDeed) {
        float base = observation.baseImportance();
        float novelty = this.computeNovelty(similarPeerCount);
        float geneModifier = this.computeGeneModifier(observation, genetics);
        float professionRelevance = this.computeProfessionRelevance(observation, profession);
        float selfDeedBonus = this.computeSelfDeedBonus(observation, isSelfDeed);

        return (base * novelty * geneModifier) + professionRelevance + selfDeedBonus;
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

    /**
     * Returns a salience bonus when the villager is re-experiencing its own meaningful deed.
     * <p>
     * This method shapes memory weight only. Admission is decided by {@code PerceptionPipeline},
     * which force-admits self-authored terminal events while still letting observer-side lifecycle
     * events remain low-signal background noise.
     */
    private float computeSelfDeedBonus(Observation observation, boolean isSelfDeed) {
        if (!isSelfDeed) {
            return 0.0F;
        }
        return SELF_DEED_SALIENCE_BONUS;
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
