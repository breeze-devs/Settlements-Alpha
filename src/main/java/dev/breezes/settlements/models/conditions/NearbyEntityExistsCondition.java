package dev.breezes.settlements.models.conditions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 */
public class NearbyEntityExistsCondition<T extends Entity, E extends Entity> implements IEntityCondition<T> {

    private final double rangeHorizontal;
    private final double rangeVertical;
    private final EntityType<E> targetEntityType;
    private final IEntityCondition<E> extraEntityCondition;
    private final int minimumTargetCount;

    @Nullable
    private List<E> targets;

    public NearbyEntityExistsCondition(double rangeHorizontal, double rangeVertical,
                                       @Nonnull EntityType<E> targetEntityType, @Nullable IEntityCondition<E> extraEntityCondition,
                                       int minimumTargetCount) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.targetEntityType = targetEntityType;
        this.extraEntityCondition = extraEntityCondition == null ? (entity) -> true : extraEntityCondition;
        this.minimumTargetCount = minimumTargetCount;

        if (this.minimumTargetCount < 1) {
            throw new IllegalArgumentException("Minimum target count must be at least 1");
        }
    }

    @Override
    public boolean test(@Nullable T entity) {
        if (entity == null) {
            this.targets = Collections.emptyList();
            return false;
        }

        AABB scanBoundary = entity.getBoundingBox().inflate(this.rangeHorizontal, this.rangeVertical, this.rangeHorizontal);
        Predicate<Entity> entityPredicate = (targetEntity) -> targetEntity.getType() == this.targetEntityType
                && this.extraEntityCondition.test(this.targetEntityType.tryCast(targetEntity));
        Stream<E> nearbyEntitiesStream = entity.level().getEntities(entity, scanBoundary, entityPredicate).stream()
                .map(targetEntityType::tryCast)
                .filter(Objects::nonNull);

        if (this.minimumTargetCount == 1) {
            // If we only need one target, we can just find any target to optimize efficiency
            this.targets = nearbyEntitiesStream.findAny()
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            this.targets = nearbyEntitiesStream.toList();
        }

        return this.targets.size() >= this.minimumTargetCount;
    }

    /**
     * Get the list of targets that satisfy the condition
     * <p>
     * Note that the size of this list may be less than the minimum target count
     */
    public List<E> getTargets() {
        return Optional.ofNullable(this.targets)
                .orElse(Collections.emptyList());
    }

}
