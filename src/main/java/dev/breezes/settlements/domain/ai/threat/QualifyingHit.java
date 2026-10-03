package dev.breezes.settlements.domain.ai.threat;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A recorded hit that registered as a threat.
 */
public record QualifyingHit(float healthLost, long gameTime, @Nonnull UUID attackerId) {

    /**
     * How long a recorded hit stays live once nothing further qualifies.
     */
    public static final ClockTicks EXPIRY = ClockTicks.seconds(10);

    public QualifyingHit {
        Validate.notNull(attackerId, "attackerId must not be null");
    }

    /**
     * @param nowGameTime the instant to test liveness at
     * @return true while elapsed time since this hit is under {@link #EXPIRY}; false from the tick
     * elapsed time reaches EXPIRY onward
     */
    public boolean isLive(long nowGameTime) {
        return nowGameTime - this.gameTime < EXPIRY.getTicks();
    }

}
