package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.threat.CombatOption;
import dev.breezes.settlements.application.ai.threat.CombatReadiness;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A nitwit's way to fight: pelting a hostile with eggs that slow it and knock it back.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class EggCombatOption implements CombatOption {

    private static final double NERVE_CONTRIBUTION = 0.7;
    private static final double REACH_BLOCKS = 10.0;

    // A nitwit's eggs reach any hostile in reach, so readiness judges no hostile and one instance serves every villager
    private static final CombatReadiness NITWIT_READINESS = hostile -> true;

    private static final BehaviorDisplayMetadata DISPLAY_INFO = BehaviorDisplayMetadata.builder()
            .displayNameKey("ui.settlements.behavior.combat.throw_eggs_at_hostile")
            .iconItemId(ResourceLocation.withDefaultNamespace("egg"))
            .build();

    private final BehaviorSupport support;

    @Override
    public double nerveContribution() {
        return NERVE_CONTRIBUTION;
    }

    @Override
    public double reachBlocks() {
        return REACH_BLOCKS;
    }

    @Override
    @Nullable
    public CombatReadiness prepare(@Nonnull BaseVillager villager) {
        // Eggs are conjured, so only a profession change can take this option away
        return VillagerProfessionKey.NITWIT.equals(villager.getProfession()) ? NITWIT_READINESS : null;
    }

    @Override
    public IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target) {
        return new ThrowEggsAtHostileBehavior(this.support, target, REACH_BLOCKS);
    }

    @Override
    public BehaviorDisplayMetadata displayInfo() {
        return DISPLAY_INFO;
    }

}
