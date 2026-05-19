package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class NearbyBreedableAnimalPairExistsCondition<T extends BaseVillager> implements IEntityCondition<T> {

    private final double rangeHorizontal;
    private final double rangeVertical;
    private final EntityType<? extends Animal> breedableAnimalType;

    @Nullable
    private BreedablePair<?> breedablePair;

    public NearbyBreedableAnimalPairExistsCondition(double rangeHorizontal,
                                                    double rangeVertical,
                                                    EntityType<? extends Animal> breedableAnimalType) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.breedableAnimalType = breedableAnimalType;
        this.breedablePair = null;
    }

    @Override
    public boolean test(@Nullable T source) {
        if (source == null) {
            this.breedablePair = null;
            return false;
        }

        AABB scanBoundary = source.getBoundingBox().inflate(this.rangeHorizontal, this.rangeVertical, this.rangeHorizontal);
        Predicate<Entity> isBreedableType = (targetEntity) -> targetEntity.getType() == this.breedableAnimalType;
        List<Entity> nearbyEntities = source.level().getEntities(source, scanBoundary, isBreedableType);

        Animal firstCandidate = null;
        for (Entity nearbyEntity : nearbyEntities) {
            if (!nearbyEntity.isAlive() || !(nearbyEntity instanceof Animal animal)) {
                continue;
            }

            // Both partners must be adults that are not already in love
            if (animal.getAge() != 0 || !animal.canFallInLove()) {
                continue;
            }

            if (firstCandidate == null) {
                firstCandidate = animal;
                continue;
            }

            this.breedablePair = new BreedablePair<>(firstCandidate, animal);
            break;
        }

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
