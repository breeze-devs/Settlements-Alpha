package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.brain.ActivityArbiter;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Override policy that fires when another villager has sent this villager a courtship invite.
 * <p>
 * Ordered above {@link TradeAcceptOverridePolicy} within {@link OverrideTier#REACTIVE} so a villager
 * holding both a courtship and a trade invite at once accepts courtship first.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CourtshipAcceptOverridePolicy implements OverridePolicy {

    private static final OverridePrecedence PRECEDENCE = OverridePrecedence.builder()
            .tier(OverrideTier.REACTIVE)
            .order(0)
            .build();

    private final CourtshipSessionRegistry courtshipSessionRegistry;

    @Override
    public OverridePrecedence precedence() {
        return PRECEDENCE;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        // A villager answering danger, a raid or a bell does not stop to court
        return activity == null || !ActivityArbiter.isReactive(activity);
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (!this.courtshipSessionRegistry.hasInviteFor(villager.getUUID())) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.COURTSHIP_ACCEPT));
    }

}
