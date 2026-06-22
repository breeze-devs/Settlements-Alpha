package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;


public class JobSiteBlockExistsCondition<T extends Villager> implements IEntityCondition<T> {

    private final ICondition<PhysicalBlock> blockCondition;
    private final int completionRange;

    @Nullable
    private PhysicalBlock jobSiteBlock;

    public JobSiteBlockExistsCondition(@Nullable ICondition<PhysicalBlock> blockCondition, int completionRange) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.blockCondition = Objects.requireNonNullElseGet(blockCondition, () -> (block) -> true);
        this.completionRange = completionRange;
    }

    @Override
    public boolean test(@Nullable T villager) {
        // Reset variable before determining if the condition is met
        this.jobSiteBlock = null;

        Optional<Location> locationOptional = Optional.ofNullable(villager)
                .map(Villager::getBrain)
                .flatMap(brain -> brain.getMemory(MemoryModuleType.JOB_SITE))
                .map(globalPos -> Location.of(globalPos.pos(), villager.level()));
        if (locationOptional.isEmpty()) {
            return false;
        }

        Location location = locationOptional.get();
        Optional<PhysicalBlock> blockOptional = location.getBlock()
                .filter(blockCondition);
        if (blockOptional.isEmpty()) {
            return false;
        }

        if (villager instanceof ISettlementsBrainEntity brainEntity
                && !brainEntity.getNavigationManager().canReach(location, this.completionRange)) {
            return false;
        }

        this.jobSiteBlock = blockOptional.get();
        return true;
    }

    public Optional<PhysicalBlock> getJobSiteBlock() {
        return Optional.ofNullable(jobSiteBlock);
    }

}
