package dev.breezes.settlements.domain.ballista;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Where a ballista's shot leaves the machine and which way it goes.
 * <p>
 * Positions are offsets in blocks, on world axes, from the center of the floor of the machine's block.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaLaunchPoint {

    private static final Vec3 SOCKET_IN_TURRET = BallistaGeometry.GIMBAL_OFFSET
            .add(BallistaGeometry.CRADLE_OFFSET)
            .add(BallistaGeometry.PUSHER_OFFSET)
            .add(BallistaGeometry.BOLT_SOCKET_OFFSET);

    // The rig is authored with y down, its muzzle along -z and its left along +x. At rest nothing below the turret
    // moves against it, so the seated tip is fixed in the barrel's own frame
    private static final double TIP_FORWARD_BLOCKS =
            (BallistaBoltGeometry.TIP_REACH_PIXELS - SOCKET_IN_TURRET.z) / BallistaGeometry.PIXELS_PER_BLOCK;
    private static final double TIP_UP_BLOCKS = -SOCKET_IN_TURRET.y / BallistaGeometry.PIXELS_PER_BLOCK;
    private static final double TIP_RIGHT_BLOCKS = -SOCKET_IN_TURRET.x / BallistaGeometry.PIXELS_PER_BLOCK;

    // The rig's root frame is drawn with x and y negated and never turns
    private static final Vec3 TURRET_PIVOT = new Vec3(
            -BallistaGeometry.TURRET_OFFSET.x,
            BallistaGeometry.GROUND_DEPTH_PIXELS - BallistaGeometry.TURRET_OFFSET.y,
            BallistaGeometry.TURRET_OFFSET.z)
            .scale(1.0 / BallistaGeometry.PIXELS_PER_BLOCK);

    /**
     * The tip of a bolt seated in the machine, turned to the aim's current angles with the pusher at rest.
     */
    public static Vec3 tipOffset(@Nonnull BallistaAim aim) {
        double yaw = Math.toRadians(aim.getYaw());
        double pitch = Math.toRadians(aim.getPitch());

        Vec3 barrelForward = direction(aim);
        // Raising the muzzle tips the barrel's up direction back toward the rear
        Vec3 barrelUp = new Vec3(Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), -Math.cos(yaw) * Math.sin(pitch));
        Vec3 barrelRight = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));

        return TURRET_PIVOT
                .add(barrelForward.scale(TIP_FORWARD_BLOCKS))
                .add(barrelUp.scale(TIP_UP_BLOCKS))
                .add(barrelRight.scale(TIP_RIGHT_BLOCKS));
    }

    /**
     * The unit vector a shot leaves along, for the aim's current angles.
     */
    public static Vec3 direction(@Nonnull BallistaAim aim) {
        double yaw = Math.toRadians(aim.getYaw());
        double pitch = Math.toRadians(aim.getPitch());
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal);
    }

}
