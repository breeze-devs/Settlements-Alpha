package dev.breezes.settlements.domain.genetics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstitutionHealthResolver {

    private static final double MIN_HEALTH = 15.0;
    private static final double MAX_HEALTH = 40.0;
    private static final double BASE_HEALTH = 20.0;

    public static double resolveBonus(double constitution) {
        return (MIN_HEALTH + constitution * (MAX_HEALTH - MIN_HEALTH)) - BASE_HEALTH;
    }

}
