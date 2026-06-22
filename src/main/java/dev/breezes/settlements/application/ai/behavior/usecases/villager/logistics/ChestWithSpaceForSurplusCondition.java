package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.economy.supply.ActiveSupply;
import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ChestWithSpaceForSurplusCondition implements IEntityCondition<BaseVillager> {

    private final SupplyEvaluator supplyEvaluator;
    private final int completionRange;

    @Nullable
    private Resolution resolution;

    public ChestWithSpaceForSurplusCondition(@Nonnull SupplyEvaluator supplyEvaluator, int completionRange) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.supplyEvaluator = supplyEvaluator;
        this.completionRange = completionRange;
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.resolution = null;
        if (villager == null) {
            return false;
        }

        List<ActiveSupply> supplies = this.supplyEvaluator.resolve(villager);
        if (supplies.isEmpty()) {
            return false;
        }

        List<GlobalPos> chests = villager.getBrain()
                .getMemory(MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType())
                .orElse(List.of())
                .stream()
                .filter(chest -> chest.dimension().equals(villager.level().dimension()))
                .sorted(Comparator.comparingDouble(chest -> chest.pos().distSqr(villager.blockPosition())))
                .toList();
        if (chests.isEmpty()) {
            return false;
        }

        Level level = villager.level();

        // Supplies-outer preserves catalog priority so the most important overflow rule wins.
        for (ActiveSupply supply : supplies) {
            int targetCount = computeDepositTargetCount(supply);
            if (targetCount <= 0) {
                continue;
            }

            ItemStack candidate = supply.representative().copyWithCount(targetCount);
            for (GlobalPos chestPos : chests) {
                if (ChestWaxService.isWaxed(level, chestPos.pos())) {
                    continue;
                }

                if (!villager.getNavigationManager().canReach(Location.of(chestPos.pos(), level), this.completionRange)) {
                    continue;
                }

                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos.pos(), null);
                if (handler == null) {
                    continue;
                }

                int acceptedCount = simulateAcceptedCount(handler, candidate);
                if (acceptedCount <= 0) {
                    continue;
                }

                this.resolution = new Resolution(chestPos, supply, acceptedCount);
                return true;
            }
        }

        return false;
    }

    public Optional<Resolution> getResolution() {
        return Optional.ofNullable(this.resolution);
    }

    static int computeDepositTargetCount(@Nonnull ActiveSupply supply) {
        return Math.min(supply.dumpableCount(), supply.representative().getMaxStackSize());
    }

    static int simulateAcceptedCount(@Nonnull IItemHandler handler, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
        return stack.getCount() - remainder.getCount();
    }

    public record Resolution(
            @Nonnull GlobalPos chestPos,
            @Nonnull ActiveSupply supply,
            int acceptedCount
    ) {
    }

}
