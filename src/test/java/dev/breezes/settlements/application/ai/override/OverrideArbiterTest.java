package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class OverrideArbiterTest {

    @Mock
    private IBehaviorCatalog catalog;

    @Mock
    private DayPlanHandoff dayPlanHandoff;

    @Test
    void constructor_rejectsTwoPoliciesDeclaringTheSamePrecedence() {
        // Arrange — counterexample: two policies both claim (REACTIVE, 0), and nothing catches it,
        // leaving their relative evaluation order to depend on incidental Set iteration order.
        OverridePolicy first = fakePolicy(new OverridePrecedence(OverrideTier.REACTIVE, 0));
        OverridePolicy second = fakePolicy(new OverridePrecedence(OverrideTier.REACTIVE, 0));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OverrideArbiter(Set.of(first, second), this.catalog, this.dayPlanHandoff));
    }

    @Test
    void constructor_acceptsPoliciesWithDistinctPrecedences() {
        // Arrange
        OverridePolicy first = fakePolicy(new OverridePrecedence(OverrideTier.REACTIVE, 0));
        OverridePolicy second = fakePolicy(new OverridePrecedence(OverrideTier.REACTIVE, 1));

        // Act & Assert
        assertDoesNotThrow(() -> new OverrideArbiter(Set.of(first, second), this.catalog, this.dayPlanHandoff));
    }

    @Test
    void isPlanInterruptible_trueWhenNoDayPlanBehaviorIsRunning() {
        // Counterexample: a villager between plan slots (no descriptor at all) treated as protected
        // would never accept a reactive or opportunistic request until its next chore starts.
        assertTrue(OverrideArbiter.isPlanInterruptible(null));
    }

    @Test
    void isPlanInterruptible_trueWhenTheRunningBehaviorIsInterruptible() {
        assertTrue(OverrideArbiter.isPlanInterruptible(descriptor(true)));
    }

    @Test
    void isPlanInterruptible_falseWhenTheRunningBehaviorIsNotInterruptible() {
        assertFalse(OverrideArbiter.isPlanInterruptible(descriptor(false)));
    }

    private static BehaviorPlanningMetadata descriptor(boolean interruptible) {
        return BehaviorPlanningMetadata.builder()
                .key(BehaviorKey.HARVEST_MELON)
                .displayName("Test")
                .description("Test descriptor")
                .estimatedDuration(GameTicks.minutes(1))
                .preconditionSummary("test")
                .interruptible(interruptible)
                .build();
    }

    private static OverridePolicy fakePolicy(OverridePrecedence precedence) {
        return new OverridePolicy() {
            @Override
            public OverridePrecedence precedence() {
                return precedence;
            }

            @Override
            public boolean isAdmissibleDuring(@Nullable Activity activity) {
                return true;
            }

            @Override
            public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
                return Optional.empty();
            }
        };
    }

}
