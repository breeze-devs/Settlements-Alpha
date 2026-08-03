package dev.breezes.settlements.domain.genetics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstitutionSizeResolver {

    private static final double MIN_SCALE = 0.8;
    // The 1.0 ceiling must never be raised: a taller villager would no longer fit through a door
    private static final double MAX_SCALE = 1.0;
    private static final double BASE_SCALE = 1.0;

    public static double resolveBonus(double constitution) {
        double resolvedScale = MIN_SCALE + constitution * (MAX_SCALE - MIN_SCALE);
        return resolvedScale - BASE_SCALE;
    }

}
