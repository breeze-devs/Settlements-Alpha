package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChestWithDemandedItemCondition implements IEntityCondition<BaseVillager> {

    private final DemandEvaluator demandEvaluator;

    @Nullable
    private Resolution resolution;

    public ChestWithDemandedItemCondition(@Nonnull DemandEvaluator demandEvaluator) {
        this.demandEvaluator = demandEvaluator;
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
                .orElse(List.of());
        if (chests.isEmpty()) {
            return false;
        }

        Level level = villager.level();
        ResourceKey<Level> dimension = level.dimension();
        Vec3 villagerPos = villager.position();

        // Walk chests closest-first so the villager targets the nearest qualifying chest.
        List<GlobalPos> chestsByDistance = chests.stream()
                .filter(gp -> gp.dimension().equals(dimension))
                .sorted(Comparator.comparingDouble(gp -> villagerPos.distanceToSqr(
                        gp.pos().getX() + 0.5, gp.pos().getY() + 0.5, gp.pos().getZ() + 0.5)))
                .toList();

        // Resolve handlers once per chest; the inverted loop below would otherwise re-fetch
        // the capability for each (demand, chest) pair. Missing chests (removed since the
        // last sensor scan) are skipped — the memory self-corrects on the next scan.
        // LinkedHashMap preserves the nearest-first iteration order of chestsByDistance.
        // Waxed chests are also skipped here as an immediacy guard: the sensor keeps memory clean,
        // but a chest waxed in the same tick would not yet be evicted from VILLAGE_CHESTS.
        Map<GlobalPos, IItemHandler> handlerByChest = new LinkedHashMap<>();
        for (GlobalPos chestPos : chestsByDistance) {
            if (ChestWaxService.isWaxed(level, chestPos.pos())) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos.pos(), null);
            if (handler != null) {
                handlerByChest.put(chestPos, handler);
            }
        }

        // Demands-outer / chests-inner so the highest-priority demand wins over a closer chest
        // that only satisfies a lower-priority demand. Within a single demand, the inner loop
        // still walks chests nearest-first.
        for (ActiveDemand demand : demands) {
            for (Map.Entry<GlobalPos, IItemHandler> entry : handlerByChest.entrySet()) {
                int slot = findMatchingSlot(entry.getValue(), demand.match());
                if (slot < 0) {
                    continue;
                }
                ItemStack matched = entry.getValue().getStackInSlot(slot);
                this.resolution = new Resolution(entry.getKey(), slot, matched.copy(), demand);
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
            int slot,
            @Nonnull ItemStack matchedStack,
            @Nonnull ActiveDemand demand
    ) {
    }

}
