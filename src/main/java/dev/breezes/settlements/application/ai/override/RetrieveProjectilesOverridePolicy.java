package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Override policy that sends a free fletcher to retrieve the stuck projectiles its scan found nearby.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class RetrieveProjectilesOverridePolicy implements OverridePolicy {

    private static final OverridePrecedence PRECEDENCE = OverridePrecedence.builder()
            .tier(OverrideTier.OPPORTUNISTIC)
            .order(20)
            .build();

    private static final Set<Activity> IDLE_WINDOW_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    @Override
    public OverridePrecedence precedence() {
        return PRECEDENCE;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        return activity != null && IDLE_WINDOW_ACTIVITIES.contains(activity);
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Policies apply to every villager, so a fletcher's own is scoped here
        if (!VillagerProfessionKey.FLETCHER.equals(villager.getProfession())) {
            return Optional.empty();
        }

        if (villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES).isEmpty()) {
            return Optional.empty();
        }

        // Retrieval is idle-time work, so it yields to any running day-plan behavior
        if (villager.getPlanRuntimeState().isBehaviorActive()) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.RETRIEVE_PROJECTILES));
    }

}
