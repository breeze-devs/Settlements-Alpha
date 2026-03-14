package dev.breezes.settlements.application.config.validation;

public class BehaviorCooldownValidator {

    public static void validateRanges(int preconditionCheckCooldownMin,
                                      int preconditionCheckCooldownMax,
                                      int behaviorCooldownMin,
                                      int behaviorCooldownMax) {
        if (preconditionCheckCooldownMin > preconditionCheckCooldownMax) {
            throw new IllegalArgumentException("preconditionCheckCooldownMin cannot be greater than preconditionCheckCooldownMax");
        }
        if (behaviorCooldownMin > behaviorCooldownMax) {
            throw new IllegalArgumentException("behaviorCooldownMin cannot be greater than behaviorCooldownMax");
        }
    }

}
