package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ChestWithDemandedItemCondition implements IEntityCondition<BaseVillager> {

    private final DemandEvaluator demandEvaluator;
    private final int completionRange;

    @Nullable
    private Resolution resolution;

    public ChestWithDemandedItemCondition(@Nonnull DemandEvaluator demandEvaluator, int completionRange) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.demandEvaluator = demandEvaluator;
        this.completionRange = completionRange;
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.resolution = null;
        if (villager == null) {
            return false;
        }

        // Demands first — cheap short-circuit before touching world state.
        List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
        if (demands.isEmpty()) {
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

        // Demands-outer / chests-inner preserves demand priority while allowing any valid chest to satisfy it
        for (ActiveDemand demand : demands) {
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

                int slot = findMatchingSlot(handler, demand.match());
                if (slot < 0) {
                    continue;
                }

                ItemStack matched = handler.getStackInSlot(slot);
                this.resolution = new Resolution(chestPos, matched.copy(), demand);
                return true;
            }
        }

        return false;
    }

    public Optional<Resolution> getResolution() {
        return Optional.ofNullable(this.resolution);
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

    public record Resolution(
            @Nonnull GlobalPos chestPos,
            @Nonnull ItemStack matchedStack,
            @Nonnull ActiveDemand demand
    ) {
    }

}
