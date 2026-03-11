package dev.breezes.settlements.application.ui.behavior.snapshot;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

@Builder
public record BehaviorBinding(
        @Nonnull IBehavior<BaseVillager> behavior,
        int priority,
        int uiBehaviorIndex,
        @Nonnull Set<Activity> registeredActivities
) {

    public BehaviorBinding {
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(registeredActivities, "registeredActivities");
        registeredActivities = Set.copyOf(registeredActivities);
    }

    public BehaviorBinding copyWithIndex(int newIndex) {
        return new BehaviorBinding(this.behavior, this.priority, newIndex, this.registeredActivities);
    }

}
