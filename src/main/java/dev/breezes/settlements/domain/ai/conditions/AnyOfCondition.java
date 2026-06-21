package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public final class AnyOfCondition<T extends Entity> implements IEntityCondition<T> {

    private final List<IEntityCondition<T>> conditions;
    private final String description;

    @Builder
    public AnyOfCondition(@Nonnull List<IEntityCondition<T>> conditions,
                          @Nonnull String description) {
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("AnyOfCondition requires at least one child condition");
        }
        this.conditions = List.copyOf(conditions);
        this.description = description;
    }

    @Override
    public boolean test(@Nullable T entity) {
        return this.conditions.stream().anyMatch(condition -> condition.test(entity));
    }

    @Override
    public String description() {
        return this.description;
    }

}
