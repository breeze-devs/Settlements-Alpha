package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.GameTicks;

import java.util.Random;

/**
 * Deterministic per-villager timing personality, derived from a stable seed (typically the villager UUID).
 * <p>
 * Encodes two time offsets that break village-wide synchronization:
 * <ul>
 *   <li>A correlated wake/sleep shift so early birds wake AND sleep earlier, night owls later.</li>
 *   <li>A modest meal-anchor shift that disrupts exact lunch/dinner lockstep while preserving the
 *       intentional village-wide overlap window (kept small for social availability).</li>
 * </ul>
 * <p>
 * Both offsets are derived from a single {@link Random} seeded at construction time — draws happen
 * in a fixed order, so the same seed always produces the same pair of values across all call sites.
 * This determinism is load-bearing: {@code PlanRunner} recomputes wake on different ticks and the
 * generator's epoch must always agree with the plan's {@code wakeAtAbsoluteTick}.
 */
public final class Chronotype {

    /**
     * Maximum wake/sleep shift in either direction.
     * ±40 game-minutes spreads same-profession villages across a meaningful window.
     */
    private static final int WAKE_SLEEP_RANGE = GameTicks.minutes(40).getTicksAsInt();

    /**
     * Maximum meal shift in either direction.
     * Kept smaller than the wake range so the village still has a rough shared lunch window.
     */
    private static final int MEAL_RANGE = GameTicks.minutes(12).getTicksAsInt();

    private final int wakeSleepOffsetTicks;
    private final int mealOffsetTicks;

    private Chronotype(long seed) {
        // Pre-mix the seed through Murmur3's finalisation step so that villagers with consecutive UUID
        // bits (or sequential seeds in tests) land in unrelated parts of the distribution — raw
        // java.util.Random with close seeds (0, 1, 2 …) produces highly correlated first draws.
        // A single seeded Random then draws both values in a fixed order — never use live RNG here
        // because PlanRunner must reproduce the same wake tick across separate invocations.
        Random rng = new Random(murmur64(seed));
        this.wakeSleepOffsetTicks = (int) Math.round(rng.nextDouble() * 2 * WAKE_SLEEP_RANGE) - WAKE_SLEEP_RANGE;
        this.mealOffsetTicks = (int) Math.round(rng.nextDouble() * 2 * MEAL_RANGE) - MEAL_RANGE;
    }

    /**
     * 64-bit Murmur3 finalisation mix — maps any long to a well-distributed long with no fixed points.
     * Pure bit arithmetic, deterministic, no external dependencies.
     */
    private static long murmur64(long v) {
        v ^= (v >>> 33);
        v *= 0xff51afd7ed558ccdL;
        v ^= (v >>> 33);
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= (v >>> 33);
        return v;
    }

    /**
     * Produces the chronotype for a villager identified by {@code seed}.
     * Repeated calls with the same seed return identical offsets (deterministic, no allocation caching needed).
     */
    public static Chronotype of(long seed) {
        return new Chronotype(seed);
    }

    /**
     * Symmetric offset in game ticks applied to BOTH wake and bedtime so early-bird / night-owl
     * personality is consistent across the day. Range: {@code [-WAKE_SLEEP_RANGE, +WAKE_SLEEP_RANGE]}.
     */
    public int wakeSleepOffsetTicks() {
        return this.wakeSleepOffsetTicks;
    }

    /**
     * Symmetric offset in game ticks applied to the lunch and dinner meal anchors.
     * Range: {@code [-MEAL_RANGE, +MEAL_RANGE]}. Breakfast moves with wake automatically and is
     * not affected by this field.
     */
    public int mealOffsetTicks() {
        return this.mealOffsetTicks;
    }

}
