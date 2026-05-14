package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.logging.ILogger;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public abstract class VillagerStateMachineBehavior extends StateMachineBehavior<BaseVillager> {

    private final HungerConfig hungerConfig;
    private final int experienceReward;

    protected VillagerStateMachineBehavior(@Nonnull ILogger log,
                                           @Nonnull ITickable preconditionCheckCooldown,
                                           @Nonnull ITickable behaviorCoolDown,
                                           @Nonnull HungerConfig hungerConfig) {
        this(log, preconditionCheckCooldown, behaviorCoolDown, hungerConfig, 0);
    }

    protected VillagerStateMachineBehavior(@Nonnull ILogger log,
                                           @Nonnull ITickable preconditionCheckCooldown,
                                           @Nonnull ITickable behaviorCoolDown,
                                           @Nonnull HungerConfig hungerConfig,
                                           int experienceReward) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        this.hungerConfig = hungerConfig;
        this.experienceReward = experienceReward;
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

    protected final void rewardExperience(@Nonnull BaseVillager villager) {
        if (GeneralConfig.disableNaturalExperienceGain || this.experienceReward <= 0) {
            return;
        }

        villager.gainExperience(this.experienceReward);
    }

}
