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

    private final BehaviorSupport support;
    private final int experienceReward;

    protected VillagerStateMachineBehavior(@Nonnull ILogger log,
                                           @Nonnull ITickable preconditionCheckCooldown,
                                           @Nonnull ITickable behaviorCoolDown,
                                           @Nonnull BehaviorSupport support) {
        this(log, preconditionCheckCooldown, behaviorCoolDown, support, 0);
    }

    protected VillagerStateMachineBehavior(@Nonnull ILogger log,
                                           @Nonnull ITickable preconditionCheckCooldown,
                                           @Nonnull ITickable behaviorCoolDown,
                                           @Nonnull BehaviorSupport support,
                                           int experienceReward) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        this.support = support;
        this.experienceReward = experienceReward;
    }

    @Override
    protected double getCooldownMultiplier(@Nonnull BaseVillager villager) {
        float hunger = villager.getHunger();
        HungerConfig hungerConfig = this.support.getHungerConfig();

        if (hunger >= hungerConfig.hungerBonusThreshold()) {
            return hungerConfig.cooldownBonusMultiplier();
        }
        if (hunger <= hungerConfig.cooldownScaleStartThreshold()) {
            double t = 1.0 - (hunger / hungerConfig.cooldownScaleStartThreshold());
            return 1.0 + (hungerConfig.maxCooldownMultiplier() - 1.0) * t;
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
