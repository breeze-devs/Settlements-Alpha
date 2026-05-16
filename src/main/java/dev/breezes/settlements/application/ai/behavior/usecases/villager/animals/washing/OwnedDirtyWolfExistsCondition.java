package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import lombok.Getter;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OwnedDirtyWolfExistsCondition implements ICondition<BaseVillager> {

    private final int scanRangeHorizontal;
    private final int scanRangeVertical;

    @Getter
    private List<SettlementsWolf> targets;

    public OwnedDirtyWolfExistsCondition(int scanRangeHorizontal, int scanRangeVertical) {
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

        Optional<SettlementsWolf> target = villager.getBrain()
                .getMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType())
                .flatMap(ownedWolfIds -> this.findFirstOwnedDirtyWolf(villager, ownedWolfIds));
        this.targets = target.map(Collections::singletonList).orElse(Collections.emptyList());
        return target.isPresent();
    }

    private Optional<SettlementsWolf> findFirstOwnedDirtyWolf(@Nonnull BaseVillager villager,
                                                              @Nonnull List<UUID> ownedWolfIds) {
        if (ownedWolfIds.isEmpty()) {
            return Optional.empty();
        }

        AABB scanBox = villager.getBoundingBox()
                .inflate(this.scanRangeHorizontal, this.scanRangeVertical, this.scanRangeHorizontal);
        List<SettlementsWolf> nearbyWolves = villager.level().getEntitiesOfClass(SettlementsWolf.class, scanBox);
        for (UUID ownedWolfId : ownedWolfIds) {
            Optional<SettlementsWolf> matchingWolf = nearbyWolves.stream()
                    .filter(wolf -> this.isEligibleOwnedDirtyWolf(wolf, ownedWolfId))
                    .findFirst();
            if (matchingWolf.isPresent()) {
                return matchingWolf;
            }
        }
        return Optional.empty();
    }

    private boolean isEligibleOwnedDirtyWolf(@Nonnull SettlementsWolf wolf,
                                             @Nonnull UUID ownedWolfId) {
        return wolf.getUUID().equals(ownedWolfId)
                && wolf.isAlive()
                && !wolf.isRemoved()
                && wolf.isDirty();
    }

}
