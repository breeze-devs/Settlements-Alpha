package dev.breezes.settlements.application.ai.targeting;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.blocks.LiveBlockSiteMatcher;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BlockMemoryTargetResolver {

    public boolean resolveBlockTarget(@Nonnull BehaviorContext<BaseVillager> context,
                                      @Nonnull MemoryType<List<GlobalPos>> memoryType,
                                      @Nonnull LiveBlockSiteMatcher matcher,
                                      @Nonnull BlockScanBox confirmBox,
                                      int maxSitesToConfirm,
                                      int completionRange) {
        BaseVillager villager = context.getInitiator();
        Level level = villager.level();
        Optional<List<GlobalPos>> memory = villager.getSettlementsBrain().getMemory(memoryType);
        if (memory.isEmpty() || memory.get().isEmpty()) {
            context.clearState(BehaviorStateType.TARGET);
            return false;
        }

        Set<GlobalPos> visitedSites = context.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                .map(VisitedBlockSitesState::getSites)
                .orElse(Set.of());
        List<GlobalPos> candidates = memory.get().stream()
                .filter(site -> !visitedSites.contains(site))
                .toList();

        Optional<GlobalPos> liveSite = BlockMemorySiteConfirmer.confirmNearest(candidates, villager.blockPosition(), level,
                matcher, confirmBox, maxSitesToConfirm, villager.getNavigationManager()::canReach, completionRange);
        if (liveSite.isEmpty()) {
            context.clearState(BehaviorStateType.TARGET);
            return false;
        }

        GlobalPos picked = liveSite.get();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                PhysicalBlock.of(Location.of(picked.pos(), level), level.getBlockState(picked.pos())))));
        return true;
    }

}
