package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.bootstrap.registry.activities.ActivityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Override policy that hands a villager whose threat verdict is COMBAT to a {@link CombatOverrideRunner}.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CombatOverridePolicy implements OverridePolicy {

    private static final OverridePrecedence PRECEDENCE = OverridePrecedence.builder()
            .tier(OverrideTier.EMERGENCY)
            .order(0)
            .build();

    @Override
    public OverridePrecedence precedence() {
        return PRECEDENCE;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        return activity == ActivityRegistry.COMBAT;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (villager.getSettlementsBrain().threats().verdict() != ThreatVerdict.COMBAT) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.PreparedRunner(new CombatOverrideRunner(PRECEDENCE.tier())));
    }

}
