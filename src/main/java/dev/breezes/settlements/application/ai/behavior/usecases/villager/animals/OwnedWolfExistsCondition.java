package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

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
import java.util.function.Predicate;

/**
 * Finds owned wolves within scan range that satisfy an optional extra predicate.
 * The predicate keeps this class reusable: callers supply what "eligible" means
 * (e.g. hurt, dirty) without embedding that logic here.
 */
public class OwnedWolfExistsCondition implements ICondition<BaseVillager> {

    private final int scanRangeHorizontal;
    private final int scanRangeVertical;
    private final Predicate<SettlementsWolf> wolfFilter;

    @Getter
    private List<SettlementsWolf> targets;

    public OwnedWolfExistsCondition(int scanRangeHorizontal,
                                    int scanRangeVertical,
                                    @Nonnull Predicate<SettlementsWolf> wolfFilter) {
        this.scanRangeHorizontal = scanRangeHorizontal;
        this.scanRangeVertical = scanRangeVertical;
        this.wolfFilter = wolfFilter;
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
                .flatMap(ownedWolfIds -> this.findFirstMatchingOwnedWolf(villager, ownedWolfIds));
        this.targets = target.map(Collections::singletonList).orElse(Collections.emptyList());
        return target.isPresent();
    }

    private Optional<SettlementsWolf> findFirstMatchingOwnedWolf(@Nonnull BaseVillager villager,
                                                                 @Nonnull List<UUID> ownedWolfIds) {
        if (ownedWolfIds.isEmpty()) {
            return Optional.empty();
        }

        AABB scanBox = villager.getBoundingBox()
                .inflate(this.scanRangeHorizontal, this.scanRangeVertical, this.scanRangeHorizontal);
        List<SettlementsWolf> nearbyWolves = villager.level().getEntitiesOfClass(SettlementsWolf.class, scanBox);
        for (UUID ownedWolfId : ownedWolfIds) {
            Optional<SettlementsWolf> match = nearbyWolves.stream()
                    .filter(wolf -> this.isEligible(wolf, ownedWolfId, villager))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private boolean isEligible(@Nonnull SettlementsWolf wolf,
                               @Nonnull UUID ownedWolfId,
                               @Nonnull BaseVillager villager) {
        return wolf.getUUID().equals(ownedWolfId)
                && wolf.isAlive()
                && !wolf.isRemoved()
                && villager.getUUID().equals(wolf.getOwnerUUID())
                && this.wolfFilter.test(wolf);
    }

}
