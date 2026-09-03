package dev.breezes.settlements.infrastructure.rendering.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.RenderParameter;
import lombok.Builder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@Builder
public class BubbleItemStackElement implements BubbleInnerElement {

    private final ItemStack stack;

    @Builder.Default
    private final int iconSize = 14;

    /**
     * The layout padding box reported to the parent container
     */
    @Builder.Default
    private final int layoutSize = 12;

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack, @Nonnull Matrix4f bubblePose) {
        poseStack.pushPose();

        poseStack.scale(this.iconSize, -this.iconSize, 1.0F);
        poseStack.translate(0.0F, 0.0F, 0.5F);

        // The pose stack turns the item's normals with the camera since the bubble is a billboard
        // Pinning the normal to world-up keeps the brightness consistent
        MultiBufferSource buffer = renderType -> new UpwardNormalVertexConsumer(parameter.getBuffer().getBuffer(renderType));

        Minecraft.getInstance().getItemRenderer().renderStatic(this.stack, ItemDisplayContext.GUI,
                parameter.getPackedLight(), OverlayTexture.NO_OVERLAY,
                poseStack, buffer, parameter.getEntity().level(), 0);

        poseStack.popPose();
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        return BubbleBoundingBox.builder()
                .width(this.layoutSize)
                .height(this.layoutSize)
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
    }

    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleItemStackElement.builder()
                .stack(this.stack.copy())
                .iconSize(this.iconSize)
                .layoutSize(this.layoutSize)
                .build();
    }

    /**
     * Replaces every vertex normal with world up, discarding the one the pose stack produced.
     */
    private static final class UpwardNormalVertexConsumer extends VertexConsumerWrapper {

        private UpwardNormalVertexConsumer(@Nonnull VertexConsumer parent) {
            super(parent);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return super.setNormal(0.0F, 1.0F, 0.0F);
        }

    }

}
