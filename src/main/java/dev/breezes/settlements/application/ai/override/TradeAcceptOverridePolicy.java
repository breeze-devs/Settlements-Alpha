package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
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
 * Override policy that fires when another villager has sent this villager a trade invite.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TradeAcceptOverridePolicy implements OverridePolicy {

    private final TradeSessionRegistry tradeSessionRegistry;

    @Override
    public OverrideTier tier() {
        return OverrideTier.REACTIVE;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        return true;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (!this.tradeSessionRegistry.hasInviteFor(villager.getUUID())) {
            return Optional.empty();
        }
        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.TRADE_ACCEPT));
    }

}
