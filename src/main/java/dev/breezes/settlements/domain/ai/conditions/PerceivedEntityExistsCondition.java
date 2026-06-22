package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.world.location.Location;
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
    @Nullable
    private final Integer completionRange;

    private PerceivedEntityExistsCondition(@Nonnull Class<E> entityType,
                                           @Nullable BiPredicate<T, E> filter,
                                           int minimumCount,
                                           @Nullable Integer completionRange) {
        if (minimumCount < 1) {
            throw new IllegalArgumentException("Minimum count must be at least 1");
        }
        if (completionRange != null && completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }

        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.filter = filter;
        this.minimumCount = minimumCount;
        this.completionRange = completionRange;
    }

    @Override
    public boolean test(@Nullable T villager) {
        if (villager == null) {
            return false;
        }

        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .ofType(this.entityType, candidate -> (this.filter == null || this.filter.test(villager, candidate))
                        && (this.completionRange == null
                        || villager.getNavigationManager().canReach(Location.fromEntity(candidate, false), this.completionRange)))
                .limit(this.minimumCount)
                .count() >= this.minimumCount;
    }

}
