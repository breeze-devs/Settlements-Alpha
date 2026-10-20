package dev.breezes.settlements.application.settlement.founding;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.List;

/**
 * How a founding at a bell ended: the settlement was founded, or nothing was stored.
 */
public sealed interface SettlementFoundingOutcome {

    /**
     * The settlement was founded and stored.
     *
     * @param settlement     the settlement as stored once its founding members joined, its claim holding only chunks
     *                       no other settlement claimed
     * @param requestedClaim the claim before the chunks other settlements claim were left out
     * @param members        the founders who became its members: the villagers gathered at the bell when the founding
     *                       began that were still alive in the level, and unassigned, when it was stored
     */
    record Founded(
            Settlement settlement,
            SettlementBounds requestedClaim,
            List<BaseVillager> members
    ) implements SettlementFoundingOutcome {

        public Founded {
            members = List.copyOf(members);
        }

    }

    /**
     * The founding was refused and nothing was stored.
     *
     * @param reason   why it was refused
     * @param claimant the settlement claiming the bell's chunk; present exactly when the reason is CLAIMED
     * @param found    how many founders or free chunks there were when the reason is TOO_FEW_FOUNDERS or
     *                 TOO_FEW_CHUNKS; otherwise 0
     * @param required how many the founding needed for those two reasons; otherwise 0
     */
    record Refused(
            Reason reason,
            @Nullable Settlement claimant,
            int found,
            int required
    ) implements SettlementFoundingOutcome {

        public Refused {
            Validate.notNull(reason, "Founding refusal reason must not be null");
            Validate.isTrue((claimant != null) == (reason == Reason.CLAIMED),
                    "A founding refusal names a claimant exactly when it is refused as claimed: %s", reason);
        }

        /**
         * Returns a refusal whose reason needs no detail.
         *
         * @throws IllegalArgumentException if the reason is CLAIMED
         */
        public static Refused of(Reason reason) {
            return new Refused(reason, null, 0, 0);
        }

        /**
         * Returns the refusal of a founding at a bell whose chunk the given settlement claims.
         */
        public static Refused claimedBy(Settlement claimant) {
            return new Refused(Reason.CLAIMED, claimant, 0, 0);
        }

        /**
         * Returns the refusal of a founding that found fewer founders or free chunks than it required.
         *
         * @throws IllegalArgumentException if the reason is CLAIMED
         */
        public static Refused shortOf(Reason reason, int found, int required) {
            return new Refused(reason, null, found, required);
        }

    }

    /**
     * Why a founding was refused.
     */
    enum Reason {

        /**
         * The level's dimension is not one in which a settlement may be founded.
         */
        DIMENSION_EXCLUDED,

        /**
         * The bell's position is not loaded.
         */
        BELL_UNLOADED,

        /**
         * Nothing at the position is a meeting point, so there is no bell there.
         */
        NO_BELL,

        /**
         * Another founding at the same bell has not finished.
         */
        IN_FLIGHT,

        /**
         * A settlement already claims the bell's chunk.
         */
        CLAIMED,

        /**
         * Fewer unassigned villagers gather at the bell than the founding requires.
         */
        TOO_FEW_FOUNDERS,

        /**
         * Fewer chunks around the bell are free of other settlements' claims than a founded settlement must claim.
         */
        TOO_FEW_CHUNKS,

        /**
         * The bell carries no name and none could be generated, because a name pool is empty.
         */
        UNNAMED,

        /**
         * The settlement store refused the settlement.
         */
        NOT_STORED

    }

}
