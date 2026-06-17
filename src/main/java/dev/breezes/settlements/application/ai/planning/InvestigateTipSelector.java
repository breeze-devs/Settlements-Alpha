package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Selects the single best pending hearsay tip for the Investigate behavior to act on.
 * <p>
 * "Best" is defined as the highest credibility-adjusted weight among all entries that:
 * <ul>
 *   <li>Are hearsay and still {@link KnowledgeResolution#UNRESOLVED}.</li>
 *   <li>Are not in a navigation-timeout cooldown (nextEligibleTick &lt;= now).</li>
 * </ul>
 * Tips from less-trusted sources are deprioritized so the planner focuses on reliable leads.
 * Tips with active cooldowns are skipped so the planner does not burn a scout slot on
 * an unreachable location the villager already tried and failed to navigate to.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvestigateTipSelector {

    /**
     * Picks the highest credibility-adjusted pending tip from the villager's knowledge store.
     *
     * @param store                the villager's knowledge store
     * @param observerVillagerUUID UUID of the villager performing the investigation (owner of credibility records)
     * @param reputationQuery      read-only interface for credibility multipliers
     * @param nowTick              current game tick, used to evaluate nextEligibleTick cooldowns
     * @return the best tip entry, or {@code null} when no eligible pending tips exist
     */
    @Nullable
    public static KnowledgeEntry select(@Nonnull VillagerKnowledgeStore store,
                                        @Nonnull UUID observerVillagerUUID,
                                        @Nonnull ReputationQuery reputationQuery,
                                        long nowTick) {
        KnowledgeEntry best = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (KnowledgeEntry entry : store.entriesView()) {
            if (!isPendingTip(entry)) {
                continue;
            }
            // Skip tips that are still in their post-timeout cooldown window.
            if (entry.getNextEligibleTick() > nowTick) {
                continue;
            }

            // Credibility-adjusted score: tips from low-credibility sources rank lower,
            // discouraging the planner from spending a scout slot on unreliable leads.
            float multiplier = entry.getSource() != null
                    ? reputationQuery.getCredibilityMultiplier(observerVillagerUUID, entry.getSource())
                    : 1.0f;
            float adjustedScore = entry.getWeight() * multiplier;

            if (adjustedScore > bestScore) {
                bestScore = adjustedScore;
                best = entry;
            }
        }

        return best;
    }

    /**
     * Returns true when the entry is a pending hearsay tip that has not yet been verified.
     * Does NOT check the cooldown; callers needing cooldown filtering should use
     * {@link #select(VillagerKnowledgeStore, UUID, ReputationQuery, long)}.
     */
    public static boolean isPendingTip(@Nonnull KnowledgeEntry entry) {
        return entry.isHearsay() && entry.getResolution() == KnowledgeResolution.UNRESOLVED;
    }

}
