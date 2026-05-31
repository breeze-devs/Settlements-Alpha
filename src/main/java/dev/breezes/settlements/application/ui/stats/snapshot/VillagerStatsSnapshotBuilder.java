package dev.breezes.settlements.application.ui.stats.snapshot;

import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.application.ui.stats.model.DemandDisplayEntry;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ServerSide
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerStatsSnapshotBuilder {

    private final VillagerWallet villagerWallet;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DemandEvaluator demandEvaluator;
    private final IBehaviorCatalog behaviorCatalog;

    @Nonnull
    public VillagerStatsSnapshot buildStats(@Nonnull BaseVillager villager, long gameTime) {
        String villagerName = villager.hasCustomName() ? villager.getName().getString() : null;
        String professionKey = villager.getVillagerData().getProfession().toString();
        int expertiseLevel = villager.getVillagerData().getLevel();

        double[] geneValues = buildGeneValues(villager);


        Activity activity = villager.getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE);
        SchedulePhase schedulePhase = mapSchedulePhase(activity);

        Optional<ActiveBehaviorInfo> activeBehavior = this.findActiveBehavior(villager);
        return VillagerStatsSnapshot.builder()
                .gameTime(gameTime)
                .villagerEntityId(villager.getId())
                .villagerName(villagerName)
                .professionKey(professionKey)
                .expertiseLevel(expertiseLevel)
                .currentHealth(villager.getHealth())
                .maxHealth(villager.getMaxHealth())
                .geneValues(geneValues)
                .homePos(extractMemoryPos(villager, MemoryModuleType.HOME).orElse(null))
                .workstationPos(extractMemoryPos(villager, MemoryModuleType.JOB_SITE).orElse(null))
                .activeBehaviorNameKey(activeBehavior.map(ActiveBehaviorInfo::nameKey).orElse(null))
                .activeBehaviorIconId(activeBehavior.map(ActiveBehaviorInfo::iconId).orElse(null))
                .schedulePhase(schedulePhase)
                .reputation(0) // TODO: wire to actual villager reputation data
                .hunger(villager.getHunger())
                .walletBalance(villagerWallet.getBalance(villager))
                .build();
    }

    public VillagerInventorySnapshot buildInventory(@Nonnull BaseVillager villager) {
        VillagerInventory inventory = villager.getSettlementsInventory();
        return VillagerInventorySnapshot.builder()
                .entries(inventory.entries())
                .build();
    }

    public VillagerTradeCatalogSnapshot buildTradeCatalog(@Nonnull BaseVillager villager) {
        var professionKey = resolveProfessionKey(villager);
        return new VillagerTradeCatalogSnapshot(this.tradeCatalogRegistry.offersFor(professionKey));
    }

    public VillagerDemandDisplaySnapshot buildDemandSnapshot(@Nonnull BaseVillager villager, long gameTime) {
        var professionKey = resolveProfessionKey(villager);
        List<ActiveDemand> activeDemands = this.demandEvaluator.resolve(villager);
        Map<ItemMatch, ActiveDemand> activeByMatch = activeDemands.stream()
                .collect(Collectors.toMap(ActiveDemand::match, demand -> demand, (left, right) -> left, LinkedHashMap::new));

        List<DemandDisplayEntry> entries = this.tradeCatalogRegistry.demandsFor(professionKey).stream()
                .map(demand -> {
                    ActiveDemand activeDemand = activeByMatch.get(demand.match());
                    return DemandDisplayEntry.builder()
                            .id(demand.id())
                            .match(demand.match())
                            .desiredMinCount(demand.desiredMinCount())
                            .basePricePerUnit(demand.basePricePerUnit())
                            .basePriority(demand.basePriority())
                            .activeDemand(activeDemand)
                            .build();
                })
                .toList();
        return new VillagerDemandDisplaySnapshot(entries);
    }

    private static double[] buildGeneValues(@Nonnull BaseVillager villager) {
        GeneticsProfile genetics = villager.getGenetics();
        GeneType[] types = GeneType.VALUES;
        double[] values = new double[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = genetics.getGeneValue(types[i]);
        }
        return values;
    }

    private static VillagerProfessionKey resolveProfessionKey(@Nonnull BaseVillager villager) {
        return VillagerProfessionKey.fromResourceLocation(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
        );
    }

    private static Optional<BlockPos> extractMemoryPos(@Nonnull BaseVillager villager,
                                                       @Nonnull MemoryModuleType<GlobalPos> memoryType) {
        return villager.getBrain()
                .getMemory(memoryType)
                .map(GlobalPos::pos);
    }

    private Optional<ActiveBehaviorInfo> findActiveBehavior(@Nonnull BaseVillager villager) {
        PlanRuntimeState runtimeState = villager.getPlanRuntimeState();
        IBehavior<BaseVillager> behavior = runtimeState.getCurrentBehavior();
        if (behavior == null || behavior.getStatus() != BehaviorStatus.RUNNING) {
            return Optional.empty();
        }

        BehaviorPlanningMetadata descriptor = runtimeState.getCurrentDescriptor();
        BehaviorDisplayMetadata displayInfo = descriptor == null
                ? null
                : this.behaviorCatalog.getDisplayInfo(descriptor.getKey()).orElse(null);

        return Optional.of(new ActiveBehaviorInfo(
                displayInfo != null ? displayInfo.displayNameKey() : "ui.settlements.behavior.behavior.unknown",
                displayInfo != null ? displayInfo.iconItemId().toString() : "minecraft:barrier"));
    }

    private static SchedulePhase mapSchedulePhase(@Nonnull Activity activity) {
        if (activity == Activity.REST) {
            return SchedulePhase.REST;
        }
        if (activity == Activity.WORK) {
            return SchedulePhase.WORK;
        }
        if (activity == Activity.MEET) {
            return SchedulePhase.MEET;
        }
        if (activity == Activity.IDLE || activity == Activity.PLAY) {
            return SchedulePhase.IDLE;
        }
        return SchedulePhase.UNKNOWN;
    }

    private record ActiveBehaviorInfo(@Nonnull String nameKey, @Nonnull String iconId) {
    }

}
