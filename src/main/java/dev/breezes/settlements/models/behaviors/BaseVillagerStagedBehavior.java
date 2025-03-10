package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.misc.ITickable;

import javax.annotation.Nonnull;

public abstract class BaseVillagerStagedBehavior extends AbstractBehavior<BaseVillager> {

    protected BaseVillagerStagedBehavior(@Nonnull ILogger log, @Nonnull ITickable preconditionCheckCooldown, @Nonnull ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
    }

}
