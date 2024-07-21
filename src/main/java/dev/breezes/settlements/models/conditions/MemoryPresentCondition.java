package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.models.memory.MemoryType;
import lombok.Builder;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Condition that evaluates if the entity has a memory that meets the specified predicate
 *
 * @param <T> the entity instance type, used to fetch the memory in the entity's brain
 */
@Builder
public class MemoryPresentCondition<T extends Entity & ISettlementsBrainEntity, M> implements IEntityCondition<T> {

    private final MemoryType<M> memoryType;

    @Builder.Default
    private final Predicate<M> memoryValuePredicate = memoryValue -> true;

    @Override
    public boolean test(@Nullable T brainEntity) {
        return Optional.ofNullable(brainEntity)
                .map(ISettlementsBrainEntity::getSettlementsBrain)
                .flatMap(brain -> brain.getMemory(this.memoryType))
                .map(memoryEntry -> this.memoryValuePredicate.test(memoryEntry.getMemoryValue()))
                .orElse(false);
    }

}
