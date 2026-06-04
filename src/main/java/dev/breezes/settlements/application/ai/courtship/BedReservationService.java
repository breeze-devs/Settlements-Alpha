package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@ServerScope
@AllArgsConstructor(onConstructor_ = @Inject)
public final class BedReservationService {

    private static final int BED_SCAN_RANGE = 48;

    /**
     * Attempts to claim a vacant HOME POI bed reachable by the given villager.
     * Mirrors vanilla VillagerMakeLove.takeVacantBed: path-reachability is validated
     * before the POI ticket is acquired to avoid claiming unreachable beds.
     */
    public Optional<BlockPos> tryClaimVacantHome(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        return level.getPoiManager().take(
                holder -> holder.is(PoiTypes.HOME),
                (holder, pos) -> this.isReachable(villager, pos, holder.value().validRange()),
                villager.blockPosition(),
                BED_SCAN_RANGE
        );
    }

    /**
     * Writes the given bed position into the child villager's HOME memory.
     */
    public void assignHome(@Nonnull BaseVillager child, @Nonnull ServerLevel level, @Nonnull BlockPos bed) {
        child.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bed));
    }

    private boolean isReachable(@Nonnull BaseVillager villager, @Nonnull BlockPos pos, int validRange) {
        Path path = villager.getNavigation().createPath(pos, validRange);
        return path != null && path.canReach();
    }

}
