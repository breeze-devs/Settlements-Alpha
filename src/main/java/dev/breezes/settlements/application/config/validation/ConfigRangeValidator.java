package dev.breezes.settlements.application.config.validation;

import javax.annotation.Nonnull;

public final class ConfigRangeValidator {

    private ConfigRangeValidator() {
    }

    public static void validateMinLessThanOrEqualMax(@Nonnull String rangeName, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("%s min cannot be greater than max".formatted(rangeName));
        }
    }

}
