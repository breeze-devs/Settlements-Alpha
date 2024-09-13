package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class NearbyBreedableAnimalPairExistsCondition<T extends BaseVillager> implements IEntityCondition<T> {

    private final double rangeHorizontal;
    private final double rangeVertical;
    private final Set<EntityType<? extends Animal>> breedableAnimalTypes;

    @Nullable
    private BreedablePair<?> breedablePair;

    public NearbyBreedableAnimalPairExistsCondition(double rangeHorizontal, double rangeVertical, Set<EntityType<? extends Animal>> breedableAnimalTypes) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.breedableAnimalTypes = breedableAnimalTypes;

        this.breedablePair = null;
        
        if (this.breedableAnimalTypes.isEmpty()) {
            throw new IllegalArgumentException("Breedable animal types must not be empty");
        }
    }

    @Override
    public boolean test(@Nullable T source) {
        if (source == null) {
            this.breedablePair = null;
//            log.warn("Source entity is null, returning empty targets");
            return false;
        }

        AABB scanBoundary = source.getBoundingBox().inflate(this.rangeHorizontal, this.rangeVertical, this.rangeHorizontal);
        Predicate<Entity> isBreedableType = (targetEntity) -> this.breedableAnimalTypes.contains(targetEntity.getType());
        List<Entity> nearbyEntities = source.level().getEntities(source, scanBoundary, isBreedableType);

        Map<EntityType<?>, Animal> singles = new HashMap<>();
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity == null || !nearbyEntity.isAlive() || !(nearbyEntity instanceof Animal animal)) {
                continue;
            }

            // Check breeding requirements
            if (animal.getAge() < 0 || !animal.canFallInLove()) {
                continue;
            }

            // Check if we've seen a breedable entity of the same type before
            if (!singles.containsKey(animal.getType())) {
                singles.put(animal.getType(), animal);
                continue;
            }

            // Successfully found a pair
            this.breedablePair = new BreedablePair<>(singles.get(animal.getType()), animal);
            break;
        }

        // TODO: log something
        return this.breedablePair != null;
    }

    public Optional<BreedablePair<?>> getBreedablePair() {
        return Optional.ofNullable(this.breedablePair);
    }

    @AllArgsConstructor
    @Getter
    public static class BreedablePair<T extends Animal> {
        private final T first;
        private final T second;
    }

}
