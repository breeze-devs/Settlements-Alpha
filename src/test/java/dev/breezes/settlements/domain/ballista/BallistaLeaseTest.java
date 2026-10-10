package dev.breezes.settlements.domain.ballista;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaLeaseTest {

    private static final UUID HOLDER = new UUID(0L, 1L);
    private static final UUID OTHER = new UUID(0L, 2L);
    private static final UUID THIRD = new UUID(0L, 3L);

    private static final long ACQUIRED_AT = 1_000L;

    // Far longer than any lease lasts unrenewed, so each test reads a lease left alone this long as lapsed
    private static final long LONG_AFTER = ACQUIRED_AT + 100_000L;

    private static final long RENEWAL_INTERVAL_TICKS = BallistaLease.RENEWAL_INTERVAL.getTicks();

    @Test
    void acquire_refused_whenAnotherHoldsALiveLease() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        boolean acquired = lease.acquire(OTHER, ACQUIRED_AT + 1);

        // Assert: taken, two villagers would converge on one machine
        assertFalse(acquired);
    }

    @Test
    void acquire_succeeds_whenTheHoldersLeaseHasLapsed() {
        // Arrange: a holder that stopped without releasing
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        boolean acquired = lease.acquire(OTHER, LONG_AFTER);

        // Assert: refused, an abandoned lease would keep the machine from every other villager for good
        assertTrue(acquired);
    }

    @Test
    void isFreeFor_holder_whileItsLeaseIsLive() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        boolean free = lease.isFreeFor(HOLDER, ACQUIRED_AT + 1);

        // Assert: read as taken, the machine would look unavailable to the very villager operating it
        assertTrue(free);
    }

    @Test
    void isFreeFor_another_isFalse_afterTheHolderMissesOneRenewal() {
        // Arrange: the holder acquires and then misses its next renewal
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act: just as the renewal after the missed one comes due
        boolean free = lease.isFreeFor(OTHER, ACQUIRED_AT + 2 * RENEWAL_INTERVAL_TICKS);

        // Assert: a lease lasting no more than two renewal intervals hands the machine to a second villager the moment
        // one renewal slips
        assertFalse(free);
    }

    @Test
    void renew_keepsTheLease_whileTheHolderRenewsOnItsInterval() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        for (long gameTime = ACQUIRED_AT + RENEWAL_INTERVAL_TICKS; gameTime <= LONG_AFTER;
             gameTime += RENEWAL_INTERVAL_TICKS) {
            lease.renew(HOLDER, gameTime);
        }

        // Assert: a renewal that did not extend the lease would let it lapse under a villager still operating
        assertFalse(lease.isFreeFor(OTHER, LONG_AFTER));
    }

    @Test
    void renew_refused_forAVillagerThatDoesNotHoldIt() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        boolean renewed = lease.renew(OTHER, ACQUIRED_AT + 1);

        // Assert: the other villager would be told it holds a machine the holder is operating
        assertFalse(renewed);
        assertFalse(lease.isFreeFor(OTHER, ACQUIRED_AT + 1));
    }

    @Test
    void renew_refused_onceTheLeaseHasLapsed() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        boolean renewed = lease.renew(HOLDER, LONG_AFTER);

        // Assert: revived, the holder would carry on as if it had never lost a machine every other villager has meanwhile
        // read as free
        assertFalse(renewed);
    }

    @Test
    void release_freesTheLease_forAnotherAtOnce() {
        // Arrange
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);

        // Act
        lease.release(HOLDER);

        // Assert: a release that did nothing would hold the machine until the lease lapsed
        assertTrue(lease.isFreeFor(OTHER, ACQUIRED_AT + 1));
    }

    @Test
    void release_ignored_whenAnotherTookTheLeaseAfterTheReleasersLapsed() {
        // Arrange: the first holder's lease lapses and another villager takes the machine
        BallistaLease lease = new BallistaLease();
        lease.acquire(HOLDER, ACQUIRED_AT);
        lease.acquire(OTHER, LONG_AFTER);

        // Act: the first holder's release arrives late
        lease.release(HOLDER);

        // Assert: freed, a third villager would converge on the machine the second is operating
        assertFalse(lease.isFreeFor(THIRD, LONG_AFTER + 1));
    }

}
