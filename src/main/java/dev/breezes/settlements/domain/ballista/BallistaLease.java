package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.time.ClockTicks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A lease that indicates which villager is operating the ballista.
 * Expires automatically unless the holder keeps renewing it.
 */
public final class BallistaLease {

    /**
     * How often a holder renews its lease.
     */
    public static final ClockTicks RENEWAL_INTERVAL = ClockTicks.of(5);

    /**
     * How many renewal intervals a lease lasts, so a holder whose renewal slips keeps the machine rather than freeing
     * it to a second villager.
     */
    private static final int RENEWAL_INTERVALS_PER_LEASE = 4;

    private static final long DURATION_TICKS = RENEWAL_INTERVAL.getTicks() * RENEWAL_INTERVALS_PER_LEASE;

    @Nullable
    private UUID operatorId;
    private long expiresAtGameTime;

    /**
     * Whether the given villager may take the lease: nobody holds a live one, or that villager does.
     */
    public boolean isFreeFor(@Nonnull UUID candidateId, long gameTime) {
        return !this.isLiveAt(gameTime) || candidateId.equals(this.operatorId);
    }

    /**
     * Takes the lease for the given villager when it is free for them, renewing it if they already hold it.
     *
     * @return whether the villager holds the lease now
     */
    public boolean acquire(@Nonnull UUID operatorId, long gameTime) {
        if (!this.isFreeFor(operatorId, gameTime)) {
            return false;
        }

        this.operatorId = operatorId;
        this.expiresAtGameTime = gameTime + DURATION_TICKS;
        return true;
    }

    /**
     * Extends the given villager's live lease. It never takes the lease: a villager without a live one is refused.
     *
     * @return whether the villager still holds the lease
     */
    public boolean renew(@Nonnull UUID operatorId, long gameTime) {
        if (!this.isLiveAt(gameTime) || !operatorId.equals(this.operatorId)) {
            return false;
        }

        this.expiresAtGameTime = gameTime + DURATION_TICKS;
        return true;
    }

    /**
     * Gives up the lease if the given villager is its holder, and ignores anyone else.
     */
    public void release(@Nonnull UUID operatorId) {
        // A late release from a holder whose lease lapsed must not free the lease another villager has taken since
        if (operatorId.equals(this.operatorId)) {
            this.operatorId = null;
        }
    }

    private boolean isLiveAt(long gameTime) {
        return this.operatorId != null && gameTime < this.expiresAtGameTime;
    }

}
