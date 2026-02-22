package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.logging.ILogger;
import dev.breezes.settlements.domain.time.ITickable;

import javax.annotation.Nonnull;

public abstract class BaseVillagerBehavior extends AbstractBehavior<BaseVillager> {

    protected BaseVillagerBehavior(@Nonnull ILogger log, @Nonnull ITickable preconditionCheckCooldown, @Nonnull ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
    }

}
