package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.blocks.LiveBlockSiteMatcher;
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
    private final LiveBlockSiteMatcher matcher;
    private final BlockScanBox confirmBox;
    private final int maxSitesToConfirm;
    private final int completionRange;
    private final String description;

    @Builder
    public KnownBlockSitesPrecondition(@Nonnull MemoryType<List<GlobalPos>> memoryType,
                                       @Nonnull LiveBlockSiteMatcher matcher,
                                       @Nonnull BlockScanBox confirmBox,
                                       int maxSitesToConfirm,
                                       int completionRange,
                                       @Nonnull String description) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.memoryType = memoryType;
        this.matcher = matcher;
        this.confirmBox = confirmBox;
        this.maxSitesToConfirm = maxSitesToConfirm;
        this.completionRange = completionRange;
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
                this.matcher, this.confirmBox, this.maxSitesToConfirm,
                villager.getNavigationManager()::canReach, this.completionRange).isPresent();
    }

    @Override
    public String description() {
        return this.description;
    }

}
