package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * Combat options that decline every check, for tests that need a distinct option but none of its behavior.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FakeCombatOptions {

    static CombatOption withOrder(int order) {
        return new CombatOption() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public double nerveContribution() {
                return 0.0;
            }

            @Override
            public double reachBlocks() {
                return 0.0;
            }

            @Override
            public boolean canEngage(@Nonnull BaseVillager villager, @Nonnull LivingEntity hostile) {
                return false;
            }

            @Override
            public boolean canContinue(@Nonnull BaseVillager villager) {
                return false;
            }

            @Override
            public IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target) {
                throw new UnsupportedOperationException("A fake combat option launches no action");
            }
        };
    }

}
