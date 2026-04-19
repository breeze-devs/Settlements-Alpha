package dev.breezes.settlements.application.ui.stats.snapshot;

import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorBinding;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorRuntimeInformation;
import dev.breezes.settlements.application.ui.behavior.snapshot.IBehaviorInfoProvider;
import dev.breezes.settlements.application.ui.stats.model.DemandDisplayEntry;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ServerSide
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerStatsSnapshotBuilder {

    private final VillagerWallet villagerWallet;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DemandEvaluator demandEvaluator;

    @Nonnull
    public VillagerStatsSnapshot buildStats(@Nonnull BaseVillager villager, long gameTime) {
        String villagerName = villager.hasCustomName() ? villager.getName().getString() : null;
        String professionKey = villager.getVillagerData().getProfession().toString();
        int expertiseLevel = villager.getVillagerData().getLevel();

        double[] geneValues = buildGeneValues(villager);

        BlockPos homePos = extractMemoryPos(villager, MemoryModuleType.HOME);
        BlockPos workstationPos = extractMemoryPos(villager, MemoryModuleType.JOB_SITE);

        Activity activity = villager.getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE);
        SchedulePhase schedulePhase = mapSchedulePhase(activity);

        ActiveBehaviorInfo activeBehavior = findActiveBehavior(villager);

        return VillagerStatsSnapshot.builder()
                .gameTime(gameTime)
                .villagerEntityId(villager.getId())
                .villagerName(villagerName)
                .professionKey(professionKey)
                .expertiseLevel(expertiseLevel)
                .currentHealth(villager.getHealth())
                .maxHealth(villager.getMaxHealth())
                .geneValues(geneValues)
                .homePos(homePos)
                .workstationPos(workstationPos)
                .activeBehaviorNameKey(activeBehavior != null ? activeBehavior.nameKey : null)
                .activeBehaviorStage(activeBehavior != null ? activeBehavior.stageLabel : null)
                .activeBehaviorIconId(activeBehavior != null ? activeBehavior.iconId : null)
                .schedulePhase(schedulePhase)
                .reputation(0) // TODO: wire to actual villager reputation data
                .hunger(villager.getHunger())
                .walletBalance(villagerWallet.getBalance(villager))
                .build();
    }

    @Nonnull
    public VillagerInventorySnapshot buildInventory(@Nonnull BaseVillager villager) {
        VillagerInventory inventory = villager.getSettlementsInventory();
        SimpleContainer backpack = inventory.getBackpack();

        List<ItemStack> nonEmptyItems = new ArrayList<>();
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            ItemStack stack = backpack.getItem(i);
            if (!stack.isEmpty()) {
                nonEmptyItems.add(stack.copy());
            }
        }

        return VillagerInventorySnapshot.builder()
                .backpackSize(inventory.getBackpackSize())
                .nonEmptyItems(nonEmptyItems)
                .build();
    }

    @Nonnull
    public VillagerTradeCatalogSnapshot buildTradeCatalog(@Nonnull BaseVillager villager) {
        var professionKey = resolveProfessionKey(villager);
        return new VillagerTradeCatalogSnapshot(this.tradeCatalogRegistry.offersFor(professionKey));
    }

    @Nonnull
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

    @Nonnull
    private static VillagerProfessionKey resolveProfessionKey(@Nonnull BaseVillager villager) {
        return VillagerProfessionKey.fromResourceLocation(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
        );
    }

    @Nullable
    private static BlockPos extractMemoryPos(@Nonnull BaseVillager villager,
                                             @Nonnull MemoryModuleType<GlobalPos> memoryType) {
        return villager.getBrain()
                .getMemory(memoryType)
                .map(GlobalPos::pos)
                .orElse(null);
    }

    @Nullable
    private static ActiveBehaviorInfo findActiveBehavior(@Nonnull BaseVillager villager) {
        for (BehaviorBinding binding : villager.getTrackedCustomBehaviors()) {
            IBehavior<BaseVillager> behavior = binding.behavior();
            if (behavior.getStatus() != BehaviorStatus.RUNNING) {
                continue;
            }
            if (!(behavior instanceof IBehaviorInfoProvider infoProvider)) {
                continue;
            }

            BehaviorDescriptor descriptor = infoProvider.getBehaviorDescriptor();
            BehaviorRuntimeInformation runtime = infoProvider.getBehaviorRuntimeInformation(villager);

            return new ActiveBehaviorInfo(
                    descriptor.displayNameKey(),
                    runtime.currentStageLabel(),
                    descriptor.iconItemId().toString()
            );
        }
        return null;
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

    private record ActiveBehaviorInfo(@Nonnull String nameKey, @Nullable String stageLabel, @Nonnull String iconId) {
    }

}
