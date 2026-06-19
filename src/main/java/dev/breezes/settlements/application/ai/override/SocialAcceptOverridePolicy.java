package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Override policy that fires when a pending trade or courtship invite is present.
 * <p>
 * Delegates to {@link OverrideTriggerDetector} for the actual registry poll so the
 * priority ordering (courtship &gt; trade) lives in one canonical place.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SocialAcceptOverridePolicy implements OverridePolicy {

    public static final int PRIORITY = 100;

    private final OverrideTriggerDetector overrideTriggerDetector;

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        BehaviorKey behaviorKey = this.overrideTriggerDetector.detect(villager.getUUID());
        if (behaviorKey == null) {
            return Optional.empty();
        }
        return Optional.of(OverrideRequest.builder().behaviorKey(behaviorKey).build());
    }

}
