package dev.breezes.settlements.di.behavior;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Builder
public record BehaviorRegistration(
        @Nonnull VillagerProfessionKey profession,
        @Nonnull Supplier<IBehavior<BaseVillager>> behaviorFactory,
        @Nonnull Activity activity,
        int weight,
        int priority
) {

}
