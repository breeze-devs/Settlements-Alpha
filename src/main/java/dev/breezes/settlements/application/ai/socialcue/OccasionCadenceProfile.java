package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * Small hardcoded profile table that biases social-cue cadence by the villager's current situation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OccasionCadenceProfile {

    public static final double DEFAULT_FACTOR = 1.0;

    private static final Map<Occasion, Double> FACTORS = factors();

    public static double factorFor(@Nonnull Occasion occasion) {
        return FACTORS.getOrDefault(occasion, DEFAULT_FACTOR);
    }

    private static Map<Occasion, Double> factors() {
        EnumMap<Occasion, Double> result = new EnumMap<>(Occasion.class);
        result.put(Occasion.MEET, 0.6);
        result.put(Occasion.IDLE, 1.0);
        result.put(Occasion.REST_DAY, 0.8);
        result.put(Occasion.WORK, 1.3);
        result.put(Occasion.PANIC, 2.0);
        result.put(Occasion.PRE_RAID, 2.0);
        result.put(Occasion.RAID, 2.0);
        return Map.copyOf(result);
    }

}
