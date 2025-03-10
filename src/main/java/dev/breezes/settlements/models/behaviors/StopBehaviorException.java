package dev.breezes.settlements.models.behaviors;

import javax.annotation.Nonnull;

/**
 * Can be thrown anywhere in the behavior to immediately stop the behavior
 */
public class StopBehaviorException extends RuntimeException {

    public StopBehaviorException(@Nonnull String message) {
        super(message);
    }

    public StopBehaviorException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

}
