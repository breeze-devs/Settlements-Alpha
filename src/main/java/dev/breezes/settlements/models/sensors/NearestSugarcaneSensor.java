package dev.breezes.settlements.models.sensors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.memory.MemoryTypeRegistry;
import dev.breezes.settlements.models.memory.entry.MemoryEntry;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.sensors.result.EmptySenseResultEntry;
import dev.breezes.settlements.models.sensors.result.ISenseResult;
import dev.breezes.settlements.models.sensors.result.ISenseResultEntry;
import dev.breezes.settlements.models.sensors.result.SenseResultEntry;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

// TODO: it's better to combine different block types into 1 sensor for efficiency, since it's one loop vs many
// TODO: although we might need different horizontal/vertical ranges for each block type
// TODO: as well as different memory keys, expiration times, and even not-found behavior (e.g. keep vs clear memory)
@Getter
public class NearestSugarcaneSensor<T extends BaseVillager> extends AbstractSensor<T> implements INearbySensor<T> {

    private final int horizontalRange;
    private final int verticalRange;

    public NearestSugarcaneSensor(List<IEntityCondition<T>> preconditions, ITickable senseCooldown, T entity, int horizontalRange, int verticalRange) {
        super(preconditions, senseCooldown, entity);
        this.horizontalRange = horizontalRange;
        this.verticalRange = verticalRange;
    }

    @Override
    public ISenseResult doSense(@Nonnull Level world, @Nonnull T entity) {
        BlockPos center = entity.blockPosition();
        Optional<GlobalPos> nearestSugarcaneGlobalPosOptional = BlockPos.findClosestMatch(center, this.horizontalRange, this.verticalRange, (pos) -> {
            // Check block state
            if (!isHarvestableSugarcane(world, pos)) {
                return false;
            }

            // TODO: check reachability (if the target is blocked off)
            // Path path = entity.getNavigation().createPath(pos, 2);
            // return path != null;
            return true;
        }).map((pos) -> GlobalPos.of(world.dimension(), pos));

        return nearestSugarcaneGlobalPosOptional.map(NearestSugarcaneSenseResult::new)
                .orElseGet(NearestSugarcaneSenseResult::new);
    }

    /**
     * Checks if the block state is a harvestable sugarcane block
     * - note that it will only return true when the sugarcane is the second block from the ground up
     * - e.g. if the block is [sand, sugarcane, *sugarcane*, sugarcane] only the "starred" sugarcane will return true
     */
    private boolean isHarvestableSugarcane(@Nonnull Level world, @Nonnull BlockPos pos) {
        // Check if current block is sugarcane
        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.SUGAR_CANE)) {
            return false;
        }

        // Check if the block below is also sugarcane
        BlockState below = world.getBlockState(pos.below());
        if (!below.is(Blocks.SUGAR_CANE)) {
            return false;
        }

        // Check if 2 blocks below is not sugarcane (otherwise we would want to harvest at least 1 block below)
        BlockState below2 = world.getBlockState(pos.below(2));
        return !below2.is(Blocks.SUGAR_CANE);
    }

    @Getter
    public static class NearestSugarcaneSenseResult implements ISenseResult {

        private final List<ISenseResultEntry<?>> senseResults;

        public NearestSugarcaneSenseResult() {
            this.senseResults = List.of(EmptySenseResultEntry.<GlobalPos>builder()
                    .memoryType(MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE)
                    .build());
        }

        public NearestSugarcaneSenseResult(@Nonnull GlobalPos nearestSugarcaneGlobalPos) {
            MemoryEntry<GlobalPos> nearestSugarcaneMemoryEntry = MemoryEntry.<GlobalPos>builder()
                    .memoryType(MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE)
                    .memoryValue(nearestSugarcaneGlobalPos)
                    .build();

            this.senseResults = List.of(SenseResultEntry.<GlobalPos>builder()
                    .memoryType(MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE)
                    .memoryEntry(nearestSugarcaneMemoryEntry)
                    .build());
        }

    }

}
