package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.petting;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.infrastructure.minecraft.entities.pet.Pettable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the nearest {@link Pettable} animal within scan range, ignoring tame state and ownership.
 * <p>
 * Scans for {@link LivingEntity} rather than a concrete animal type so any current or future
 * {@link Pettable} (wolf, cat, or later additions) is picked up through one query instead of a
 * per-species scan.
 */
public class NearbyPettableExistsCondition implements ICondition<BaseVillager> {

    private final int scanRangeHorizontal;
    private final int scanRangeVertical;

    @Getter
    private List<LivingEntity> targets;

    public NearbyPettableExistsCondition(int scanRangeHorizontal, int scanRangeVertical) {
        this.scanRangeHorizontal = scanRangeHorizontal;
        this.scanRangeVertical = scanRangeVertical;
        this.targets = Collections.emptyList();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        if (villager == null) {
            this.targets = Collections.emptyList();
            return false;
        }

        Optional<LivingEntity> nearest = this.findNearestPettable(villager);
        this.targets = nearest.map(Collections::singletonList).orElse(Collections.emptyList());
        return nearest.isPresent();
    }

    private Optional<LivingEntity> findNearestPettable(@Nonnull BaseVillager villager) {
        AABB scanBox = villager.getBoundingBox()
                .inflate(this.scanRangeHorizontal, this.scanRangeVertical, this.scanRangeHorizontal);
        List<LivingEntity> candidates = villager.level().getEntitiesOfClass(LivingEntity.class, scanBox,
                entity -> entity instanceof Pettable && entity.isAlive() && !entity.isRemoved());

        return candidates.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(villager)));
    }

}
