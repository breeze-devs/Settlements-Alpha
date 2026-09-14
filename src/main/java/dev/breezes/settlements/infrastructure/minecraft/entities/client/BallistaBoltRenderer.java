package dev.breezes.settlements.infrastructure.minecraft.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.ballista.BallistaBoltGeometry;
import dev.breezes.settlements.domain.ballista.BallistaGeometry;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.BallistaBolt;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;

/**
 * Renders a bolt along its flight direction, with its shaft trailing behind.
 */
@ClientSide
public class BallistaBoltRenderer extends EntityRenderer<BallistaBolt> {

    /**
     * How far the tip extends past the entity position when stuck in a block, in pixels.
     */
    private static final float STUCK_EMBED_PIXELS = 6.5F;

    private final BallistaBoltModel model;

    public BallistaBoltRenderer(@Nonnull EntityRendererProvider.Context context) {
        super(context);
        this.model = new BallistaBoltModel(context.bakeLayer(BallistaBoltModel.LAYER));
    }

    @Override
    public void render(@Nonnull BallistaBolt bolt,
                       float entityYaw,
                       float partialTick,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource bufferSource,
                       int packedLight) {
        float yRot = Mth.rotLerp(partialTick, bolt.yRotO, bolt.getYRot());
        float xRot = Mth.lerp(partialTick, bolt.xRotO, bolt.getXRot());

        poseStack.pushPose();

        // Align +z with flight; negate pitch because positive X rotation tilts +z downward
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRot)));
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-xRot)));

        // The entity position marks the tip, but the model's pivot is near its tail
        // Move that pivot backward to align the tip; the extra forward offset makes a stuck bolt look embedded in the block
        float tipAheadOfPositionPixels = bolt.isInGround() ? STUCK_EMBED_PIXELS : 0.0F;
        poseStack.translate(0.0F, 0.0F, (tipAheadOfPositionPixels - BallistaBoltGeometry.TIP_REACH_PIXELS) / BallistaGeometry.PIXELS_PER_BLOCK);

        // The model points along -z with y downward. Flip both axes to match the flight frame.
        // Using X for the flip keeps the bolt from twisting as it leaves the ballista.
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.PI));
        this.model.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        super.render(bolt, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    @Nonnull
    public ResourceLocation getTextureLocation(@Nonnull BallistaBolt bolt) {
        return BallistaBoltModel.TEXTURE;
    }

}
