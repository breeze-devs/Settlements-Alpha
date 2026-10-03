package dev.breezes.settlements.domain.ai.threat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifyingHitTest {

    private static final UUID ATTACKER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final long HIT_TIME = 1_000L;

    @Test
    void remainsLive_oneTickBeforeExpiry() {
        // Arrange -- catches early expiry and measuring age from world time zero instead of the hit.
        QualifyingHit hit = new QualifyingHit(10.0f, HIT_TIME, ATTACKER);
        long justBeforeExpiry = HIT_TIME + QualifyingHit.EXPIRY.getTicks() - 1;

        // Act & Assert
        assertTrue(hit.isLive(justBeforeExpiry));
    }

    @Test
    void expires_atExpiry() {
        // Arrange -- counterexample: an inclusive boundary that keeps a hit live one tick too long.
        QualifyingHit hit = new QualifyingHit(10.0f, HIT_TIME, ATTACKER);
        long atExpiry = HIT_TIME + QualifyingHit.EXPIRY.getTicks();

        // Act & Assert
        assertFalse(hit.isLive(atExpiry));
    }

    @Test
    void constructor_rejectsANullAttacker() {
        // Arrange, Act & Assert -- counterexample: an observation with no attacker, leaving the flee
        // and the assessment nothing to resolve.
        assertThrows(NullPointerException.class, () -> new QualifyingHit(10.0f, 0L, null));
    }

}
