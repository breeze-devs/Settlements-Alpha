package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.logging.ILogger;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public abstract class VillagerStateMachineBehavior extends StateMachineBehavior<BaseVillager> {

    private final HungerConfig hungerConfig;

    protected VillagerStateMachineBehavior(@Nonnull ILogger log,
                                           @Nonnull ITickable preconditionCheckCooldown,
                                           @Nonnull ITickable behaviorCoolDown,
                                           @Nonnull HungerConfig hungerConfig) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        this.hungerConfig = hungerConfig;
    }

    @Override
    protected double getCooldownMultiplier(@Nonnull BaseVillager villager) {
        float hunger = villager.getHunger();

        if (hunger >= this.hungerConfig.hungerBonusThreshold()) {
            return this.hungerConfig.cooldownBonusMultiplier();
        }
        if (hunger <= this.hungerConfig.cooldownScaleStartThreshold()) {
            double t = 1.0 - (hunger / this.hungerConfig.cooldownScaleStartThreshold());
            return 1.0 + (this.hungerConfig.maxCooldownMultiplier() - 1.0) * t;
        }
        return 1.0;
    }

    public double getHungerDrainModifier() {
        return 1.0;
    }

}
