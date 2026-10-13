package dev.breezes.settlements.infrastructure.minecraft.behavior.threat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Flees from the threat once it comes close towards the nearest village.
 * <p>
 * This is vanilla's SetWalkTargetAwayFrom without a walk target, with its destinations ranked by distance to the
 * village. Fleeing straight away from a slower chaser lets the chaser herd the mob out of the village.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FleeTowardVillage {

    // Mirrors vanilla SetWalkTargetAwayFrom's destination search
    private static final int MAX_HORIZONTAL_DISTANCE = 16;
    private static final int MAX_VERTICAL_DISTANCE = 7;
    private static final int SEARCH_ATTEMPTS = 10;

    // Only run away or sideways from threat, banning the ~90 degrees towards it
    private static final double BANNED_TOWARD_THREAT_ANGLE = Math.PI / 4;
    private static final double AWAY_MAX_ANGLE = Math.PI - BANNED_TOWARD_THREAT_ANGLE / 2;

    public static <T extends Entity> BehaviorControl<PathfinderMob> create(@Nonnull MemoryModuleType<T> threatMemory,
                                                                           float speedModifier,
                                                                           int fleeDistance) {
        return BehaviorBuilder.create(instance -> instance.group(instance.absent(MemoryModuleType.WALK_TARGET), instance.present(threatMemory))
                .apply(instance, (walkTarget, threat) -> (level, mob, gameTime) -> {
                    Vec3 threatPosition = instance.get(threat).position();
                    if (!mob.position().closerThan(threatPosition, fleeDistance)) {
                        return false;
                    }

                    // Each search can come up empty against walls and drops, so it retries as vanilla does
                    for (int i = 0; i < SEARCH_ATTEMPTS; i++) {
                        Vec3 destination = findDestination(level, mob, threatPosition);
                        if (destination != null) {
                            walkTarget.set(new WalkTarget(destination, speedModifier, 0));
                            break;
                        }
                    }

                    return true;
                }));
    }

    @Nullable
    private static Vec3 findDestination(@Nonnull ServerLevel level, @Nonnull PathfinderMob mob, @Nonnull Vec3 threatPosition) {
        Vec3 away = mob.position().subtract(threatPosition);
        boolean restricted = GoalUtils.mobRestricted(mob, MAX_HORIZONTAL_DISTANCE);

        // Rebuilt from vanilla LandRandomPos.getPosAway, which takes no ranking. Ties keep the earliest candidate, so
        // where every candidate is equally near the village this flees as vanilla does.
        return RandomPos.generateRandomPos(() -> {
            BlockPos offset = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), MAX_HORIZONTAL_DISTANCE,
                    MAX_VERTICAL_DISTANCE, 0, away.x, away.z, AWAY_MAX_ANGLE);
            if (offset == null) {
                return null;
            }

            BlockPos candidate = LandRandomPos.generateRandomPosTowardDirection(mob, MAX_HORIZONTAL_DISTANCE, restricted, offset);
            return candidate == null ? null : LandRandomPos.movePosUpOutOfSolid(mob, candidate);
        }, position -> -level.sectionsToVillage(SectionPos.of(position)));
    }

}
