package dev.breezes.settlements.application.ai.socialcue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialCueCadencePolicyTest {

    private static final long BASE_TICKS = 1200L;
    private static final double LOW_CHA_MULTIPLIER = 4.0;
    private static final double HIGH_CHA_MULTIPLIER = 0.5;
    private static final double JITTER = 0.25;

    // -------------------------------------------------------------------------
    // cooldownTicks — CHA × jitter interaction
    // -------------------------------------------------------------------------

    @Test
    void cooldownTicks_linearNeutralChaAndMidJitter_returnsMidpointMultiplier() {
        // Arrange: linear CHA=0.5 lands halfway between the configured endpoint multipliers.
        long expected = Math.round(BASE_TICKS * 2.25);

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 0.5, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.LINEAR, JITTER, 0.5);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_highChaAndMidJitter_returnsShorterCooldown() {
        // Arrange: CHA = 1.0 uses the configured high-charisma multiplier.
        long expected = Math.round(BASE_TICKS * HIGH_CHA_MULTIPLIER);

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 1.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, JITTER, 0.5);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_zeroChaAndMidJitter_returnsLongerCooldown() {
        // Arrange: CHA = 0.0 uses the configured low-charisma multiplier.
        long expected = Math.round(BASE_TICKS * LOW_CHA_MULTIPLIER);

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 0.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, JITTER, 0.5);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_exponentialNeutralCha_usesGeometricMidpoint() {
        // Arrange: exponential scaling interpolates in log space for wide multiplier ranges.
        long expected = Math.round(BASE_TICKS * Math.sqrt(LOW_CHA_MULTIPLIER * HIGH_CHA_MULTIPLIER));

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 0.5, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, 0.0, 0.5);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_minJitterRandom_appliesFloorFactor() {
        // Arrange: random01 = 0.0 → jitterFactor = (1.0 - 0.25) + 0 = 0.75
        // CHA = 1.0 → high-CHA multiplier = 0.5; combined = 0.5 * 0.75
        long expected = Math.round(BASE_TICKS * HIGH_CHA_MULTIPLIER * 0.75);

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 1.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.LINEAR, JITTER, 0.0);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_nearMaxJitterRandom_appliesNearCeilingFactor() {
        // Arrange: random01 ≈ 1 → jitterFactor ≈ (1.0 - 0.25) + 1*(2*0.25) = 1.25
        // CHA = 1.0 → high-CHA multiplier = 0.5; combined ≈ 0.5 * 1.25
        double random01 = 0.999999;
        long expected = Math.round(BASE_TICKS * HIGH_CHA_MULTIPLIER * ((1.0 - 0.25) + random01 * (2.0 * 0.25)));

        // Act
        long result = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 1.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.LINEAR, JITTER, random01);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void cooldownTicks_higherCharismaYieldsShorterOrEqualCooldown() {
        // Monotonic: a more charismatic villager should wait no longer than a less charismatic one
        // for the same random sample.
        long lowCha = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 0.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, JITTER, 0.5);
        long midCha = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 0.5, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, JITTER, 0.5);
        long highCha = SocialCueCadencePolicy.cooldownTicks(BASE_TICKS, 1.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, JITTER, 0.5);

        // Assert: strictly ordered
        assertTrue(highCha <= midCha, "CHA=1.0 should produce ≤ cooldown than CHA=0.5");
        assertTrue(midCha <= lowCha, "CHA=0.5 should produce ≤ cooldown than CHA=0.0");
    }

    @Test
    void cooldownTicks_neverNegative() {
        // Even with the smallest possible inputs the result must be non-negative.
        long result = SocialCueCadencePolicy.cooldownTicks(0L, 0.0, LOW_CHA_MULTIPLIER,
                HIGH_CHA_MULTIPLIER, SocialCueCooldownScaling.EXPONENTIAL, 0.0, 0.0);

        assertFalse(result < 0, "cooldownTicks must never return a negative value");
    }

    // -------------------------------------------------------------------------
    // initialScanPhaseTicks
    // -------------------------------------------------------------------------

    @Test
    void initialScanPhaseTicks_zeroRandom_returnsZero() {
        // Arrange
        long spreadTicks = 80L;

        // Act
        long result = SocialCueCadencePolicy.initialScanPhaseTicks(spreadTicks, 0.0);

        // Assert
        assertEquals(0L, result);
    }

    @Test
    void initialScanPhaseTicks_nearOneRandom_returnsValueInRange() {
        // Arrange
        long spreadTicks = 80L;
        double random01 = 0.999999;

        // Act
        long result = SocialCueCadencePolicy.initialScanPhaseTicks(spreadTicks, random01);

        // Assert: result must be in [0, spreadTicks)
        assertTrue(result >= 0L && result < spreadTicks,
                "phase offset must be in [0, spreadTicks) but was " + result);
    }

}
