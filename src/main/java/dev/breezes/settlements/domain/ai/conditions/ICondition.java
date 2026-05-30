package dev.breezes.settlements.domain.ai.conditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public interface ICondition<T> extends Predicate<T> {

    /**
     * Override to add nullable annotation
     */
    @Override
    boolean test(@Nullable T t);

    default String description() {
        return this.getClass().getSimpleName();
    }

    @Nonnull
    static <T> ICondition<T> named(@Nonnull String description, @Nonnull Predicate<T> predicate) {
        return new ICondition<>() {
            @Override
            public boolean test(@Nullable T value) {
                return predicate.test(value);
            }

            @Override
            public @Nonnull String description() {
                return description;
            }
        };
    }

}
