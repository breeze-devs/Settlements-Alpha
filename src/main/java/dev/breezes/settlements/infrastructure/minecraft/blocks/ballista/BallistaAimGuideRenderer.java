package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A dashed line from a ballista's muzzle along the barrel's current aim.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BallistaAimGuideRenderer implements ClientSessionResettable {

    @VisibleForTesting
    static final long SHOWN_FOR_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(5));

    /**
     * The end of the showing over which the guide fades away.
     */
    @VisibleForTesting
    static final long FADE_OUT_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(1));

    /**
     * How long the aim line is.
     */
    private static final double LENGTH_BLOCKS = 6.0;
    private static final double DASH_BLOCKS = 0.3;
    private static final double GAP_BLOCKS = 0.2;

    /**
     * The far end of the line that fades out along its length.
     */
    private static final double FAR_FADE_SHARE = 0.4;
    private static final float FULL_ALPHA = 0.8F;

    @Nullable
    private Level level;
    @Nullable
    private BlockPos pos;
    private long adjustedAtMillis;

    @Override
    public void onClientSessionEnded() {
        this.clearTarget();
    }

    private void clearTarget() {
        this.level = null;
        this.pos = null;
    }

    /**
     * Shows the guide on the ballista at pos, starting its showing over.
     */
    void adjusted(@Nonnull Level level, @Nonnull BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
        this.adjustedAtMillis = ClientMonotonicClock.nowMillis();
    }

    public void render(@Nonnull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || this.pos == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // Dimension travel can put an unrelated ballista at the same position
        if (minecraft.level == null || minecraft.level != this.level) {
            this.clearTarget();
            return;
        }

        float alpha = FULL_ALPHA * computeTimeOpacity(ClientMonotonicClock.nowMillis() - this.adjustedAtMillis);
        if (alpha <= 0.0F || !(minecraft.level.getBlockEntity(this.pos) instanceof BallistaBlockEntity ballista)) {
            this.clearTarget();
            return;
        }

        BallistaAim aim = ballista.interpolatedAim(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        Vec3 tip = Vec3.atBottomCenterOf(this.pos).add(BallistaLaunchPoint.tipOffset(aim));
        Vec3 direction = BallistaLaunchPoint.direction(aim);

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        // Subtract in double precision before the pose narrows to floats, preserving small dashes far from origin
        poseStack.translate(tip.x - cameraPos.x, tip.y - cameraPos.y, tip.z - cameraPos.z);
        PoseStack.Pose pose = poseStack.last();
        for (double dashStart = 0.0; dashStart < LENGTH_BLOCKS; dashStart += DASH_BLOCKS + GAP_BLOCKS) {
            double dashEnd = Math.min(dashStart + DASH_BLOCKS, LENGTH_BLOCKS);
            emitVertex(lines, pose, direction.scale(dashStart), direction, alpha * computeDistanceOpacity(dashStart));
            emitVertex(lines, pose, direction.scale(dashEnd), direction, alpha * computeDistanceOpacity(dashEnd));
        }
        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    /**
     * The guide's opacity. The guide slowly fades out over time.
     */
    @VisibleForTesting
    static float computeTimeOpacity(long millisSinceAdjusted) {
        long remaining = SHOWN_FOR_MILLIS - millisSinceAdjusted;
        if (remaining <= 0L) {
            return 0.0F;
        }

        return Math.min(1.0F, (float) remaining / FADE_OUT_MILLIS);
    }

    private static float computeDistanceOpacity(double distanceBlocks) {
        double fadeStart = LENGTH_BLOCKS * (1.0 - FAR_FADE_SHARE);
        if (distanceBlocks <= fadeStart) {
            return 1.0F;
        }

        return (float) ((LENGTH_BLOCKS - distanceBlocks) / (LENGTH_BLOCKS - fadeStart));
    }

    /**
     * A line vertex's normal is the line's own direction, which the line shader widens it across.
     */
    private static void emitVertex(@Nonnull VertexConsumer lines, @Nonnull PoseStack.Pose pose,
                                   @Nonnull Vec3 point, @Nonnull Vec3 direction, float alpha) {
        lines.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

}
