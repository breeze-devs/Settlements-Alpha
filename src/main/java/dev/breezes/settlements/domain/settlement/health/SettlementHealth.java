package dev.breezes.settlements.domain.settlement.health;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A settlement's health, assessed from its living members and whether a bell stands in its claim.
 * <p>
 * Too few living members makes a settlement unhealthy whatever its bell. With enough members, a missing bell makes it
 * unhealthy, and a bell that may stand in an unloaded claimed chunk leaves its health undetermined.
 *
 * @param livingMembers    how many living members its roster lists; not negative
 * @param minimumResidents the fewest living members a healthy settlement has; not negative
 * @param bell             what the search for a bell in its claim found
 */
public record SettlementHealth(int livingMembers, int minimumResidents, BellPresence bell) {

    public SettlementHealth {
        Validate.isTrue(livingMembers >= 0, "Living members must not be negative: %d", livingMembers);
        Validate.isTrue(minimumResidents >= 0, "Minimum residents must not be negative: %d", minimumResidents);
        Validate.notNull(bell, "Bell presence must not be null");
    }

    public Status status() {
        if (this.tooFewResidents() || this.bell == BellPresence.MISSING) {
            return Status.UNHEALTHY;
        }
        return this.bell == BellPresence.FOUND ? Status.HEALTHY : Status.UNDETERMINED;
    }

    /**
     * Returns why the settlement is unhealthy, in declaration order.
     */
    public Set<Cause> causes() {
        EnumSet<Cause> causes = EnumSet.noneOf(Cause.class);
        if (this.bell == BellPresence.MISSING) {
            causes.add(Cause.NO_BELL);
        }
        if (this.tooFewResidents()) {
            causes.add(Cause.TOO_FEW_RESIDENTS);
        }
        return Collections.unmodifiableSet(causes);
    }

    private boolean tooFewResidents() {
        return this.livingMembers < this.minimumResidents;
    }

    public enum Status {

        HEALTHY,

        UNHEALTHY,

        /**
         * The members suffice, but no bell was found and one may stand in a claimed chunk that is not loaded.
         */
        UNDETERMINED

    }

    public enum Cause {

        /**
         * The bell search found the bell {@link BellPresence#MISSING}.
         */
        NO_BELL,

        /**
         * Fewer living members than the minimum residents.
         */
        TOO_FEW_RESIDENTS

    }

    /**
     * What the search for a bell in a settlement's claim found.
     */
    public enum BellPresence {

        /**
         * A bell stands in a loaded claimed chunk.
         */
        FOUND,

        /**
         * Every claimed chunk is loaded, and none holds a bell.
         */
        MISSING,

        /**
         * No loaded claimed chunk holds a bell, and some claimed chunk is not loaded.
         */
        NOT_KNOWABLE

    }

}
