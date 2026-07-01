package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.supply.ActiveSupply;
import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detects when at least one reachable, non-waxed chest either holds a demanded item or has room for a dumpable surplus.
 * <p>
 * Besides the boolean gate, {@link #test} caches the sorted list of reachable, non-waxed chests that
 * currently have work (distance-ordered, dimension-filtered).
 */
public class ChestInteractionCondition implements IEntityCondition<BaseVillager> {

    private final DemandEvaluator demandEvaluator;
    private final SupplyEvaluator supplyEvaluator;
    private final int completionRange;

    @Getter
    private List<BlockPos> reachableChests;

    public ChestInteractionCondition(@Nonnull DemandEvaluator demandEvaluator,
                                     @Nonnull SupplyEvaluator supplyEvaluator,
                                     int completionRange) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.demandEvaluator = demandEvaluator;
        this.supplyEvaluator = supplyEvaluator;
        this.completionRange = completionRange;
        this.reachableChests = List.of();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.reachableChests = List.of();
        if (villager == null) {
            return false;
        }

        // Cheap short-circuit before touching world state: nothing to fetch and nothing to dump.
        List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
        List<ActiveSupply> supplies = this.supplyEvaluator.resolve(villager);
        if (demands.isEmpty() && supplies.isEmpty()) {
            return false;
        }

        Level level = villager.level();
        List<GlobalPos> chests = villager.getBrain()
                .getMemory(MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType())
                .orElse(List.of())
                .stream()
                .filter(chest -> chest.dimension().equals(level.dimension()))
                .sorted(Comparator.comparingDouble(chest -> chest.pos().distSqr(villager.blockPosition())))
                .toList();
        if (chests.isEmpty()) {
            return false;
        }

        List<BlockPos> reachable = new ArrayList<>();
        for (GlobalPos chestPos : chests) {
            BlockPos pos = chestPos.pos();
            if (ChestWaxService.isWaxed(level, pos)
                    || !villager.getNavigationManager().canReach(Location.of(pos, level), this.completionRange)) {
                continue;
            }

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (handler == null) {
                continue;
            }
            // Only retain chests that currently offer work
            if (!chestHasWork(handler, demands, supplies)) {
                continue;
            }

            reachable.add(pos);
        }

        this.reachableChests = reachable;
        return !reachable.isEmpty();
    }

    /**
     * Whether this chest currently holds a demanded item or has room for a dumpable surplus
     */
    public static boolean chestHasWork(@Nonnull IItemHandler handler,
                                       @Nonnull List<ActiveDemand> demands,
                                       @Nonnull List<ActiveSupply> supplies) {
        for (ActiveDemand demand : demands) {
            if (findMatchingSlot(handler, demand.match()) >= 0) {
                return true;
            }
        }

        for (ActiveSupply supply : supplies) {
            int targetCount = computeDepositTargetCount(supply);
            if (targetCount <= 0) {
                continue;
            }

            if (simulateAcceptedCount(handler, supply.representative().copyWithCount(targetCount)) > 0) {
                return true;
            }
        }

        return false;
    }

    private static int findMatchingSlot(@Nonnull IItemHandler handler, @Nonnull ItemMatch match) {
        int size = handler.getSlots();
        for (int i = 0; i < size; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (ItemMatches.test(match, stack)) {
                return i;
            }
        }
        return -1;
    }

    public static int computeDepositTargetCount(@Nonnull ActiveSupply supply) {
        return Math.min(supply.dumpableCount(), supply.representative().getMaxStackSize());
    }

    public static int simulateAcceptedCount(@Nonnull IItemHandler handler, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
        return stack.getCount() - remainder.getCount();
    }

}
