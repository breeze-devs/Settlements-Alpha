package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.threat.CombatOption;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * A nitwit's way to fight: pelting a hostile with eggs that slow it and knock it back.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class EggCombatOption implements CombatOption {

    // Low priority: eggs only hinder, so any stronger option the villager also has should be selected first
    private static final int ORDER = 100;
    private static final double NERVE_CONTRIBUTION = 0.7;
    private static final double REACH_BLOCKS = 10.0;

    private final BehaviorSupport support;

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public double nerveContribution() {
        return NERVE_CONTRIBUTION;
    }

    @Override
    public double reachBlocks() {
        return REACH_BLOCKS;
    }

    @Override
    public boolean canEngage(@Nonnull BaseVillager villager, @Nonnull LivingEntity hostile) {
        return VillagerProfessionKey.NITWIT.equals(villager.getProfession());
    }

    @Override
    public boolean canContinue(@Nonnull BaseVillager villager) {
        // Eggs are conjured, so only a profession change can take this option away
        return VillagerProfessionKey.NITWIT.equals(villager.getProfession());
    }

    @Override
    public IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target) {
        return new ThrowEggsAtHostileBehavior(this.support, target, REACH_BLOCKS);
    }

}
