package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.BallistaBolt;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Lets a ballista's shot go: the bolt in flight, the release sound and the muzzle puff.
 */
@ServerSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BallistaLauncher {

    private static final float LAUNCH_INACCURACY = 0.0F;

    /**
     * Launches the ammunition from a seated bolt's tip along the machine's current aim.
     *
     * @param shooter the entity the shot is attributed to, or null when there is none
     */
    static void launch(@Nonnull ServerLevel level,
                       @Nonnull BlockPos machinePos,
                       @Nonnull BallistaAim aim,
                       @Nonnull ItemStack ammunition,
                       @Nullable Entity shooter,
                       boolean sparesVillagerAllies) {
        Vec3 floorCenter = new Vec3(machinePos.getX() + 0.5, machinePos.getY(), machinePos.getZ() + 0.5);
        Vec3 tip = floorCenter.add(BallistaLaunchPoint.tipOffset(aim));
        Vec3 direction = BallistaLaunchPoint.direction(aim);

        BallistaBolt bolt = new BallistaBolt(level, tip, ammunition, sparesVillagerAllies);
        if (shooter != null) {
            bolt.setOwner(shooter);
        }

        bolt.shoot(direction.x, direction.y, direction.z, BallistaBolt.LAUNCH_SPEED_BLOCKS_PER_TICK, LAUNCH_INACCURACY);
        level.addFreshEntity(bolt);

        BallistaSoundPalette.fire(Location.of(tip.x, tip.y, tip.z, level));
        level.sendParticles(ParticleTypes.POOF, tip.x, tip.y, tip.z, 6, 0.08, 0.08, 0.08, 0.02);
    }

}
