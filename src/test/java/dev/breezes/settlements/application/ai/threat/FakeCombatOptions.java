package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Combat options that decline every check, for tests that need a distinct option but none of its behavior.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FakeCombatOptions {

    static CombatOption inert() {
        return new CombatOption() {
            @Override
            public double nerveContribution() {
                return 0.0;
            }

            @Override
            public double reachBlocks() {
                return 0.0;
            }

            @Override
            @Nullable
            public CombatReadiness prepare(@Nonnull BaseVillager villager) {
                return null;
            }

            @Override
            public IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target) {
                throw new UnsupportedOperationException("A fake combat option launches no action");
            }
        };
    }

}
