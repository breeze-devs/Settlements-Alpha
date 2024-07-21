package dev.breezes.settlements.models.conditions;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public interface ICondition<T> extends Predicate<T> {

    /**
     * Override to add nullable annotation
     */
    @Override
    boolean test(@Nullable T t);

}
