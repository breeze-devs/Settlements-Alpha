package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.planning.InvestigateTipSelector;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Override policy that triggers an urgent Investigate when a pending hearsay tip is
 * fresh and high-weight enough to justify pre-empting the current plan.
 * <p>
 * Urgency = tip weight × staleness-decay. Staleness uses a half-life matching
 * {@link dev.breezes.settlements.domain.ai.knowledge.GossipWeightCalculator} so a tip
 * older than ~10 real minutes (12 000 ticks at 20 tps) is never urgent on its own.
 * <p>
 * The threshold is intentionally conservative: overrides should be exceptions, not
 * the norm. Most hearsay is handled by the daily scout slot, not this urgency gate.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class UrgentInvestigateOverridePolicy implements OverridePolicy {

    public static final int PRIORITY = 200;

    /**
     * Urgency threshold: tip weight × freshness must exceed this to pre-empt the plan.
     * Tips older than ~1 game hour fall below this at typical weights.
     */
    private static final double URGENCY_THRESHOLD = 2.5;

    /**
     * Halving time in ticks for freshness decay. Mirrors GossipWeightCalculator.
     */
    private static final double FRESHNESS_HALVING_TICKS = 6_000.0;

    private final ReputationQuery reputationQuery;

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Skip the full scan if the store is empty; most ticks for most villagers take this path.
        if (villager.getKnowledgeStore().isEmpty()) {
            return Optional.empty();
        }

        long nowTick = level.getGameTime();
        KnowledgeEntry tip = InvestigateTipSelector.select(
                villager.getKnowledgeStore(), villager.getUUID(), this.reputationQuery, nowTick);
        if (tip == null) {
            return Optional.empty();
        }

        long ageTicks = Math.max(0L, nowTick - tip.getOriginTimestampTick());
        double freshness = Math.pow(0.5, ageTicks / FRESHNESS_HALVING_TICKS);
        double urgencyScore = tip.getWeight() * freshness;

        if (urgencyScore >= URGENCY_THRESHOLD) {
            return Optional.of(OverrideRequest.builder().behaviorKey(BehaviorKey.INVESTIGATE).build());
        }

        return Optional.empty();
    }

}
