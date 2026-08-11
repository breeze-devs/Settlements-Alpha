package dev.breezes.settlements.infrastructure.rendering.zone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * Draws a translucent wall standing on a flat rectangle: solid along the rectangle's own plane, hazing out
 * symmetrically above and below it, and pulsing gently so a boundary reads as alive rather than painted on.
 * <p>
 * Knows a box, a color, and an opacity.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZoneWallRenderer {

    /**
     * How far the wall reaches up/down from the boundary line — the whole wall is twice this tall.
     */
    public static final double PLOT_REACH_BLOCKS = 0.55D;

    /**
     * How far inside the rectangle the wall's plane stands. Prevents Z-fighting.
     */
    private static final double EDGE_INSET_BLOCKS = 0.02D;

    /**
     * How sharply the wall thins with distance.
     * <p>
     * Above 1 the fade starts slow and then accelerates, which reads as a solid boundary line with haze
     * standing on it; at 1 it is an even ramp, and the line stops reading as a line at all.
     */
    private static final double FADE_EXPONENT = 3.0D;

    /**
     * Horizontal slices per half. Alpha interpolates linearly between a quad's own edges, so the curve
     * {@link #wallAlpha} describes is only as curved as the number of edges sampling it — a single quad
     * draws the straight line from solid to nothing and loses the shape entirely.
     */
    private static final int WALL_BANDS = 3;

    /**
     * Ticks for one full pulse cycle.
     */
    @VisibleForTesting
    static final long PULSE_PERIOD_TICKS = ClockTicks.seconds(1).getTicks();

    /**
     * How far the pulse dims the wall at its trough.
     */
    @VisibleForTesting
    static final float PULSE_MIN_MULTIPLIER = 0.5F;

    @VisibleForTesting
    static final float PULSE_MAX_MULTIPLIER = 0.8F;

    /**
     * As {@link #render(RenderLevelStageEvent, AABB, Vector3f, float, long, double)} at the reach a plot
     * boundary is drawn with.
     */
    public static void render(@Nonnull RenderLevelStageEvent event,
                              @Nonnull AABB plane,
                              @Nonnull Vector3f color,
                              float alpha,
                              long gameTime) {
        render(event, plane, color, alpha, gameTime, PLOT_REACH_BLOCKS);
    }

    /**
     * Draws the wall around the given flat rectangle, reaching the given distance either side of it.
     * <p>
     * The opacity a caller passes is a ceiling rather than the value used: the pulse dims every wall drawn
     * here, so that any boundary reads as alive on screen whoever asked for it. Game time is the caller's to
     * supply, so the animation is driven by the same clock whose level the caller has already established.
     * <p>
     * Only the rectangle's horizontal extent and its Y are read; a box with height is flattened to its
     * {@code minY}, because the wall's own vertical span is what {@code reachBlocks} states and two owners
     * for one quantity is one too many.
     */
    public static void render(@Nonnull RenderLevelStageEvent event,
                              @Nonnull AABB plane,
                              @Nonnull Vector3f color,
                              float alpha,
                              long gameTime,
                              double reachBlocks) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer wall = bufferSource.getBuffer(ZoneWallRenderType.WALL);

        AABB box = plane.inflate(-EDGE_INSET_BLOCKS, 0.0D, -EDGE_INSET_BLOCKS);
        double lineY = box.minY;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float pulsedAlpha = alpha * pulseMultiplier(gameTime, partialTick);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f pose = poseStack.last().pose();

        emitFace(wall, pose, box.minX, box.minZ, box.maxX, box.minZ, lineY, reachBlocks, color, pulsedAlpha);
        emitFace(wall, pose, box.maxX, box.minZ, box.maxX, box.maxZ, lineY, reachBlocks, color, pulsedAlpha);
        emitFace(wall, pose, box.maxX, box.maxZ, box.minX, box.maxZ, lineY, reachBlocks, color, pulsedAlpha);
        emitFace(wall, pose, box.minX, box.maxZ, box.minX, box.minZ, lineY, reachBlocks, color, pulsedAlpha);

        poseStack.popPose();
        bufferSource.endBatch(ZoneWallRenderType.WALL);
    }

    /**
     * One side: the haze above the boundary line and its mirror image below it.
     */
    private static void emitFace(@Nonnull VertexConsumer buffer, @Nonnull Matrix4f pose,
                                 double x1, double z1, double x2, double z2,
                                 double lineY, double reachBlocks, @Nonnull Vector3f color, float alpha) {
        emitHalf(buffer, pose, x1, z1, x2, z2, lineY, 1.0D, reachBlocks, color, alpha);
        emitHalf(buffer, pose, x1, z1, x2, z2, lineY, -1.0D, reachBlocks, color, alpha);
    }

    /**
     * One side's haze in one direction from the line, stacked out of {@link #WALL_BANDS} quads. The fade is
     * still the GPU's per-vertex interpolation between each quad's own edges; the slices exist only so that
     * interpolation samples {@link #wallAlpha}'s curve often enough to stay a curve.
     * <p>
     * The direction argument is the sign the reach is applied with, which is all that separates the upper
     * half from its reflection — both halves read the same curve, so they cannot drift apart.
     */
    private static void emitHalf(@Nonnull VertexConsumer buffer, @Nonnull Matrix4f pose,
                                 double x1, double z1, double x2, double z2,
                                 double lineY, double direction, double reachBlocks,
                                 @Nonnull Vector3f color, float alpha) {
        for (int band = 0; band < WALL_BANDS; band++) {
            double nearSpread = (double) band / WALL_BANDS;
            double farSpread = (double) (band + 1) / WALL_BANDS;
            emitQuad(buffer, pose, x1, z1, x2, z2,
                    lineY + direction * nearSpread * reachBlocks,
                    lineY + direction * farSpread * reachBlocks,
                    nearSpread, farSpread, color, alpha);
        }
    }

    /**
     * One slice. The near edge is the one closer to the boundary line, whichever direction this slice runs
     * in, so the mirrored half needs no separate winding: the render type does not cull faces.
     */
    private static void emitQuad(@Nonnull VertexConsumer buffer, @Nonnull Matrix4f pose,
                                 double x1, double z1, double x2, double z2,
                                 double yNear, double yFar,
                                 double nearSpread, double farSpread,
                                 @Nonnull Vector3f color, float alpha) {
        float alphaNear = wallAlpha(nearSpread) * alpha;
        float alphaFar = wallAlpha(farSpread) * alpha;

        buffer.addVertex(pose, (float) x1, (float) yNear, (float) z1).setColor(color.x(), color.y(), color.z(), alphaNear);
        buffer.addVertex(pose, (float) x2, (float) yNear, (float) z2).setColor(color.x(), color.y(), color.z(), alphaNear);
        buffer.addVertex(pose, (float) x2, (float) yFar, (float) z2).setColor(color.x(), color.y(), color.z(), alphaFar);
        buffer.addVertex(pose, (float) x1, (float) yFar, (float) z1).setColor(color.x(), color.y(), color.z(), alphaFar);
    }

    /**
     * Alpha at a normalized distance from the boundary line: solid at 0, the line itself, and gone at 1,
     * either outer edge — holding near solid for the first stretch and then falling away quickly, per
     * {@link #FADE_EXPONENT}. Distance rather than height, so one curve serves both halves.
     */
    @VisibleForTesting
    static float wallAlpha(double spreadFraction) {
        return (float) (1.0D - Math.pow(spreadFraction, FADE_EXPONENT));
    }

    /**
     * Brightness multiplier for the given moment, oscillating between {@link #PULSE_MIN_MULTIPLIER} and
     * {@link #PULSE_MAX_MULTIPLIER} over {@link #PULSE_PERIOD_TICKS}.
     */
    @VisibleForTesting
    static float pulseMultiplier(long gameTime, float partialTick) {
        double phase = (gameTime % PULSE_PERIOD_TICKS + partialTick) / (double) PULSE_PERIOD_TICKS * 2.0D * Math.PI;
        double wave = (Math.sin(phase) + 1.0D) / 2.0D;
        return (float) (PULSE_MIN_MULTIPLIER + wave * (PULSE_MAX_MULTIPLIER - PULSE_MIN_MULTIPLIER));
    }

}
