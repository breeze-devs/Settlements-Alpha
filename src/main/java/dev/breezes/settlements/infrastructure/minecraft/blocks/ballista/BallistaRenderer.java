package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaGeometry;
import dev.breezes.settlements.infrastructure.minecraft.entities.client.BallistaBoltModel;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

@ClientSide
public class BallistaRenderer implements BlockEntityRenderer<BallistaBlockEntity> {

    // The unrotated muzzle faces north (-z)
    private static final float MODEL_REST_HEADING_DEGREES = 180.0F;

    /**
     * Must cover the seated bolt's tip near release at every heading, including its overhang beyond the limbs.
     */
    private static final double RENDER_BOUNDS_REACH_BLOCKS = 3.0;

    /**
     * The raised bolt can extend above the rig's body.
     */
    private static final double RENDER_BOUNDS_TOP_BLOCKS = 2.25;

    /**
     * Crank handles at high elevation and the bolt tip when aiming down can extend below the block floor.
     */
    private static final double RENDER_BOUNDS_BOTTOM_BLOCKS = 0.5;

    private final BallistaModel model;
    private final BallistaBoltModel boltModel;

    public BallistaRenderer(@Nonnull BlockEntityRendererProvider.Context context) {
        this.model = new BallistaModel(context.bakeLayer(BallistaModel.LAYER));
        this.boltModel = new BallistaBoltModel(context.bakeLayer(BallistaBoltModel.LAYER));
    }

    @Override
    public void render(@Nonnull BallistaBlockEntity ballista,
                       float partialTick,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        BallistaAim interpolatedAim = ballista.interpolatedAim(partialTick);
        float yaw = interpolatedAim.getYaw();
        float pitch = interpolatedAim.getPitch();
        // The x/y flip below makes yaw follow world heading
        // Negative model pitch raise the muzzle
        float turretYRot = (float) Math.toRadians(yaw - MODEL_REST_HEADING_DEGREES);
        float turretXRot = (float) Math.toRadians(-pitch);
        this.model.pose(turretYRot, turretXRot, ballista.sampleAnimation(partialTick));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // Convert the rig's downward y and mirrored x to world coordinates
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -BallistaGeometry.GROUND_DEPTH_PIXELS / BallistaGeometry.PIXELS_PER_BLOCK, 0.0F);
        this.model.render(poseStack, bufferSource, packedLight, packedOverlay);

        if (ballista.drawsBoltOnSocket()) {
            poseStack.pushPose();
            this.model.translateToBoltSocket(poseStack);
            this.boltModel.render(poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(@Nonnull BallistaBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double axisX = pos.getX() + 0.5;
        double axisZ = pos.getZ() + 0.5;
        // Include overhangs so visible geometry is not culled when the block's own cell is off screen
        return new AABB(axisX - RENDER_BOUNDS_REACH_BLOCKS, pos.getY() - RENDER_BOUNDS_BOTTOM_BLOCKS,
                axisZ - RENDER_BOUNDS_REACH_BLOCKS, axisX + RENDER_BOUNDS_REACH_BLOCKS,
                pos.getY() + RENDER_BOUNDS_TOP_BLOCKS, axisZ + RENDER_BOUNDS_REACH_BLOCKS);
    }

}
