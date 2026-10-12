package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer.RaidCharting;
import dev.breezes.settlements.bootstrap.registry.activities.ActivityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Override policy that sends a cartographer sheltering from a raid wave to chart the raiders nobody has charted yet.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ChartRaidersOverridePolicy implements OverridePolicy {

    @Override
    public OverrideTier tier() {
        return OverrideTier.DUTY;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        // Charting serves only a live wave
        return activity == ActivityRegistry.RAID_HIDE;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Policies apply to every villager, so a cartographer's own is scoped here
        if (!VillagerProfessionKey.CARTOGRAPHER.equals(villager.getProfession())) {
            return Optional.empty();
        }

        if (villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.CHART_RAIDERS_COOLDOWN).isPresent()) {
            return Optional.empty();
        }

        Optional<Raid> raid = RaidCharting.raidInWave(level, villager);
        if (raid.isEmpty()) {
            return Optional.empty();
        }

        // A sketch with nobody to chart would play for nothing
        boolean anyChartable = raid.get().getAllRaiders().stream()
                .anyMatch(raider -> RaidCharting.isChartable(raider, level));
        if (!anyChartable) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.CHART_RAIDERS));
    }

}
