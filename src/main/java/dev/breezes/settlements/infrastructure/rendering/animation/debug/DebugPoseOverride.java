package dev.breezes.settlements.infrastructure.rendering.animation.debug;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationTarget;
import dev.breezes.settlements.domain.animation.AnimationTargets;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Live, per-process slider values that splice into the sampled animation frame before render.
 */
@ClientSide
@ClientScope
@Getter
@Setter
public final class DebugPoseOverride {

    private volatile boolean enabled;

    private volatile double armsPitchDegrees;
    private volatile double armsYawDegrees;
    private volatile double armsRollDegrees;

    private volatile double headPitchDegrees;
    private volatile double headYawDegrees;
    private volatile double headRollDegrees;

    private volatile double bodyPitchDegrees;
    private volatile double bodyYawDegrees;
    private volatile double bodyRollDegrees;

    private volatile double armsTranslationX;
    private volatile double armsTranslationY;
    private volatile double armsTranslationZ;

    @Inject
    public DebugPoseOverride() {
    }

    public void reset() {
        this.armsPitchDegrees = 0.0D;
        this.armsYawDegrees = 0.0D;
        this.armsRollDegrees = 0.0D;
        this.headPitchDegrees = 0.0D;
        this.headYawDegrees = 0.0D;
        this.headRollDegrees = 0.0D;
        this.bodyPitchDegrees = 0.0D;
        this.bodyYawDegrees = 0.0D;
        this.bodyRollDegrees = 0.0D;
        this.armsTranslationX = 0.0D;
        this.armsTranslationY = 0.0D;
        this.armsTranslationZ = 0.0D;
    }

    public AnimationFrame applyTo(@Nonnull AnimationFrame frame) {
        if (!this.enabled) {
            return frame;
        }

        Map<AnimationTarget<?>, Object> overrides = new HashMap<>(4);
        overrides.put(AnimationTargets.ARMS_CROSSED_ROTATION,
                RotationUtil.degrees((float) this.armsPitchDegrees, (float) this.armsYawDegrees, (float) this.armsRollDegrees));
        overrides.put(AnimationTargets.HEAD_ROTATION_OVERRIDE,
                RotationUtil.degrees((float) this.headPitchDegrees, (float) this.headYawDegrees, (float) this.headRollDegrees));
        overrides.put(AnimationTargets.BODY_ROTATION,
                RotationUtil.degrees((float) this.bodyPitchDegrees, (float) this.bodyYawDegrees, (float) this.bodyRollDegrees));
        overrides.put(AnimationTargets.ARMS_CROSSED_TRANSLATION,
                new Vec3(this.armsTranslationX, this.armsTranslationY, this.armsTranslationZ));
        return frame.overlay(overrides);
    }

}
