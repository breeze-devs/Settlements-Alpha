package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.domain.presentation.Socket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * The six knobs that place one socket: its local translation, in blocks, and its local rotation, in degrees.
 * {@link Socket} stores radians, and the conversion lives here.
 */
@ClientSide
public final class DebugTuningSocketKnobs {

    private static final float TRANSLATION_STEP_BLOCKS = 0.025F;
    private static final float ROTATION_STEP_DEGREES = 2.5F;

    private final DebugTuningKnob translationX;
    private final DebugTuningKnob translationY;
    private final DebugTuningKnob translationZ;
    private final DebugTuningKnob rotationX;
    private final DebugTuningKnob rotationY;
    private final DebugTuningKnob rotationZ;
    private final DebugTuningGroup group;

    private DebugTuningSocketKnobs(@Nonnull String groupName, @Nonnull Socket base) {
        Vec3 translation = base.getLocalTranslation();
        Vector3f rotation = base.getLocalRotation();

        this.translationX = DebugTuningKnob.of("pos.x", (float) translation.x, TRANSLATION_STEP_BLOCKS);
        this.translationY = DebugTuningKnob.of("pos.y", (float) translation.y, TRANSLATION_STEP_BLOCKS);
        this.translationZ = DebugTuningKnob.of("pos.z", (float) translation.z, TRANSLATION_STEP_BLOCKS);
        this.rotationX = DebugTuningKnob.of("rot.x deg", (float) Math.toDegrees(rotation.x()), ROTATION_STEP_DEGREES);
        this.rotationY = DebugTuningKnob.of("rot.y deg", (float) Math.toDegrees(rotation.y()), ROTATION_STEP_DEGREES);
        this.rotationZ = DebugTuningKnob.of("rot.z deg", (float) Math.toDegrees(rotation.z()), ROTATION_STEP_DEGREES);
        this.group = DebugTuningGroup.of(groupName, this.translationX, this.translationY, this.translationZ,
                this.rotationX, this.rotationY, this.rotationZ);
    }

    public static DebugTuningSocketKnobs forSocket(@Nonnull String groupName, @Nonnull Socket base) {
        return new DebugTuningSocketKnobs(groupName, base);
    }

    public DebugTuningGroup group() {
        return this.group;
    }

    public Socket applyTo(@Nonnull Socket base) {
        return this.rebuild(base, 1.0F);
    }

    /**
     * The same placement reflected across the villager's sagittal plane, for the opposite-side socket of a
     * mirrored pair.
     */
    public Socket applyMirroredTo(@Nonnull Socket base) {
        return this.rebuild(base, -1.0F);
    }

    private Socket rebuild(@Nonnull Socket base, float lateralSign) {
        return Socket.builder()
                .id(base.getId())
                .bone(base.getBone())
                .localTranslation(new Vec3(lateralSign * this.translationX.getValue(),
                        this.translationY.getValue(),
                        this.translationZ.getValue()))
                // Mirroring negates the two rotations that carry a side -- yaw and roll
                .localRotation(new Vector3f((float) Math.toRadians(this.rotationX.getValue()),
                        lateralSign * (float) Math.toRadians(this.rotationY.getValue()),
                        lateralSign * (float) Math.toRadians(this.rotationZ.getValue())))
                .localScale(base.getLocalScale())
                .inheritsBoneTransform(base.isInheritsBoneTransform())
                .build();
    }

}
