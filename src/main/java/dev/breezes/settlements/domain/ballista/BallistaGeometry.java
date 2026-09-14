package dev.breezes.settlements.domain.ballista;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;

/**
 * The ballista rig's measurements that more than its drawing depends on, in the entity-model space its layer definition
 * is authored in: pixels, y pointing down, and, in the turret's frame, the muzzle along -z and the machine's left along
 * +x.
 * <p>
 * Every offset is a bone's place in its parent's frame. None of the bones from the turret down to the bolt socket
 * carries a rest rotation, so their offsets sum to the socket's place in the turret's frame.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaGeometry {

    public static final float PIXELS_PER_BLOCK = 16.0F;

    /**
     * How far down model-space y the entity model format stands the floor of the block the model is drawn in.
     */
    public static final float GROUND_DEPTH_PIXELS = 24.0F;

    /**
     * The aim bone, whose pivot is the point aim turns the machine about.
     */
    public static final Vec3 TURRET_OFFSET = new Vec3(0.0, 21.0, 0.0);
    public static final Vec3 GIMBAL_OFFSET = Vec3.ZERO;
    public static final Vec3 CRADLE_OFFSET = new Vec3(0.0, -6.0, 0.0);
    public static final Vec3 PUSHER_OFFSET = new Vec3(0.0, -1.5, -9.0);

    /**
     * Where a seated bolt's pivot sits.
     */
    public static final Vec3 BOLT_SOCKET_OFFSET = new Vec3(0.0, 0.5, -0.5);

}
