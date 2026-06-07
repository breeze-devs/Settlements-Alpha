package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiPredicate;

@Builder
public class PerceivedEntityExistsCondition<T extends BaseVillager, E extends Entity> implements IEntityCondition<T> {

    private final Class<E> entityType;
    @Nullable
    private final BiPredicate<T, E> filter;
    @Builder.Default
    private final int minimumCount = 1;

    private PerceivedEntityExistsCondition(@Nonnull Class<E> entityType,
                                           @Nullable BiPredicate<T, E> filter,
                                           int minimumCount) {
        if (minimumCount < 1) {
            throw new IllegalArgumentException("Minimum count must be at least 1");
        }

        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.filter = filter;
        this.minimumCount = minimumCount;
    }

    @Override
    public boolean test(@Nullable T villager) {
        if (villager == null) {
            return false;
        }

        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .ofType(this.entityType, candidate -> this.filter == null || this.filter.test(villager, candidate))
                .limit(this.minimumCount)
                .count() >= this.minimumCount;
    }

}
