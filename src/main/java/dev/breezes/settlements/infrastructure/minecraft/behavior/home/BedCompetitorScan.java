package dev.breezes.settlements.infrastructure.minecraft.behavior.home;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Settles a bed that nearby villagers (modded and vanilla) also remember as their home.
 * <p>
 * Of each pair of claimants, the one with the lower UUID keeps the bed; the other forgets it and wakes if asleep.
 */
@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedCompetitorScan {

    private static final ClockTicks SCAN_INTERVAL = ClockTicks.seconds(10);

    /**
     * Creates the scan for one brain. The instance holds that villager's scan cadence, so it must not be shared.
     */
    public static BehaviorControl<Villager> create() {
        Tickable cadence = Tickable.staggered(SCAN_INTERVAL);
        return BehaviorBuilder.create(instance -> instance.group(instance.present(MemoryModuleType.HOME), instance.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
                .apply(instance, (homeMemory, nearestLivingEntitiesMemory) -> (level, villager, gameTime) -> {
                    if (!cadence.tickCheckAndReset(1)) {
                        return false;
                    }

                    settleContests(villager, instance.get(homeMemory), instance.get(nearestLivingEntitiesMemory));
                    return true;
                }));
    }

    private static void settleContests(Villager self, GlobalPos home, List<LivingEntity> nearbyEntities) {
        // Each rival contests whoever still claims the bed, which stops being self once self loses
        Villager keeper = self;
        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Villager rival
                    && rival != self
                    && rival.isAlive()
                    && rival.getBrain().isMemoryValue(MemoryModuleType.HOME, home)) {
                keeper = settle(home, keeper, rival);
            }
        }
    }

    private static Villager settle(GlobalPos home, Villager first, Villager second) {
        boolean firstKeeps = first.getUUID().compareTo(second.getUUID()) < 0;
        Villager winner = firstKeeps ? first : second;
        Villager loser = firstKeeps ? second : first;

        // No release: the bed's ticket does not record its holder, so whatever ticket the bed has must stay to back the winner's claim
        loser.getBrain().eraseMemory(MemoryModuleType.HOME);

        // Wake the loser up explicitly
        if (loser.isSleeping()) {
            loser.stopSleeping();

            // Waking clears the bed's occupied flag even when the winner is still asleep; we re-set occupied
            winner.getSleepingPos().ifPresent(bed -> {
                BlockState state = winner.level().getBlockState(bed);
                if (state.isBed(winner.level(), bed, winner)) {
                    state.setBedOccupied(winner.level(), bed, winner, true);
                }
            });
        }

        log.debug("Bed {} in {}: villager {} keeps it, villager {} forgets it",
                home.pos().toShortString(), home.dimension().location(), winner.getUUID(), loser.getUUID());
        return winner;
    }

}
