package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.location.Location;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;


public class JobSiteBlockExistsCondition<T extends Villager> implements IEntityCondition<T> {

    private final ICondition<PhysicalBlock> blockCondition;

    @Nullable
    private PhysicalBlock jobSiteBlock;

    public JobSiteBlockExistsCondition() {
        this(null);
    }

    public JobSiteBlockExistsCondition(@Nullable ICondition<PhysicalBlock> blockCondition) {
        this.blockCondition = Objects.requireNonNullElseGet(blockCondition, () -> (block) -> true);
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

        this.jobSiteBlock = blockOptional.get();
        return true;
    }

    public Optional<PhysicalBlock> getJobSiteBlock() {
        return Optional.ofNullable(jobSiteBlock);
    }

}
