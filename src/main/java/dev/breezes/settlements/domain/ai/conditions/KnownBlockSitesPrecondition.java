package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public final class KnownBlockSitesPrecondition implements IEntityCondition<BaseVillager> {

    private final MemoryType<List<GlobalPos>> memoryType;
    private final BlockMatcher matcher;
    private final BlockScanBox confirmBox;
    private final int maxSitesToConfirm;
    private final String description;

    @Builder
    public KnownBlockSitesPrecondition(@Nonnull MemoryType<List<GlobalPos>> memoryType,
                                       @Nonnull BlockMatcher matcher,
                                       @Nonnull BlockScanBox confirmBox,
                                       int maxSitesToConfirm,
                                       @Nonnull String description) {
        this.memoryType = memoryType;
        this.matcher = matcher;
        this.confirmBox = confirmBox;
        this.maxSitesToConfirm = maxSitesToConfirm;
        this.description = description;
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        if (villager == null) {
            return false;
        }

        Optional<List<GlobalPos>> memory = villager.getSettlementsBrain().getMemory(this.memoryType);
        if (memory.isEmpty() || memory.get().isEmpty()) {
            return false;
        }

        Level level = villager.level();
        return BlockMemorySiteConfirmer.confirmNearest(memory.get(), villager.blockPosition(), level,
                this.matcher, this.confirmBox, this.maxSitesToConfirm).isPresent();
    }

    @Override
    public String description() {
        return this.description;
    }

}
