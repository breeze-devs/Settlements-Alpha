package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Runs one behavior as an override with a duration limit.
 */
@CustomLog
final class SimpleBehaviorOverrideRunner implements OverrideRunner {

    /**
     * Bounds execution for a behavior whose catalog descriptor is absent.
     */
    private static final ClockTicks FALLBACK_MAX_DURATION = ClockTicks.seconds(120);

    private final BehaviorKey behaviorKey;
    private final IBehavior<BaseVillager> behavior;
    private final OverrideTier tier;
    private final Set<BehaviorChannel> occupiedChannels;
    private final int maxDurationTicks;

    private int elapsedTicks;
    private boolean wedged;

    SimpleBehaviorOverrideRunner(@Nonnull BehaviorKey behaviorKey,
                                 @Nonnull IBehavior<BaseVillager> behavior,
                                 @Nonnull OverrideTier tier,
                                 @Nonnull IBehaviorCatalog catalog) {
        this.behaviorKey = behaviorKey;
        this.behavior = behavior;
        this.tier = tier;

        BehaviorPlanningMetadata descriptor = catalog.getDescriptor(behaviorKey).orElse(null);
        this.maxDurationTicks = descriptor != null
                ? descriptor.getMaxRunDuration().getTicksAsInt()
                : FALLBACK_MAX_DURATION.getTicksAsInt();
        this.occupiedChannels = descriptor != null
                ? descriptor.getRequiredChannels()
                : Collections.emptySet();
    }

    @Override
    public void start(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        this.behavior.start(level, villager);
    }

    /**
     * Marks execution complete when the duration limit is exceeded or the wrapped tick throws a
     * RuntimeException.
     */
    @Override
    public void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        this.elapsedTicks += 1;
        // Check before delegation so execution cannot exceed its allotted number of ticks.
        if (this.elapsedTicks > this.maxDurationTicks) {
            log.behaviorWarn("Override '{}' exceeded max duration ({} ticks) for villager {}; force-stopping wedged override",
                    this.behaviorKey, this.maxDurationTicks, villager.getUUID());
            this.wedged = true;
            return;
        }

        try {
            this.behavior.tick(1, level, villager);
        } catch (RuntimeException e) {
            log.behaviorError("Override '{}' threw during tick for villager {}; force-stopping wedged override",
                    this.behaviorKey, villager.getUUID(), e);
            // Signal completion so a failed tick still follows the normal stop() cleanup contract.
            this.wedged = true;
        }
    }

    @Override
    public void stop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Avoid repeating teardown after natural completion or a previous stop.
        if (this.behavior.getStatus() != BehaviorStatus.STOPPED) {
            this.behavior.stop(level, villager);
        }
    }

    @Override
    public boolean isComplete() {
        return this.wedged || this.behavior.getStatus() == BehaviorStatus.STOPPED;
    }

    @Override
    public OverrideTier tier() {
        return this.tier;
    }

    @Override
    public Set<BehaviorChannel> occupiedChannels() {
        return this.occupiedChannels;
    }

    @Override
    public String diagnosticId() {
        return this.behaviorKey.toString();
    }

}
