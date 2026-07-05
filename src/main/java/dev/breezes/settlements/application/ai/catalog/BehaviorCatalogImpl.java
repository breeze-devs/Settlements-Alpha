package dev.breezes.settlements.application.ai.catalog;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.catalog.BehaviorCatalogEntry;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ServerScope
@CustomLog
public class BehaviorCatalogImpl implements IBehaviorCatalog {

    /**
     * Safety margin applied to {@link BehaviorPlanningMetadata#getEstimatedDuration()} (in-game
     * {@link GameTicks}) once converted to wall-clock {@link ClockTicks}, before comparing against
     * {@link BehaviorPlanningMetadata#getMaxRunDuration()} (already wall-clock). See
     * {@link #validateRunDurationCeiling} for why this is a loose sanity check rather than a hard
     * invariant.
     */
    private static final int MAX_RUN_DURATION_SAFETY_FACTOR = 2;

    private final Map<BehaviorKey, BehaviorPlanningMetadata> descriptors;
    private final Map<BehaviorKey, BehaviorDisplayMetadata> displayInfos;
    private final Map<BehaviorKey, Supplier<IBehavior<BaseVillager>>> factories;

    @Inject
    BehaviorCatalogImpl(Set<BehaviorCatalogEntry> entries) {
        this.descriptors = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::descriptor));
        this.displayInfos = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::displayInfo));
        this.factories = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::factory));

        this.descriptors.values().forEach(BehaviorCatalogImpl::validateRunDurationCeiling);
    }

    /**
     * Warns when a behavior's {@code maxRunDuration} looks implausibly short next to its
     * {@code estimatedDuration}. The two fields live on different clocks — {@link GameTicks} tracks
     * the Minecraft day cycle, {@link ClockTicks} tracks real elapsed time — so estimatedDuration is
     * converted to ClockTicks via {@link GameTicks#asClockTicks()} before comparing; comparing the
     * raw values directly would silently compare the wrong units.
     * <p>
     * This is a non-fatal sanity check, not an enforced invariant: estimatedDuration is a coarse
     * prompt-line hint rather than a per-behavior calibrated worst-case figure, so an occasional
     * warning on a behavior whose real-world worst case is already known to be safely under its
     * ceiling is expected and should be triaged case by case rather than silenced generically.
     */
    private static void validateRunDurationCeiling(@Nonnull BehaviorPlanningMetadata descriptor) {
        int estimatedAsWallClockTicks = descriptor.getEstimatedDuration().asClockTicks().getTicksAsInt();
        int maxRunDurationTicks = descriptor.getMaxRunDuration().getTicksAsInt();
        int minimumPlausibleTicks = estimatedAsWallClockTicks * MAX_RUN_DURATION_SAFETY_FACTOR;

        if (maxRunDurationTicks < minimumPlausibleTicks) {
            log.warn("Behavior '{}' has maxRunDuration ({} ticks) shorter than {}x its wall-clock-converted "
                            + "estimatedDuration ({} ticks) — it may be force-stopped mid-run on a typical execution",
                    descriptor.getKey().id(), maxRunDurationTicks, MAX_RUN_DURATION_SAFETY_FACTOR, estimatedAsWallClockTicks);
        }
    }

    @Override
    public Optional<BehaviorPlanningMetadata> getDescriptor(BehaviorKey key) {
        return Optional.ofNullable(this.descriptors.get(key));
    }

    @Override
    public Optional<BehaviorDisplayMetadata> getDisplayInfo(BehaviorKey key) {
        return Optional.ofNullable(this.displayInfos.get(key));
    }

    @Override
    public Optional<IBehavior<BaseVillager>> createBehavior(BehaviorKey key) {
        Supplier<IBehavior<BaseVillager>> factory = this.factories.get(key);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.get());
    }

    @Override
    public boolean exists(BehaviorKey key) {
        return this.descriptors.containsKey(key);
    }

    @Override
    public List<BehaviorPlanningMetadata> getAllDescriptors() {
        return List.copyOf(this.descriptors.values());
    }

}
