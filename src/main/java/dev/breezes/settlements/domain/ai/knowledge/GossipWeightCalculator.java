package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Utility that computes the adjusted weight for a hearsay {@link KnowledgeEntry} received during a gossip exchange.
 * <p>
 * Weight formula: {@code originalWeight × stalenessDecay × hopPenalty × charismaBonus}
 * <ul>
 *   <li><b>Staleness decay</b> — exponential decay based on how many ticks have elapsed
 *       since the original observation. A fresh tip retains most of its weight; an old tip
 *       loses value quickly. Halving time is ~5 game minutes (6,000 ticks).</li>
 *   <li><b>Hop penalty</b> — each additional hop reduces weight. A second-hand tip
 *       is worth ~60% of a first-hand tip; a third-hand tip is ~36%. At the cap
 *       the entry is still stored but never re-shared, so the floor only matters for
 *       planning prioritization.</li>
 *   <li><b>CHA bonus</b> — high-charisma villagers are better at extracting meaning from
 *       social exchanges. The same tip lands with higher effective weight when the receiver
 *       has high CHA, making charming villagers natural information hubs.</li>
 * </ul>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class GossipWeightCalculator {

    /**
     * Halving time for staleness decay in ticks
     * <p>
     * 6,000 ticks ≈ 5 minutes real time at 20 tps ≈ 5 game hours (roughly half a day cycle)
     */
    public static final double STALENESS_HALVING_TICKS = 6_000.0;

    /**
     * Fraction of weight retained per gossip hop
     * <p>
     * 0.6 means each hop reduces the weight to 60% of its predecessor
     */
    public static final double HOP_DECAY_FACTOR = 0.6;

    /**
     * Computes the weight the receiver should assign to a hearsay entry
     *
     * @param originalWeight   the weight of the entry in the gossiper's store
     * @param originTick       the game tick at which the fact was originally observed
     * @param currentTick      the current game tick (at time of gossip exchange)
     * @param receiverGenetics the receiver's genetics profile (CHA is read)
     * @param incomingHop      the hop count the entry will have in the receiver's store (= gossiper's hop + 1)
     */
    public static float compute(float originalWeight,
                                long originTick,
                                long currentTick,
                                @Nonnull GeneticsProfile receiverGenetics,
                                int incomingHop) {
        float staleness = staleness(originTick, currentTick);
        float hopPenalty = hopPenalty(incomingHop);
        float chaBonus = charismaBonus(receiverGenetics);

        return originalWeight * staleness * hopPenalty * chaBonus;
    }

    public static float staleness(long originTick, long currentTick) {
        long ageTicks = Math.max(0L, currentTick - originTick);
        // Exponential decay: weight halves every STALENESS_HALVING_TICKS ticks.
        double decay = Math.pow(0.5, ageTicks / STALENESS_HALVING_TICKS);
        return (float) Math.max(0.01, decay);
    }

    public static float hopPenalty(int incomingHop) {
        // Clamp to the cap so penalty is deterministic regardless of how the hop was set.
        int clampedHop = Math.min(incomingHop, KnowledgeEntry.MAX_HOP_COUNT);
        return (float) Math.pow(HOP_DECAY_FACTOR, clampedHop);
    }

    public static float charismaBonus(@Nonnull GeneticsProfile genetics) {
        // CHA in [0, 1] → bonus multiplier in [0.7, 1.3]
        float cha = (float) genetics.getGeneValue(GeneType.CHARISMA);
        return 0.7f + (cha * 0.6f);
    }

}
