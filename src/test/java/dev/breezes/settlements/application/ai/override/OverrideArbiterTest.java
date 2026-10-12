package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
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
import java.util.List;
import java.util.Optional;

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
    void constructor_rejectsAPolicyRankedBehindALowerTier() {
        // Arrange — counterexample: an opportunistic policy ranked ahead of a reactive one goes unnoticed,
        // so an idle pickup is asked first and wins over a pending invite.
        OverridePolicy opportunistic = fakePolicy(OverrideTier.OPPORTUNISTIC);
        OverridePolicy reactive = fakePolicy(OverrideTier.REACTIVE);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OverrideArbiter(List.of(opportunistic, reactive), this.catalog, this.dayPlanHandoff));
    }

    @Test
    void constructor_acceptsPoliciesSharingATier() {
        // Arrange — counterexample: two policies in one tier rejected, leaving no way to rank within a tier.
        OverridePolicy first = fakePolicy(OverrideTier.REACTIVE);
        OverridePolicy second = fakePolicy(OverrideTier.REACTIVE);

        // Act & Assert
        assertDoesNotThrow(() -> new OverrideArbiter(List.of(first, second), this.catalog, this.dayPlanHandoff));
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

    @Test
    void isAdmissible_refusesATierBelowDutyDuringAReactiveActivity() {
        // Arrange — counterexample: the arbiter asks only the policy, so a trade invite whose policy admits every
        // activity is accepted while the villager shelters from a raid. The fake ignores the activity, so none is
        // needed.
        OverridePolicy invite = fakePolicy(OverrideTier.REACTIVE, true);

        // Act
        boolean admitted = OverrideArbiter.isAdmissible(invite, null, true);

        // Assert
        assertFalse(admitted);
    }

    @Test
    void isAdmissible_admitsATierBelowDutyOutsideAReactiveActivity() {
        // Arrange — counterexample: the reactivity flag ignored or forced on, so every invite and pickup is refused
        // during day activities as well.
        OverridePolicy invite = fakePolicy(OverrideTier.REACTIVE, true);

        // Act
        boolean admitted = OverrideArbiter.isAdmissible(invite, null, false);

        // Assert
        assertTrue(admitted);
    }

    @Test
    void isAdmissible_refusesWhenThePolicyRefusesTheActivity() {
        // Arrange — counterexample: only the tier rule is applied, so a policy's own narrowing, such as combat's
        // COMBAT-only admission, is ignored and its override starts or stays installed outside its activities.
        OverridePolicy narrowed = fakePolicy(OverrideTier.EMERGENCY, false);

        // Act
        boolean admitted = OverrideArbiter.isAdmissible(narrowed, null, false);

        // Assert
        assertFalse(admitted);
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

    private static OverridePolicy fakePolicy(OverrideTier tier) {
        return fakePolicy(tier, true);
    }

    private static OverridePolicy fakePolicy(OverrideTier tier, boolean admitsActivity) {
        return new OverridePolicy() {
            @Override
            public OverrideTier tier() {
                return tier;
            }

            @Override
            public boolean isAdmissibleDuring(@Nullable Activity activity) {
                return admitsActivity;
            }

            @Override
            public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
                return Optional.empty();
            }
        };
    }

}
